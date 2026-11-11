package com.workshopcraft.simplegolems.menu;

import com.workshopcraft.simplegolems.block.ProgrammerBlockEntity;
import com.workshopcraft.simplegolems.item.GolemProgram;
import com.workshopcraft.simplegolems.item.ProgramCardItem;
import com.workshopcraft.simplegolems.registry.ModContent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.minecraft.world.inventory.ClickType;
*///?}

public class ProgrammerMenu extends AbstractContainerMenu {
    public static final int CARD_SLOT = ProgrammerBlockEntity.CARD_SLOT;
    public static final int COPY_SLOT = ProgrammerBlockEntity.COPY_SLOT;
    public static final int FILTER_SLOTS = ProgrammerBlockEntity.FILTER_SLOTS;
    public static final int BTN_COPY = 0;
    public static final int BTN_CLEAR = 1;

    private final Container cards;
    private final SimpleContainer ghosts = new SimpleContainer(FILTER_SLOTS);
    private final @Nullable ProgrammerBlockEntity programmer;
    private boolean loading;

    public static ProgrammerMenu client(int id, Inventory inventory) {
        return new ProgrammerMenu(id, inventory, new SimpleContainer(ProgrammerBlockEntity.CARD_SLOTS), null);
    }

    public ProgrammerMenu(int id, Inventory inventory, ProgrammerBlockEntity programmer) {
        this(id, inventory, programmer, programmer);
        loadFromCard();
    }

    private ProgrammerMenu(int id, Inventory inventory, Container cards, @Nullable ProgrammerBlockEntity programmer) {
        super(ModContent.PROGRAMMER_MENU, id);
        this.cards = cards;
        this.programmer = programmer;
        if (programmer != null) {
            for (int i = 0; i < FILTER_SLOTS; i++) ghosts.setItem(i, programmer.filterAt(i).copy());
        }
        addSlot(new CardSlot(cards, CARD_SLOT, 17, 27));
        addSlot(new CardSlot(cards, COPY_SLOT, 17, 53));
        for (int i = 0; i < FILTER_SLOTS; i++) {
            addSlot(new Slot(ghosts, i, 63 + i % 3 * 18, 18 + i / 3 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        //? if >=26.1 {
        addStandardInventorySlots(inventory, 8, 84);
        //?} else {
        /*for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        *///?}
    }

    private static boolean isCard(ItemStack stack) {
        return stack.is(ModContent.PROGRAM_CARD_ITEM);
    }

    private void loadFromCard() {
        loading = true;
        GolemProgram p = isCard(cards.getItem(CARD_SLOT)) ? ProgramCardItem.program(cards.getItem(CARD_SLOT)) : null;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack s = p == null || i >= p.paddedFilter().size() ? ItemStack.EMPTY : p.paddedFilter().get(i);
            ghosts.setItem(i, s);
            if (programmer != null) programmer.setFilter(i, s);
        }
        loading = false;
    }

    private void writeFilter() {
        if (loading) return;
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack s = ghosts.getItem(i).copy();
            if (programmer != null) programmer.setFilter(i, s);
            list.add(s);
        }
        ItemStack card = cards.getItem(CARD_SLOT);
        if (!isCard(card)) return;
        ProgramCardItem.write(card, new GolemProgram(GolemProgram.Kind.COURIER_FILTER, list));
        cards.setItem(CARD_SLOT, card);
    }

    private void setGhost(int slot, ItemStack stack) {
        ItemStack one = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        ghosts.setItem(slot, one);
        writeFilter();
    }

    @Override
    //? if >=26.1 {
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 2 && slotId < 2 + FILTER_SLOTS) {
            if (input == ContainerInput.PICKUP) setGhost(slotId - 2, getCarried());
            else if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.THROW) setGhost(slotId - 2, ItemStack.EMPTY);
            return;
        }
        ItemStack before = cards.getItem(CARD_SLOT).copy();
        super.clicked(slotId, button, input, player);
        if (slotId == CARD_SLOT && !ItemStack.matches(before, cards.getItem(CARD_SLOT))) loadFromCard();
    }
    //?} else {
    /*public void clicked(int slotId, int button, ClickType input, Player player) {
        if (slotId >= 2 && slotId < 2 + FILTER_SLOTS) {
            if (input == ClickType.PICKUP) setGhost(slotId - 2, getCarried());
            else if (input == ClickType.QUICK_MOVE || input == ClickType.THROW) setGhost(slotId - 2, ItemStack.EMPTY);
            return;
        }
        ItemStack before = cards.getItem(CARD_SLOT).copy();
        super.clicked(slotId, button, input, player);
        if (slotId == CARD_SLOT && !ItemStack.matches(before, cards.getItem(CARD_SLOT))) loadFromCard();
    }
    *///?}

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int invStart = 2 + FILTER_SLOTS;
        if (index == CARD_SLOT || index == COPY_SLOT) {
            if (!moveItemStackTo(stack, invStart, slots.size(), true)) return ItemStack.EMPTY;
            slot.setChanged();
            return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
        if (index >= invStart && isCard(stack)) {
            if (cards.getItem(CARD_SLOT).isEmpty() && moveItemStackTo(stack, CARD_SLOT, CARD_SLOT + 1, false)) {
                loadFromCard();
                return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
            if (cards.getItem(COPY_SLOT).isEmpty() && moveItemStackTo(stack, COPY_SLOT, COPY_SLOT + 1, false)) {
                return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
        }
        if (index >= invStart) {
            int free = -1;
            for (int i = 0; i < FILTER_SLOTS; i++) {
                ItemStack g = ghosts.getItem(i);
                if (g.isEmpty()) {
                    if (free < 0) free = i;
                } else if (g.is(stack.getItem())) return ItemStack.EMPTY;
            }
            if (free >= 0) setGhost(free, stack);
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_CLEAR) {
            loading = true;
            for (int i = 0; i < FILTER_SLOTS; i++) {
                ghosts.setItem(i, ItemStack.EMPTY);
                if (programmer != null) programmer.setFilter(i, ItemStack.EMPTY);
            }
            loading = false;
            ItemStack card = cards.getItem(CARD_SLOT);
            if (isCard(card)) {
                ProgramCardItem.write(card, null);
                cards.setItem(CARD_SLOT, card);
            }
            return true;
        }
        if (id == BTN_COPY) {
            ItemStack dest = cards.getItem(COPY_SLOT);
            if (!isCard(dest)) return false;
            java.util.List<ItemStack> list = new java.util.ArrayList<>();
            for (int i = 0; i < FILTER_SLOTS; i++) list.add(ghosts.getItem(i).copy());
            ProgramCardItem.write(dest, new GolemProgram(GolemProgram.Kind.COURIER_FILTER, list));
            cards.setItem(COPY_SLOT, dest);
            return true;
        }
        return false;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container != ghosts;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != ghosts;
    }

    @Override
    public boolean stillValid(Player player) {
        return programmer == null || Container.stillValidBlockEntity(programmer, player);
    }

    private static class CardSlot extends Slot {
        CardSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isCard(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
