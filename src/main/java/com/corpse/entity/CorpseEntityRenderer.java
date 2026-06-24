package com.corpse.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;

public class CorpseEntityRenderer extends EntityRenderer<CorpseEntity> {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/player/wide/steve.png");

    public CorpseEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity entity) {
        return FALLBACK_TEXTURE;
    }

    @Override
    public void render(CorpseEntity entity, float yaw, float tickDelta, PoseStack stack,
                       MultiBufferSource source, int packedLight) {
        if (entity.isSkeleton()) {
            return;
        }

        GameProfile profile = new GameProfile(entity.getPlayerUuid(), entity.getPlayerName());
        EnumMap<EquipmentSlot, ItemStack> equipment = entity.getEquipment();

        DummyPlayerEntity dummyPlayer = new DummyPlayerEntity(
                (ClientLevel) entity.level(),
                profile,
                equipment
        );

        PlayerRenderer playerRenderer = (PlayerRenderer) MC.getEntityRenderDispatcher()
                .getRenderer(dummyPlayer);
        if (playerRenderer == null) {
            return;
        }

        stack.pushPose();

        // Rotate to face camera direction
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        // Lay player flat on back
        stack.mulPose(Axis.XP.rotationDegrees(-90F));
        stack.translate(0.0D, -1.0D, 0.125D);

        // Delegate to vanilla player renderer — this renders skin, cape, armor layers, held items
        playerRenderer.render(dummyPlayer, 0F, tickDelta, stack, source, packedLight);

        stack.popPose();
    }
}
