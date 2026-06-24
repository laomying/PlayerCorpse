package com.corpse.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * A client-side dummy player used to render the player's skin on the corpse.
 * Extends RemotePlayer to get the skin rendering through PlayerRenderer.
 * This entity never exists on the server - it's purely for client rendering.
 */
public class DummyPlayerEntity extends RemotePlayer {

    public DummyPlayerEntity(ClientLevel level, GameProfile gameProfile,
                             EnumMap<EquipmentSlot, ItemStack> equipment) {
        super(level, gameProfile);

        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            setItemSlot(entry.getKey(), entry.getValue());
        }

        setPos(0.0D, 0.0D, 0.0D);
        xo = 0.0D;
        yo = 0.0D;
        zo = 0.0D;

        // Freeze all rotations and animations — corpse should be motionless
        this.yBodyRot = 0.0F;
        this.yBodyRotO = 0.0F;
        this.yHeadRot = 0.0F;
        this.yHeadRotO = 0.0F;
        this.xRotO = 0.0F;
        this.setYHeadRot(0.0F);
        this.setXRot(0.0F);
        this.walkAnimation.setSpeed(0.0F);
        this.walkAnimation.position(0.0F);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }
}
