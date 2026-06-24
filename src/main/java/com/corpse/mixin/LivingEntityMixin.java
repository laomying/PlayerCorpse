package com.corpse.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that cancels the vanilla item drop on player death.
 * The CorpseEntity will handle item storage instead.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * Cancel dropAllDeathLoot for players so items stay in the inventory.
     * The DeathEventHandler will create a CorpseEntity that stores the items.
     */
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void corpse_cancelDeathDrops(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer) {
            ci.cancel();
        }
    }
}
