package com.corpse;

import com.corpse.entity.CorpseEntity;
import com.corpse.event.DeathEventHandler;
import com.corpse.gui.CorpseScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorpseMod implements ModInitializer {

    public static final String MOD_ID = "corpse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final EntityType<CorpseEntity> CORPSE_ENTITY_TYPE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "corpse"),
            EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
                    .sized(2F, 0.5F)
                    .eyeHeight(0.25F)
                    .build("corpse")
    );

    public static final MenuType<CorpseScreenHandler> CORPSE_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "corpse_inventory"),
            new MenuType<>(CorpseScreenHandler::new, FeatureFlags.DEFAULT_FLAGS)
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Corpse Mod initializing...");

        DeathEventHandler.register();

        LOGGER.info("Corpse Mod initialized!");
    }
}
