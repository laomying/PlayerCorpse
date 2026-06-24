package com.corpse;

import com.corpse.entity.CorpseEntityRenderer;
import com.corpse.gui.CorpseScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class CorpseModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(CorpseMod.CORPSE_ENTITY_TYPE, CorpseEntityRenderer::new);

        MenuScreens.register(CorpseMod.CORPSE_SCREEN_HANDLER, CorpseScreen::new);

        CorpseMod.LOGGER.info("Corpse Mod client initialized!");
    }
}
