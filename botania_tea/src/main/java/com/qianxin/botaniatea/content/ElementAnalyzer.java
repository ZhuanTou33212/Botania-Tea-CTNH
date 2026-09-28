package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.registry.ModItems;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.Reader;
import java.math.BigInteger;
import java.util.*;

/** Immutable server snapshots; unresolved routes never become free prices. */
public final class ElementAnalyzer {
    public static final Set<String> NAMESPACES=Set.of("minecraft","botania","tea_art","botania_tea","create","farmersdelight");
    private static volatile Snapshot current=new Snapshot(Map.of(),Map.of(),List.of(),Map.of());
    private record Snapshot(Map<ResourceLocation,ItemElementProfile> profiles,
                            Map<ResourceLocation,ElementLiquidVector> liquids,
                            List<NormalizedProcess> processes,Map<String,String> diagnostics) {}
    private ElementAnalyzer() {}
    public static void rebuild() { /* Final tags are unavailable during common setup. */ }
    public static synchronized void rebuild(MinecraftServer server) {
        Builder b=new Builder(server); b.build();
        current=new Snapshot(Map.copyOf(b.values),Map.copyOf(b.liquids),List.copyOf(b.all),Map.copyOf(b.notes));
    }
    public static ItemElementProfile profile(ItemStack stack) { return stack.isEmpty()?null:current.profiles.get(ForgeRegistries.ITEMS.getKey(stack.getItem())); }
    public static Map<ResourceLocation,ItemElementProfile> snapshot() { return current.profiles; }
    public static ElementLiquidVector liquidProfile(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return current.liquids.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
    public static Map<ResourceLocation,ElementLiquidVector> liquidSnapshot() { return current.liquids; }
    public static List<NormalizedProcess> processes() { return current.processes; }
    public static Map<String,String> diagnostics() { return current.diagnostics; }
    public static Map<String,Map<String,String>> report() {
        Map<String,Map<String,String>> out=new TreeMap<>();
        current.profiles.forEach((id,p)->out.put(id.toString(),Map.of("status",p.status(),"vector",p.vector()==null?"unresolved":p.vector().toString(),
                "liquid_mb",current.liquids.getOrDefault(id,new ElementLiquidVector()).toString(),
                "source",p.source(),"warning",p.warning(),"categories",p.categories().toString())));
        return out;
    }
    private static final class Builder {
        final MinecraftServer server;
        final Map<ResourceLocation,ItemElementProfile> values=new LinkedHashMap<>();
        final Map<ResourceLocation,ElementLiquidVector> liquids=new LinkedHashMap<>();
        final Map<Item,List<NormalizedProcess>> routes=new HashMap<>();
        final Map<String,String> notes=new TreeMap<>();
        final List<NormalizedProcess> all=new ArrayList<>();
        final Map<Item,Set<String>> tags=new HashMap<>();
        final List<ElementRule> rules=new ArrayList<>();
        /** Recipe id to station class; the route priority also depends on the output's namespace. */
        final Map<ResourceLocation,Integer> stationClasses=new HashMap<>();
        Builder(MinecraftServer server) { this.server=server; }
        void build() {
            long start=System.nanoTime();
            rules.addAll(loadRules());
            for (Item item:ForgeRegistries.ITEMS) if (supported(item)) {
                Set<String> set=new TreeSet<>(); item.builtInRegistryHolder().tags().forEach(t->set.add(t.location().toString())); tags.put(item,set);
                ElementVector spell=spellAnchor(set);
                if (spell!=null) put(item,spell,"MANUAL","spell_element_tag","Spell affinity tags; not a material or crafting-cost vector");
                else {
                    RuleMatch override=bestOverrideRule(item);
                    if (override!=null) put(item,override.vector(),"OVERRIDE",override.source(),override.reason());
                    else {
                        ElementVector anchor=anchor(item,set);
                        if (anchor!=null) put(item,anchor,"MANUAL","material_anchor","First-release anchor; functional tags add no elements");
                    }
                }
            }
            for (ElementType type:ElementType.values()) put(ModItems.ELEMENT_CRYSTALS.get(type).get(),ElementVector.of(type,1),"MANUAL","crystal_anchor","Magical production, not ordinary material conservation");
            var recipes=server.getRecipeManager().getRecipes().stream().sorted(Comparator.comparing(r->r.getId().toString())).toList();
            for (Recipe<?> recipe:recipes) {
                if (!NAMESPACES.contains(recipe.getId().getNamespace())) continue;
                try {
                    NormalizedProcess p=normalize(recipe);
                    if (p==null) { notes.put(recipe.getId().toString(),"UNSUPPORTED_RECIPE: "+ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer())); continue; }
                    stationClasses.put(recipe.getId(),stationClass(recipe));
                    all.add(p);
                    for (var o:p.outputs()) if (supported(o.stack().getItem())) routes.computeIfAbsent(o.stack().getItem(),k->new ArrayList<>()).add(p);
                } catch (RuntimeException ex) { notes.put(recipe.getId().toString(),"UNSUPPORTED_RECIPE: "+ex.getClass().getSimpleName()+": "+ex.getMessage()); }
            }
            for (Item item:tags.keySet().stream().sorted(Comparator.comparing(i->ForgeRegistries.ITEMS.getKey(i).toString())).toList()) resolve(item,new LinkedHashSet<>());
            notes.put("element_rules_loaded",Integer.toString(rules.size()));
            // Apply conservative tag fallbacks once more after route resolution. A cyclic or
            // unsupported recipe must not prevent an otherwise valid material-family rule.
            for (Item item:tags.keySet()) if (!values.containsKey(ForgeRegistries.ITEMS.getKey(item))) {
                RuleMatch override=bestOverrideRule(item);
                if (override!=null) { put(item,override.vector(),"OVERRIDE",override.source(),override.reason()); continue; }
                RuleMatch fallback=bestFallbackRule(item);
                if (fallback!=null) put(item,fallback.vector(),"RULE_FALLBACK",fallback.source(),fallback.reason());
            }
            for (var entry:routes.entrySet()) {
                ItemElementProfile base=values.get(ForgeRegistries.ITEMS.getKey(entry.getKey()));
                if (base==null || base.vector()==null) continue;
                for (var route:entry.getValue()) {
                    ElementVector alt=evaluate(route,new LinkedHashSet<>());
                    if (alt!=null && !alt.equals(base.vector())) notes.put(route.id().toString(),"ROUTE_DIFFERS: baseline="+base.source()+" "+base.vector()+" alternative="+alt);
                }
            }
            for (Item item:tags.keySet()) if (!values.containsKey(ForgeRegistries.ITEMS.getKey(item))) put(item,null,"UNRESOLVED","","No unambiguous route or approved anchor");
            applyAugmentRules();
            buildLiquidValues();
            notes.put("analysis_elapsed_ms",Long.toString((System.nanoTime()-start)/1_000_000));
        }

        /**
         * Builds the concrete mB ledger after abstract elements have stabilized.  Nuggets and
         * ingots are explicit material tiers.  Single-output compression recipes can only lower
         * the output ledger, never raise it above the sum of their children.
         */
        void buildLiquidValues() {
            values.forEach((id, profile) -> {
                if (profile.vector()!=null) liquids.put(id,ElementLiquidVector.from(profile.vector()));
            });
            for (var entry:tags.entrySet()) {
                ResourceLocation id=ForgeRegistries.ITEMS.getKey(entry.getKey());
                if (id==null) continue;
                Set<String> itemTags=entry.getValue();
                boolean nugget=itemTags.contains("forge:nuggets") || itemTags.stream().anyMatch(t->t.startsWith("forge:nuggets/"));
                boolean ingot=itemTags.contains("forge:ingots") || itemTags.stream().anyMatch(t->t.startsWith("forge:ingots/"));
                if (nugget) liquids.put(id,ElementLiquidVector.of(ElementType.METAL,15));
                else if (ingot) liquids.put(id,ElementLiquidVector.of(ElementType.METAL,100));
            }
            for (ElementType type:ElementType.values()) {
                ResourceLocation id=ForgeRegistries.ITEMS.getKey(ModItems.ELEMENT_CRYSTALS.get(type).get());
                if (id!=null) liquids.put(id,ElementLiquidVector.of(type,ElementLiquidVector.MB_PER_ESSENCE));
            }

            int maxPasses=Math.max(1,liquids.size());
            int changes=0;
            for (int pass=0;pass<maxPasses;pass++) {
                boolean changed=false;
                for (NormalizedProcess process:all) {
                    ElementLiquidVector available=liquidInputs(process);
                    if (available==null || process.outputs().size()!=1) continue;
                    ItemStack output=process.outputs().get(0).stack();
                    // A split/decompression recipe is not allowed to redefine the explicit value
                    // of a child tier (for example, 1 ingot -> 9 nuggets).
                    if (output.isEmpty() || output.hasTag() || output.getCount()!=1) continue;
                    ResourceLocation outputId=ForgeRegistries.ITEMS.getKey(output.getItem());
                    ElementLiquidVector old=liquids.get(outputId);
                    if (old==null) continue;
                    ElementLiquidVector capped=old.clampTo(available);
                    if (!capped.equals(old)) {
                        liquids.put(outputId,capped); changed=true; changes++;
                        notes.put("liquid_cap:"+process.id(),"OUTPUT_CAPPED: "+old+" -> "+capped+"; inputs="+available);
                    }
                }
                if (!changed) break;
            }
            notes.put("element_liquid_profiles",Integer.toString(liquids.size()));
            notes.put("element_liquid_caps",Integer.toString(changes));
        }

        /** Conservative liquid available from all slots of a deterministic crafting route. */
        ElementLiquidVector liquidInputs(NormalizedProcess process) {
            if (!process.deterministicSingleOutput()) return null;
            ElementLiquidVector sum=new ElementLiquidVector();
            boolean consumed=false;
            for (Ingredient ingredient:process.inputs()) {
                if (ingredient.isEmpty()) continue;
                ElementLiquidVector representative=null;
                for (ItemStack candidate:ingredient.getItems()) {
                    if (!supported(candidate.getItem()) || candidate.hasTag()) continue;
                    ResourceLocation id=ForgeRegistries.ITEMS.getKey(candidate.getItem());
                    ElementLiquidVector value=liquids.get(id);
                    if (value==null) return null;
                    if (process.craftingRemainders() && candidate.hasCraftingRemainingItem()) {
                        ItemStack remainder=candidate.getCraftingRemainingItem();
                        if (remainder.is(candidate.getItem())) value=new ElementLiquidVector();
                        else {
                            ElementLiquidVector refund=liquids.get(ForgeRegistries.ITEMS.getKey(remainder.getItem()));
                            if (refund==null) return null;
                            value=value.subtractFloor(refund.multiply(remainder.getCount()));
                        }
                    }
                    representative=representative==null?value:representative.componentMin(value);
                }
                if (representative==null) return null;
                sum=sum.add(representative); consumed=true;
            }
            return consumed?sum:null;
        }
        boolean supported(Item item) { var id=ForgeRegistries.ITEMS.getKey(item); return id!=null && NAMESPACES.contains(id.getNamespace()) && item!=Items.AIR; }
        void put(Item item,ElementVector vector,String status,String source,String warning) {
            var id=ForgeRegistries.ITEMS.getKey(item); values.put(id,new ItemElementProfile(id,vector,Set.copyOf(tags.getOrDefault(item,Set.of())),status,source,warning));
        }
        ElementVector anchor(Item item,Set<String> set) {
            if (item==Items.IRON_NUGGET) return ElementVector.of(ElementType.METAL,1);
            if (item==Items.COBBLESTONE) return ElementVector.of(ElementType.STONE,1);
            if (set.contains("minecraft:logs")) return ElementVector.of(ElementType.PLANT,4);
            if (set.contains("forge:seeds") || set.stream().anyMatch(t->t.startsWith("forge:seeds/"))) return ElementVector.of(ElementType.PLANT,1).add(ElementVector.of(ElementType.LIFE,1));
            return null;
        }
        ElementVector spellAnchor(Set<String> set) {
            ElementVector result=new ElementVector(); boolean found=false;
            String prefix="botania_tea:spell_elements/";
            for (String tag:set) if (tag.startsWith(prefix)) {
                ElementType type=ElementType.byId(tag.substring(prefix.length()));
                if (type!=null) { result=result.add(ElementVector.of(type,1)); found=true; }
            }
            return found?result:null;
        }
        ElementVector resolve(Item item,Set<Item> visiting) {
            var known=values.get(ForgeRegistries.ITEMS.getKey(item)); if (known!=null) return known.vector();
            if (!supported(item)) return null;
            // Explicit override wins over recipe derivation: the reviewer-approved value is authoritative.
            RuleMatch override=bestOverrideRule(item);
            if (override!=null) { put(item,override.vector(),"OVERRIDE",override.source(),override.reason()); return override.vector(); }
            if (visiting.size()>128) { notes.put("depth:"+ForgeRegistries.ITEMS.getKey(item),"UNRESOLVED: graph depth limit 128"); return null; }
            if (!visiting.add(item)) { notes.put("cycle:"+ForgeRegistries.ITEMS.getKey(item),"CYCLE: requires external anchor"); return null; }
            try {
        for (var route:routes.getOrDefault(item,List.of()).stream()
                .sorted(java.util.Comparator.comparingInt(r -> routePriority(item,r.id()))).toList()) {
                    ElementVector value=evaluate(route,visiting);
                    if (value!=null && value.isIntegral()) { put(item,value,"RESOLVED",route.id().toString(),"Canonical first integer-valued recipeId; see route differences"); return value; }
                    if (value!=null) notes.putIfAbsent("non_integer:"+ForgeRegistries.ITEMS.getKey(item),"Skipped non-integer route "+route.id());
                }
                RuleMatch fallback=bestFallbackRule(item);
                if (fallback!=null) {
                    put(item,fallback.vector(),"RULE_FALLBACK",fallback.source(),fallback.reason());
                    return fallback.vector();
                }
                return null;
            } finally { visiting.remove(item); }
        }
    /**
     * Crafting station a recipe belongs to, independent of what it produces:
     * 0 petal apothecary, 1 runic altar, 2 mana pool, 3 crafting table, 4 everything else.
     */
    private static int stationClass(Recipe<?> recipe) {
        String path=recipe.getId().getPath();
        if (recipe.getId().getNamespace().equals("botania")) {
            if (path.startsWith("petal_apothecary/")) return 0;
            if (path.startsWith("runic_altar/")) return 1;
            if (path.startsWith("mana_infusion/")) return 2;
        }
        return recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe ? 3 : 4;
    }

    /**
     * Route priority for element sourcing.
     *
     * <p>Botania items follow the station chain the player actually builds them on: the petal
     * apothecary first, then the runic altar, then the mana pool, then the crafting table.
     *
     * <p>Vanilla items are defined by their own recipes: a {@code minecraft:} route always outranks
     * an add-on route, so Botania's dye/mana cycles, Create's milling and Farmer's Delight's crate
     * recipes cannot redefine what a vanilla item is made of.
     *
     * <p>Every other add-on item prefers its own crafting recipe and falls back to other stations.
     * Conservation is still enforced by {@link #evaluate}: the output is the summed inputs divided
     * by the output count.
     */
    private int routePriority(Item output,ResourceLocation recipeId) {
        int station=stationClasses.getOrDefault(recipeId,4);
        ResourceLocation itemId=ForgeRegistries.ITEMS.getKey(output);
        boolean magicalItem=itemId!=null && (itemId.getNamespace().equals("botania")||itemId.getNamespace().equals("botania_tea"));
        if (magicalItem) return switch (station) { case 0->0; case 1->1; case 2->2; case 3->3; default->4; };
        boolean vanillaItem=itemId!=null && itemId.getNamespace().equals("minecraft");
        if (vanillaItem) return recipeId.getNamespace().equals("minecraft") ? (station==3?0:1) : 2;
        return station==3?0:1;
    }

        ElementVector evaluate(NormalizedProcess p,Set<Item> visiting) {
            if (!p.deterministicSingleOutput()) { notes.putIfAbsent(p.id().toString(),"UNRESOLVED: allocation/fluid/conditional route; "+p.warning()); return null; }
            ElementVector sum=new ElementVector(); boolean consumed=false;
            for (Ingredient ingredient:p.inputs()) {
                if (ingredient.isEmpty()) continue;
                ElementVector representative=null;
                var candidates=Arrays.stream(ingredient.getItems()).filter(s->supported(s.getItem())).toList();
                if (candidates.isEmpty()) return null;
                for (ItemStack candidate:candidates) {
                    if (candidate.hasTag()) return null;
                    ElementVector value=resolve(candidate.getItem(),visiting); if (value==null) return null;
                    if (p.craftingRemainders() && candidate.hasCraftingRemainingItem()) {
                        var remainder=candidate.getCraftingRemainingItem();
                        if (remainder.is(candidate.getItem())) value=new ElementVector();
                        else {
                            ElementVector refund=resolve(remainder.getItem(),visiting); if (refund==null) return null;
                            try { value=value.subtract(refund.multiply(remainder.getCount())); } catch (IllegalArgumentException ex) { notes.put(p.id().toString(),"NEGATIVE_NET_INPUT"); return null; }
                        }
                    }
                    // A tag whose members disagree stays usable: the first candidate is the canonical
                    // representative, which keeps the recipe deterministic instead of rejecting it outright.
                    if (representative!=null && !representative.equals(value)) notes.put(p.id().toString(),"AMBIGUOUS_INGREDIENT: using first representative");
                    representative=value;
                }
                consumed=true; sum=sum.add(representative);
            }
            var output=p.outputs().get(0).stack(); if (!consumed || output.isEmpty() || output.hasTag()) return null;
            ElementVector result=sum.divide(output.getCount());
            if (!p.servingContainer().isEmpty()) {
                var c=p.servingContainer(); ElementVector value=resolve(c.getItem(),visiting); if (value==null) return null;
                result=result.add(value.multiply(c.getCount()));
            }
            return result;
        }
        NormalizedProcess normalize(Recipe<?> recipe) {
            var access=server.registryAccess();
            // Tea's own crafting stations are authoritative for their products and must be
            // normalized before the vanilla recipe fallbacks below.
            if (recipe instanceof com.qianxin.botaniatea.recipe.OfferingCraftingRecipe crucible)
                return OfferingCraftingNormalizer.normalize(crucible,access);
        if (BotaniaCraftingNormalizer.handles(recipe)) return BotaniaCraftingNormalizer.normalize(recipe,access);
            if (net.minecraftforge.fml.ModList.get().isLoaded("create")) {
                var p=com.qianxin.botaniatea.compat.create.CreateElementAdapter.normalize(recipe,access); if (p!=null) return p;
            }
            if (net.minecraftforge.fml.ModList.get().isLoaded("farmersdelight")) {
                var p=com.qianxin.botaniatea.compat.farmersdelight.FarmersDelightElementAdapter.normalize(recipe,access); if (p!=null) return p;
            }
            if (!(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe || recipe instanceof AbstractCookingRecipe || recipe instanceof StonecutterRecipe)) return null;
            if (recipe.isSpecial()) return null;
            return new NormalizedProcess(recipe.getId(),recipe.getIngredients(),List.of(),List.of(new NormalizedProcess.Output(recipe.getResultItem(access),"1.0")),List.of(),ItemStack.EMPTY,recipe instanceof CraftingRecipe,"");
        }

        List<ElementRule> loadRules() {
            List<ElementRule> loaded=new ArrayList<>();
            var resources=server.getResourceManager().listResources("element_rules",
                    path->path.getNamespace().equals("botania_tea") && path.getPath().endsWith(".json"));
            for (var entry:resources.entrySet()) {
                String source=entry.getKey().toString();
                try (Reader reader=entry.getValue().openAsReader()) {
                    JsonElement root=JsonParser.parseReader(reader);
                    if (root.isJsonArray()) for (JsonElement element:root.getAsJsonArray()) loaded.add(parseRule(element.getAsJsonObject(),source));
                    else if (root.isJsonObject() && root.getAsJsonObject().has("rules")) for (JsonElement element:root.getAsJsonObject().getAsJsonArray("rules")) loaded.add(parseRule(element.getAsJsonObject(),source));
                    else loaded.add(parseRule(root.getAsJsonObject(),source));
                } catch (RuntimeException | java.io.IOException ex) {
                    notes.put("rule:"+source,"INVALID_RULE: "+ex.getClass().getSimpleName()+": "+ex.getMessage());
                }
            }
            loaded.sort(Comparator.comparingInt(ElementRule::priority).reversed().thenComparing(ElementRule::source));
            return loaded;
        }

        ElementRule parseRule(JsonObject root,String source) {
            int schema=root.has("schema_version")?root.get("schema_version").getAsInt():1;
            if (schema!=1) throw new IllegalArgumentException("unsupported schema_version "+schema);
            JsonObject selector=root.getAsJsonObject("selector");
            if (selector==null || selector.entrySet().size()!=1) throw new IllegalArgumentException("selector must contain exactly one selector");
            var selected=selector.entrySet().iterator().next();
            String kind=selected.getKey(), value=selected.getValue().getAsString();
            if (!Set.of("item","item_tag","block_tag","tag_prefix","item_prefix","item_namespace").contains(kind)) throw new IllegalArgumentException("unsupported selector "+kind);
            if ((kind.equals("item_prefix") || kind.equals("item_namespace")) && value.isBlank()) throw new IllegalArgumentException("empty "+kind);
            if (kind.equals("item") && ResourceLocation.tryParse(value)==null) throw new IllegalArgumentException("invalid item id "+value);
            if (kind.equals("tag_prefix") && ResourceLocation.tryParse(value.substring(0,Math.max(0,value.length()-1)))==null && !value.contains(":")) throw new IllegalArgumentException("invalid tag_prefix "+value);
            EnumMap<ElementType,RationalAmount> values=new EnumMap<>(ElementType.class);
            JsonObject vector=root.getAsJsonObject("fallback_vector");
            if (vector==null) throw new IllegalArgumentException("fallback_vector is missing");
            for (var element:vector.entrySet()) {
                ElementType type=ElementType.byId(element.getKey());
                if (type==null) throw new IllegalArgumentException("unknown element "+element.getKey());
                RationalAmount amount=parseAmount(element.getValue().getAsString());
                if (!amount.isInteger()) throw new IllegalArgumentException("non-integer fallback value "+element.getKey()+"="+amount);
                values.put(type,amount);
            }
            return new ElementRule(root.has("priority")?root.get("priority").getAsInt():0,kind,value,
                    new ElementVector(values),root.has("mode")?root.get("mode").getAsString():"fallback_if_no_recipe",
                    root.has("reason")?root.get("reason").getAsString():"Data rule fallback",source);
        }

        RationalAmount parseAmount(String raw) {
            String[] parts=raw.split("/",-1);
            if (parts.length>2 || parts.length==0 || parts[0].isBlank()) throw new IllegalArgumentException("invalid rational "+raw);
            BigInteger numerator=new BigInteger(parts[0]);
            BigInteger denominator=parts.length==2?new BigInteger(parts[1]):BigInteger.ONE;
            RationalAmount amount=new RationalAmount(numerator,denominator);
            if (!amount.isInteger()) throw new IllegalArgumentException("non-integer rule amount "+raw);
            return amount;
        }

        /** Reviewer-approved absolute value; replaces any recipe-derived vector. */
        RuleMatch bestOverrideRule(Item item) {
            ResourceLocation id=ForgeRegistries.ITEMS.getKey(item);
            Set<String> set=item.builtInRegistryHolder().tags().map(t->t.location().toString()).collect(java.util.stream.Collectors.toSet());
            List<ElementRule> matching=rules.stream().filter(rule->rule.mode().equals("override") && rule.matches(id,item,set)).toList();
            if (matching.isEmpty()) return null;
            int priority=matching.get(0).priority();
            List<ElementRule> top=matching.stream().filter(rule->rule.priority()==priority).toList();
            ElementVector vector=top.get(0).vector();
            if (top.stream().anyMatch(rule->!rule.vector().equals(vector))) {
                notes.put("override-conflict:"+id,"AMBIGUOUS_OVERRIDES: "+top.stream().map(ElementRule::source).sorted().toList());
                return null;
            }
            ElementRule selected=top.get(0);
            return new RuleMatch(vector,selected.source(),selected.reason());
        }

        RuleMatch bestFallbackRule(Item item) {
            ResourceLocation id=ForgeRegistries.ITEMS.getKey(item);
            Set<String> set=tags.getOrDefault(item,Set.of());
            List<ElementRule> matching=rules.stream().filter(rule->rule.mode().equals("fallback_if_no_recipe") && rule.matches(id,item,set)).toList();
            if (matching.isEmpty()) return null;
            int priority=matching.get(0).priority();
            List<ElementRule> top=matching.stream().filter(rule->rule.priority()==priority).toList();
            ElementVector vector=top.get(0).vector();
            if (top.stream().anyMatch(rule->!rule.vector().equals(vector))) {
                notes.put("rule-conflict:"+id,"AMBIGUOUS_RULES: "+top.stream().map(ElementRule::source).sorted().toList());
                return null;
            }
            ElementRule selected=top.get(0);
            return new RuleMatch(vector,selected.source(),selected.reason());
        }

        void applyAugmentRules() {
            for (Item item:tags.keySet()) {
                ResourceLocation id=ForgeRegistries.ITEMS.getKey(item);
                Set<String> set=tags.getOrDefault(item,Set.of());
                List<ElementRule> matching=rules.stream()
                        .filter(rule->rule.mode().equals("augment") && rule.matches(id,item,set)).toList();
                if (matching.isEmpty()) continue;
                int priority=matching.get(0).priority();
                List<ElementRule> top=matching.stream().filter(rule->rule.priority()==priority).toList();
                ElementVector addition=top.get(0).vector();
                if (top.stream().anyMatch(rule->!rule.vector().equals(addition))) {
                    notes.put("augment-conflict:"+id,"AMBIGUOUS_AUGMENTS: "+top.stream().map(ElementRule::source).sorted().toList());
                    continue;
                }
                ItemElementProfile existing=values.get(id);
                ElementVector vector=existing==null || existing.vector()==null?addition:existing.vector().add(addition);
                String source=existing==null?top.get(0).source():existing.source()+" + "+top.get(0).source();
                String warning=existing==null?top.get(0).reason():existing.warning()+"; "+top.get(0).reason();
                put(item,vector,"AUGMENTED",source,warning);
            }
        }

        record RuleMatch(ElementVector vector,String source,String reason) {}
        record ElementRule(int priority,String selectorKind,String selector,ElementVector vector,String mode,String reason,String source) {
            boolean matches(ResourceLocation id,Item item,Set<String> tags) {
                return switch (selectorKind) {
                    case "item" -> id!=null && id.toString().equals(selector);
                    case "item_prefix" -> id!=null && id.toString().startsWith(selector);
                    case "item_namespace" -> id!=null && id.getNamespace().equals(selector);
                    case "item_tag" -> tags.contains(selector);
                    case "block_tag" -> item instanceof BlockItem block
                            && block.getBlock().builtInRegistryHolder().tags()
                            .anyMatch(tag->tag.location().toString().equals(selector));
                    case "tag_prefix" -> tags.stream().anyMatch(tag->tag.startsWith(selector));
                    default -> false;
                };
            }
        }
    }
}
