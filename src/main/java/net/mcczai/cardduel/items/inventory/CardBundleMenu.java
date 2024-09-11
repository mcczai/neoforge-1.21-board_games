package net.mcczai.cardduel.items.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CardBundleMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 30;
    private final Container cardBundle;

    public CardBundleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(30));
    }

    public CardBundleMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuType.HOPPER, containerId);
        this.cardBundle = container;
        checkContainerSize(container, 30);
        container.startOpen(playerInventory.player);
        int i = 51;

        for (int j = 0; j < 5; j++) {
            this.addSlot(new Slot(container, j, 44 + j * 18, 20));
        }

        for (int l = 0; l < 3; l++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, l * 18 + 51));
            }
        }

        for (int i1 = 0; i1 < 9; i1++) {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 109));
        }

    }


    @Override
    public  ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < this.cardBundle.getContainerSize()) {
                if (!this.moveItemStackTo(itemstack1, this.cardBundle.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.cardBundle.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public boolean stillValid(@NotNull Player player) {
        return this.cardBundle.stillValid(player);
    }

    public void removed(Player player) {
        super.removed(player);
        this.cardBundle.stopOpen(player);
    }
}
