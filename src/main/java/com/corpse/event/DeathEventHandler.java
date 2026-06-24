package com.corpse.event;

import com.corpse.CorpseMod;
import com.corpse.entity.CorpseEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public class DeathEventHandler {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                // At this point, the mixin has cancelled vanilla item drops
                // The player's inventory is still intact
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
