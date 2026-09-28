package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.content.ElementAnalyzer;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid=BotaniaTeaMod.MOD_ID)
public final class ElementAnalysisEvents {
    @SubscribeEvent public static void started(ServerStartedEvent event) { rebuild(event.getServer()); }
    @SubscribeEvent public static void reload(OnDatapackSyncEvent event) {
        if (event.getPlayer()==null) {
            rebuild(event.getPlayerList().getServer());
            event.getPlayerList().getPlayers().forEach(com.qianxin.botaniatea.network.ElementProfilesPacket::send);
        } else com.qianxin.botaniatea.network.ElementProfilesPacket.send(event.getPlayer());
    }
    private static void rebuild(MinecraftServer server) {
        ElementAnalyzer.rebuild(server);
        String directory=System.getProperty("tea.elements.report");
        if (directory==null) return;
        try {
            Path root=Path.of(directory); Files.createDirectories(root);
            Gson gson=new GsonBuilder().setPrettyPrinting().create();
            Map<String,List<String>> tags=new TreeMap<>();
            ForgeRegistries.ITEMS.tags().getTagNames().forEach(key-> {
                List<String> members=ForgeRegistries.ITEMS.tags().getTag(key).stream().map(ForgeRegistries.ITEMS::getKey)
                        .filter(id->ElementAnalyzer.NAMESPACES.contains(id.getNamespace())).map(Object::toString).sorted().toList();
                tags.put(key.location().toString(),members);
            });
            Map<String,List<String>> types=new TreeMap<>();
            server.getRecipeManager().getRecipes().stream().filter(r->ElementAnalyzer.NAMESPACES.contains(r.getId().getNamespace())).forEach(r->
                    types.computeIfAbsent(String.valueOf(ForgeRegistries.RECIPE_TYPES.getKey(r.getType())),k->new ArrayList<>()).add(r.getId().toString()));
            JsonArray processes=new JsonArray();
            for (var p:ElementAnalyzer.processes()) {
                JsonObject row=new JsonObject(); row.addProperty("recipeId",p.id().toString());
                JsonArray inputs=new JsonArray(); p.inputs().forEach(i->inputs.add(i.toJson())); row.add("inputs",inputs);
                JsonArray tools=new JsonArray(); p.retainedTools().forEach(i->tools.add(i.toJson())); row.add("retainedTools",tools);
                JsonArray outputs=new JsonArray(); p.outputs().forEach(o->{ JsonObject out=new JsonObject(); out.addProperty("item",String.valueOf(ForgeRegistries.ITEMS.getKey(o.stack().getItem()))); out.addProperty("count",o.stack().getCount()); out.addProperty("chance",o.chance()); outputs.add(out); });
                row.add("outputs",outputs); row.add("fluids",gson.toJsonTree(p.fluids())); row.addProperty("warning",p.warning());
                row.addProperty("servingContainer",p.servingContainer().toString()); processes.add(row);
            }
            write(root,"tag-members.json",gson.toJson(tags)); write(root,"recipe-types.json",gson.toJson(types));
            write(root,"item-elements.json",gson.toJson(ElementAnalyzer.report())); write(root,"recipe-elements.json",gson.toJson(processes));
            write(root,"conflicts.json",gson.toJson(ElementAnalyzer.diagnostics()));
            Map<String,Long> counts=new TreeMap<>(); ElementAnalyzer.snapshot().values().forEach(p->counts.merge(p.status(),1L,Long::sum));
            write(root,"coverage.json",gson.toJson(counts));
        } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot export element acceptance evidence",ex); }
    }
    private static void write(Path root,String name,String data) throws java.io.IOException { Files.writeString(root.resolve(name),data,StandardCharsets.UTF_8); }
}
