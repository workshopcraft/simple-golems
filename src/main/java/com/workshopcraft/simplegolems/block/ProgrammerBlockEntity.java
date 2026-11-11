package com.workshopcraft.simplegolems.block;

import com.workshopcraft.simplegolems.menu.ProgrammerMenu;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
//? if >=1.20.5 {
import net.minecraft.core.HolderLookup;
//?}
*///?}

import java.util.List;

public class ProgrammerBlockEntity extends BaseContainerBlockEntity {
    public static final int CARD_SLOT = 0;
    public static final int COPY_SLOT = 1;
    public static final int CARD_SLOTS = 2;
    public static final int FILTER_SLOTS = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(CARD_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> filter = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);

    public ProgrammerBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.PROGRAMMER_ENTITY, pos, state);
    }

    public ItemStack filterAt(int i) {
        return filter.get(i);
    }

    public void setFilter(int i, ItemStack stack) {
        filter.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    public List<ItemStack> filterCopy() {
        List<ItemStack> out = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SLOTS; i++) out.set(i, filter.get(i).copy());
        return out;
    }

    public void loadFilter(List<ItemStack> list) {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            filter.set(i, i < list.size() && !list.get(i).isEmpty() ? list.get(i).copyWithCount(1) : ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplegolems.programmer");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        for (int i = 0; i < CARD_SLOTS; i++) this.items.set(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return CARD_SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ProgrammerMenu(id, inventory, this);
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        List<ItemStack> saved = input.read("filter", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        loadFilter(saved);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.store("filter", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(filter));
    }
    //?} else {
    /*@Override
    //? if >=1.20.5 {
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        filter.clear();
        //? if >=1.21.5 {
        /^for (int i = 0; i < tag.getListOrEmpty("filter").size() && i < FILTER_SLOTS; i++) {
            filter.set(i, ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.getListOrEmpty("filter").get(i)).result().orElse(ItemStack.EMPTY));
        }
        ^///?} else {
        ListTag list = tag.getList("filter", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < FILTER_SLOTS; i++) {
            filter.set(i, ItemStack.parseOptional(registries, list.getCompound(i)));
        }
        //?}
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        ListTag list = new ListTag();
        for (ItemStack s : filter) list.add(s.saveOptional(registries));
        tag.put("filter", list);
    }
    //?} else {
    /^@Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        ListTag list = tag.getList("filter", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < FILTER_SLOTS; i++) filter.set(i, ItemStack.of(list.getCompound(i)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        ListTag list = new ListTag();
        for (ItemStack s : filter) list.add(s.save(new CompoundTag()));
        tag.put("filter", list);
    }
    ^///?}
    *///?}
}
