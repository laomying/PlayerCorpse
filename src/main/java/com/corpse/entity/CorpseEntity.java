package com.corpse.entity;

import com.corpse.CorpseMod;
import com.corpse.gui.CorpseScreenHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.UUID;

public class CorpseEntity extends Entity implements MenuProvider {

    private static final EntityDataAccessor<String> PLAYER_NAME = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PLAYER_UUID_STR = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_SKELETON = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> EQUIPMENT_DATA = SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);

    private NonNullList<ItemStack> mainInventory;
    private NonNullList<ItemStack> armorInventory;
    private NonNullList<ItemStack> offHandInventory;
    private EnumMap<EquipmentSlot, ItemStack> equipment;

    private int age;
    private int emptyAge;

    private static final int SKELETON_TIME = 72000;
    private static final int EMPTY_DESPAWN_TIME = 6000;

    public CorpseEntity(EntityType<?> type, Level level) {
        super(type, level);
        mainInventory = NonNullList.withSize(36, ItemStack.EMPTY);
        armorInventory = NonNullList.withSize(4, ItemStack.EMPTY);
        offHandInventory = NonNullList.withSize(1, ItemStack.EMPTY);
        equipment = new EnumMap<>(EquipmentSlot.class);
        blocksBuilding = true;
        emptyAge = -1;
    }

    public CorpseEntity(Level level) {
        this(CorpseMod.CORPSE_ENTITY_TYPE, level);
    }

    public static CorpseEntity createFromPlayer(ServerPlayer player) {
        CorpseEntity corpse = new CorpseEntity(player.level());

        corpse.setPlayerName(player.getGameProfile().getName());
        corpse.setPlayerUuidStr(player.getGameProfile().getId().toString());
        corpse.setPos(player.getX(), Math.max(player.getY(), player.level().getMinBuildHeight()), player.getZ());
        corpse.setYRot(player.getYRot());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot).copy();
            if (!stack.isEmpty()) {
                corpse.equipment.put(slot, stack);
            }
        }

        if (!player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            for (int i = 0; i < 36; i++) {
                corpse.mainInventory.set(i, player.getInventory().items.get(i).copy());
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < 4; i++) {
                corpse.armorInventory.set(i, player.getInventory().armor.get(i).copy());
                player.getInventory().armor.set(i, ItemStack.EMPTY);
            }
            corpse.offHandInventory.set(0, player.getInventory().offhand.get(0).copy());
            player.getInventory().offhand.set(0, ItemStack.EMPTY);
        }

        corpse.syncEquipmentData();
        return corpse;
    }

    private void syncEquipmentData() {
        // Simple text format: "SLOT:namespace:item_id;..."
        // Example: "HEAD:minecraft:iron_helmet;CHEST:fantasy_armor:dragon_chestplate"
        StringBuilder sb = new StringBuilder();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = equipment.get(slot);
            if (stack != null && !stack.isEmpty()) {
                if (sb.length() > 0) sb.append(';');
                sb.append(slot.name()).append(':')
                        .append(net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(stack.getItem()).toString());
            }
        }
        entityData.set(EQUIPMENT_DATA, sb.toString());
    }

    public EnumMap<EquipmentSlot, ItemStack> getEquipment() {
        if (level().isClientSide) {
            String equipStr = entityData.get(EQUIPMENT_DATA);
            EnumMap<EquipmentSlot, ItemStack> eq = new EnumMap<>(EquipmentSlot.class);
            if (equipStr.isEmpty()) return eq;
            try {
                // Parse "SLOT:namespace:path" format (colon in slot separator, then colon in ResourceLocation)
                for (String part : equipStr.split(";")) {
                    int firstColon = part.indexOf(':');
                    if (firstColon < 0) continue;
                    String slotName = part.substring(0, firstColon);
                    String itemId = part.substring(firstColon + 1);
                    EquipmentSlot slot = EquipmentSlot.valueOf(slotName);
                    net.minecraft.world.item.Item item =
                            net.minecraft.core.registries.BuiltInRegistries.ITEM
                                    .get(net.minecraft.resources.ResourceLocation.parse(itemId));
                    if (item != null) {
                        eq.put(slot, new ItemStack(item));
                    }
                }
            } catch (Exception e) {
                CorpseMod.LOGGER.warn("Failed to parse equipment: {}", e.getMessage());
            }
            return eq;
        }
        return equipment;
    }

    @Override
    public void tick() {
        super.tick();

        // Re-sync equipment data on every tick — needed for chunk reload scenarios
        // where EntityDataAccessor may not have been sent after readAdditionalSaveData.
        // entityData.set() internally only sends an update when the value changes.
        if (!level().isClientSide) {
            syncEquipmentData();
        }

        if (!isNoGravity()) {
            double yMotion;
            Vec3 motion = getDeltaMovement();
            if (isEyeInFluid(FluidTags.WATER) || isEyeInFluid(FluidTags.LAVA)) {
                yMotion = (motion.y < 0.03D) ? (motion.y + (motion.y < 0.03D ? 0.01D : 0D)) : (motion.y + 5E-4D);
            } else {
                yMotion = Math.max(-2D, motion.y - 0.0625D);
            }
            setDeltaMovement(motion.x * 0.75D, yMotion, motion.z * 0.75D);

            if (getY() < level().getMinBuildHeight()) {
                teleportTo(getX(), level().getMinBuildHeight(), getZ());
            }

            move(MoverType.SELF, getDeltaMovement());
        }

        if (level().isClientSide) {
            return;
        }

        age++;

        if (age >= SKELETON_TIME && !isSkeleton()) {
            setIsSkeleton(true);
        }

        boolean empty = isInventoryEmpty();
        if (empty && emptyAge < 0) {
            emptyAge = age;
        } else if (!empty) {
            emptyAge = -1;
        }

        if (empty && emptyAge >= 0 && age - emptyAge >= EMPTY_DESPAWN_TIME) {
            discard();
        }
    }

    public boolean isInventoryEmpty() {
        for (ItemStack stack : mainInventory) {
            if (!stack.isEmpty()) return false;
        }
        for (ItemStack stack : armorInventory) {
            if (!stack.isEmpty()) return false;
        }
        for (ItemStack stack : offHandInventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE) && amount >= 2F) {
            discard();
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    // MenuProvider implementation
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CorpseScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        String name = getPlayerName();
        if (name == null || name.trim().isEmpty()) {
            return Component.translatable("entity.corpse.corpse");
        }
        return Component.translatable("entity.corpse.corpse_of", name);
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    public String getPlayerName() {
        return entityData.get(PLAYER_NAME);
    }

    private void setPlayerName(String name) {
        entityData.set(PLAYER_NAME, name);
    }

    public UUID getPlayerUuid() {
        String uuidStr = getPlayerUuidStr();
        if (uuidStr.isEmpty()) return new UUID(0, 0);
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return new UUID(0, 0);
        }
    }

    public String getPlayerUuidStr() {
        return entityData.get(PLAYER_UUID_STR);
    }

    private void setPlayerUuidStr(String uuidStr) {
        entityData.set(PLAYER_UUID_STR, uuidStr);
    }

    public boolean isSkeleton() {
        return entityData.get(IS_SKELETON);
    }

    private void setIsSkeleton(boolean skeleton) {
        entityData.set(IS_SKELETON, skeleton);
    }

    public NonNullList<ItemStack> getMainInventory() {
        return mainInventory;
    }

    public NonNullList<ItemStack> getArmorInventory() {
        return armorInventory;
    }

    public NonNullList<ItemStack> getOffHandInventory() {
        return offHandInventory;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PLAYER_NAME, "");
        builder.define(PLAYER_UUID_STR, "");
        builder.define(IS_SKELETON, false);
        builder.define(EQUIPMENT_DATA, "");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        mainInventory = NonNullList.withSize(36, ItemStack.EMPTY);
        armorInventory = NonNullList.withSize(4, ItemStack.EMPTY);
        offHandInventory = NonNullList.withSize(1, ItemStack.EMPTY);

        if (compound.contains("MainInventory")) {
            ContainerHelper.loadAllItems(compound.getCompound("MainInventory"), mainInventory, registryAccess());
        }
        if (compound.contains("ArmorInventory")) {
            ContainerHelper.loadAllItems(compound.getCompound("ArmorInventory"), armorInventory, registryAccess());
        }
        if (compound.contains("OffHandInventory")) {
            ContainerHelper.loadAllItems(compound.getCompound("OffHandInventory"), offHandInventory, registryAccess());
        }

        age = compound.getInt("Age");
        emptyAge = compound.getInt("EmptyAge");

        setPlayerName(compound.getString("PlayerName"));
        setPlayerUuidStr(compound.getString("PlayerUUID"));
        setIsSkeleton(compound.getBoolean("Skeleton"));

        equipment = new EnumMap<>(EquipmentSlot.class);
        if (compound.contains("Equipment")) {
            CompoundTag equipTag = compound.getCompound("Equipment");
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (equipTag.contains(slot.name())) {
                    equipment.put(slot, ItemStack.parseOptional(registryAccess(), equipTag.getCompound(slot.name())));
                }
            }
        }
        syncEquipmentData();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        CompoundTag mainTag = new CompoundTag();
        ContainerHelper.saveAllItems(mainTag, mainInventory, registryAccess());
        compound.put("MainInventory", mainTag);

        CompoundTag armorTag = new CompoundTag();
        ContainerHelper.saveAllItems(armorTag, armorInventory, registryAccess());
        compound.put("ArmorInventory", armorTag);

        CompoundTag offTag = new CompoundTag();
        ContainerHelper.saveAllItems(offTag, offHandInventory, registryAccess());
        compound.put("OffHandInventory", offTag);

        compound.putInt("Age", age);
        compound.putInt("EmptyAge", emptyAge);
        compound.putString("PlayerName", getPlayerName());
        compound.putString("PlayerUUID", getPlayerUuidStr());
        compound.putBoolean("Skeleton", isSkeleton());

        CompoundTag equipTag = new CompoundTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = equipment.get(slot);
            if (stack != null && !stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                stack.save(registryAccess(), itemTag);
                equipTag.put(slot.name(), itemTag);
            }
        }
        compound.put("Equipment", equipTag);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            for (ItemStack stack : mainInventory) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack);
                }
            }
            for (ItemStack stack : armorInventory) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack);
                }
            }
            for (ItemStack stack : offHandInventory) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack);
                }
            }
        }
        super.remove(reason);
    }
}
