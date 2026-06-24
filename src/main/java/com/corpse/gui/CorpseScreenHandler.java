package com.corpse.gui;

import com.corpse.CorpseMod;
import com.corpse.entity.CorpseEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CorpseScreenHandler extends AbstractContainerMenu {

    private final Container corpseContainer;
    private final CorpseEntity corpseEntity; // null on client

    // Client-side constructor (called by MenuType factory)
    public CorpseScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, (CorpseEntity) null);
    }

    // Client-side constructor with buffer (for ExtendedScreenHandlerType)
    public CorpseScreenHandler(int syncId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(syncId, playerInventory, (CorpseEntity) null);
    }

    // Server-side constructor
    public CorpseScreenHandler(int syncId, Inventory playerInventory, CorpseEntity corpseEntityIn) {
        super(CorpseMod.CORPSE_SCREEN_HANDLER, syncId);
        this.corpseEntity = corpseEntityIn;

        int totalCorpseSlots = 41; // 36 main + 4 armor + 1 offhand
        this.corpseContainer = new SimpleContainer(totalCorpseSlots);

        // Server: copy items from entity to container
        if (corpseEntity != null && !playerInventory.player.level().isClientSide) {
            int idx = 0;
            for (int i = 0; i < 36; i++) {
                corpseContainer.setItem(idx++, corpseEntity.getMainInventory().get(i).copy());
            }
            for (int i = 0; i < 4; i++) {
                corpseContainer.setItem(idx++, corpseEntity.getArmorInventory().get(i).copy());
            }
            corpseContainer.setItem(idx, corpseEntity.getOffHandInventory().get(0).copy());
        }

        // Corpse main inventory: 4 rows of 9 (slots 0-35)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(corpseContainer, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }

        // Corpse armor + offhand row (slots 36-40)
        this.addSlot(new Slot(corpseContainer, 36, 8, 94));   // Helmet
        this.addSlot(new Slot(corpseContainer, 37, 26, 94));  // Chestplate
        this.addSlot(new Slot(corpseContainer, 38, 44, 94));  // Leggings
        this.addSlot(new Slot(corpseContainer, 39, 62, 94));  // Boots
        this.addSlot(new Slot(corpseContainer, 40, 98, 94));  // Offhand

        // Player inventory: 3 rows of 9 (slots 41-67) — vanilla y=140
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player hotbar: 1 row of 9 (slots 68-76) — vanilla y=198
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index < 41) {
                // From corpse inventory to player inventory
                if (!this.moveItemStackTo(stackInSlot, 41, 77, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to corpse inventory
                if (!this.moveItemStackTo(stackInSlot, 0, 41, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (corpseEntity != null) {
            return corpseEntity.isAlive() && player.distanceToSqr(corpseEntity) <= 64.0;
        }
        return this.corpseContainer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        // Server side: sync container items back to entity
        if (!player.level().isClientSide && corpseEntity != null && corpseEntity.isAlive()) {
            int idx = 0;
            for (int i = 0; i < 36; i++) {
                corpseEntity.getMainInventory().set(i, corpseContainer.getItem(idx++).copy());
            }
            for (int i = 0; i < 4; i++) {
                corpseEntity.getArmorInventory().set(i, corpseContainer.getItem(idx++).copy());
            }
            corpseEntity.getOffHandInventory().set(0, corpseContainer.getItem(idx).copy());
        }
    }
}
