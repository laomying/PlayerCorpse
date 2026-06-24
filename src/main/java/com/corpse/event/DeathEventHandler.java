package com.corpse.event;

import com.corpse.CorpseMod;
import com.corpse.entity.CorpseEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public class DeathEventHandler {

    /** Tag a player must have to prevent corpse spawning on death. */
    public static final String NO_CORPSE_TAG = "corpse.keep_inventory";

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {

                // Skip players tagged with the no-corpse tag
                if (player.getTags().contains(NO_CORPSE_TAG)) {
                    CorpseMod.LOGGER.info("Player {} has the '{}' tag — skipping corpse spawn",
                            player.getGameProfile().getName(), NO_CORPSE_TAG);
                    return;
                }

                CorpseMod.LOGGER.info("Player {} died, creating corpse at {}",
                        player.getGameProfile().getName(), player.blockPosition());

                CorpseEntity corpse = CorpseEntity.createFromPlayer(player);
                player.level().addFreshEntity(corpse);

                CorpseMod.LOGGER.info("Corpse created with {} items",
                        corpse.getMainInventory().stream().filter(s -> !s.isEmpty()).count()
                        + corpse.getArmorInventory().stream().filter(s -> !s.isEmpty()).count()
                        + corpse.getOffHandInventory().stream().filter(s -> !s.isEmpty()).count());
            }
        });
    }
}
