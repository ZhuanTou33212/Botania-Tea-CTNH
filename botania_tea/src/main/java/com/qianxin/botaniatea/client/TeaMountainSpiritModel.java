package com.qianxin.botaniatea.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qianxin.botaniatea.entity.TeaMountainSpiritEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Bark-armored tea spirit with a porcelain face, hinged kettle arm and six-leaf crown. */
public final class TeaMountainSpiritModel extends EntityModel<TeaMountainSpiritEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("botania_tea", "tea_mountain_spirit"), "main");
    private static final float[][] LEAF_COLORS = {
            {1F, 1F, 1F}, {.55F, 1F, .45F}, {1F, 1F, .25F},
            {1F, .35F, .25F}, {.8F, .5F, .25F}, {.3F, .25F, .25F}
    };
    private final ModelPart root;
    private final ModelPart mask;
    private final ModelPart leftRoot;
    private final ModelPart rightRoot;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart kettle;
    private final ModelPart[] crown = new ModelPart[6];

    public TeaMountainSpiritModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.mask = body.getChild("mask");
        this.leftRoot = body.getChild("left_root");
        this.rightRoot = body.getChild("right_root");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.kettle = rightArm.getChild("kettle");
        for (int n = 0; n < crown.length; n++) crown[n] = root.getChild("crown" + n);
    }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -29, -4, 10, 21, 8)
                .texOffs(64, 16).addBox(-4, -24, -5, 8, 9, 1)
                .texOffs(30, 22).addBox(-9, -28, -3, 5, 5, 6)
                .texOffs(30, 22).addBox(4, -28, -3, 5, 5, 6),
                PartPose.offset(0, 24, 0));
        body.addOrReplaceChild("left_root", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-2, 0, -4, 5, 9, 8), PartPose.offset(-4, -9, 0));
        body.addOrReplaceChild("right_root", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-3, 0, -4, 5, 9, 8), PartPose.offset(4, -9, 0));
        body.addOrReplaceChild("mask", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-4, -5, -2, 8, 10, 3)
                .texOffs(88, 0).addBox(-1, 0, -3, 2, 3, 1)
                .texOffs(96, 0).addBox(-4, -2, -3, 3, 1, 1)
                .texOffs(104, 0).addBox(1, -2, -3, 3, 1, 1),
                PartPose.offset(0, -32, -4));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(30, 0).addBox(-2, 0, -2, 4, 16, 5)
                .texOffs(30, 34).addBox(-3, 14, -3, 6, 2, 6), PartPose.offset(-7, -26, 0));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(30, 0).addBox(-2, 0, -2, 4, 16, 5), PartPose.offset(7, -26, 0));
        rightArm.addOrReplaceChild("kettle", CubeListBuilder.create()
                .texOffs(64, 64).addBox(-3, -1, -3, 6, 7, 6)
                .texOffs(64, 82).addBox(3, 1, -1, 3, 2, 2)
                .texOffs(84, 64).addBox(-2, -3, -2, 4, 2, 4)
                .texOffs(96, 64).addBox(-4, 0, -2, 1, 5, 4), PartPose.offset(3, 10, -4));
        for (int n = 0; n < 6; n++) {
            root.addOrReplaceChild("crown" + n, CubeListBuilder.create()
                            .texOffs(0, 64).addBox(-2, -5, -1, 4, 6, 2),
                    PartPose.offsetAndRotation((n - 2.5F) * 2F, -11, n % 2 == 0 ? -1 : 1,
                            0, 0, (n - 2.5F) * .16F));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(TeaMountainSpiritEntity entity, float limbSwing, float limbAmount,
                          float age, float yaw, float pitch) {
        mask.yRot = Math.max(-.5F, Math.min(.5F, yaw * (float) Math.PI / 180F));
        mask.xRot = Math.max(-.3F, Math.min(.3F, pitch * (float) Math.PI / 180F));
        leftRoot.xRot = (float) Math.sin(limbSwing * .6F) * limbAmount * .25F;
        rightRoot.xRot = -leftRoot.xRot;
        boolean windingUp = entity.attackWindup() > 0;
        leftArm.xRot = windingUp ? -.65F : -leftRoot.xRot * .5F;
        rightArm.xRot = windingUp ? -.85F : -rightRoot.xRot * .5F;
        kettle.zRot = windingUp ? -.2F : 0F;
        for (int n = 0; n < crown.length; n++)
            crown[n].xRot = (float) Math.sin(age * .05F + n) * .08F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float alpha) {
        for (ModelPart leaf : crown) leaf.visible = false;
        root.render(pose, buffer, light, overlay, r, g, b, alpha);
        for (int n = 0; n < crown.length; n++) {
            crown[n].visible = true;
            crown[n].render(pose, buffer, light, overlay,
                    LEAF_COLORS[n][0], LEAF_COLORS[n][1], LEAF_COLORS[n][2], alpha);
        }
    }
}
