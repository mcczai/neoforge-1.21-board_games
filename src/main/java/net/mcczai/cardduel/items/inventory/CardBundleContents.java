package net.mcczai.cardduel.items.inventory;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.mcczai.cardduel.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class CardBundleContents implements TooltipComponent {

    public static final Codec<CardBundleContents> CODEC;
    public static final StreamCodec<RegistryFriendlyByteBuf, CardBundleContents> STREAM_CODEC;
    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT;
    final List<ItemStack> items;
    final Fraction weight;

    CardBundleContents(List<ItemStack> items, Fraction weight){
        this.items = items;
        this.weight = weight;
    }

    public CardBundleContents(List<ItemStack> items) {
        this(items, computeContentWeight(items));
    }

    private static Fraction computeContentWeight(List<ItemStack> content) {
        Fraction fraction = Fraction.ZERO;

        ItemStack itemstack;
        for(Iterator var2 = content.iterator(); var2.hasNext(); fraction = fraction.add(getWeight(itemstack).multiplyBy(Fraction.getFraction(itemstack.getCount(), 1)))) {
            itemstack = (ItemStack)var2.next();
        }

        return fraction;
    }

    static Fraction getWeight(ItemStack stack) {
        CardBundleContents cardBundleContents = (CardBundleContents) stack.get(ModDataComponents.CARD_BUNDLE);
        if (cardBundleContents != null) {
            return BUNDLE_IN_BUNDLE_WEIGHT.add(cardBundleContents.weight());
        } else {
            List<BeehiveBlockEntity.Occupant> list = (List)stack.getOrDefault(DataComponents.BEES, List.of());
            return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, stack.getMaxStackSize());
        }
    }
    public ItemStack getItemUnsafe(int index){
        return (ItemStack) this.items.get(index);
    }

    public Stream<ItemStack> itemCopyStream() {
        return this.items.stream().map(ItemStack::copy);
    }

    public Iterable<ItemStack> items() {
        return this.items;
    }

    public Iterable<ItemStack> itemsCopy() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    public int size() {
        return this.items.size();
    }

    public Fraction weight() {
        return this.weight;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else {
            boolean var10000;
            if (other instanceof CardBundleContents) {
                CardBundleContents cardBundleContents = (CardBundleContents) other;
                var10000 = this.weight.equals(cardBundleContents.weight) && ItemStack.listMatches(this.items, cardBundleContents.items);
            } else {
                var10000 = false;
            }

            return var10000;
        }
    }

    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    public String toString() {
        return "CardBundleContents" + String.valueOf(this.items);
    }

    static {
        CODEC = ItemStack.CODEC.listOf().xmap(CardBundleContents::new,(p_331551_) -> {
            return p_331551_.items;
        });
        STREAM_CODEC = ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()).map(CardBundleContents::new, (p_331649_) -> {
            return p_331649_.items;
        });
        BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);
    }

    public static class Mutable{
        private final List<ItemStack> items;
        private Fraction weight;

        public Mutable(CardBundleContents contents){
            this.items = new ArrayList<>(contents.items);
        }

        public Mutable clearItems(){
            this.items.clear();
            return this;
        }

        private int findStackIndex(ItemStack stack){
            if(!stack.isStackable()){
                return -1;
            }else {
                for (int i = 0;i < this.items.size(); ++i){
                    if (ItemStack.isSameItemSameComponents((ItemStack) this.items.get(i),stack))
                        return i;
                }
            }
            return -1;
        }

        private int getMaxAmountToAdd(ItemStack stack) {
            Fraction fraction = Fraction.ONE.subtract(this.weight);
            return Math.max(fraction.divideBy(CardBundleContents.getWeight(stack)).intValue(), 0);
        }

        public int tryInsert(ItemStack stack) {
            if (!stack.isEmpty() && stack.getItem().canFitInsideContainerItems()) {
                int i = Math.min(stack.getCount(), this.getMaxAmountToAdd(stack));
                if (i == 0) {
                    return 0;
                } else {
                    int j = this.findStackIndex(stack);
                    if (j != -1) {
                        ItemStack itemstack = (ItemStack)this.items.remove(j);
                        ItemStack itemstack1 = itemstack.copyWithCount(itemstack.getCount() + i);
                        stack.shrink(i);
                        this.items.add(0, itemstack1);
                    } else {
                        this.items.add(0, stack.split(i));
                    }

                    return i;
                }
            } else {
                return 0;
            }
        }

        public int tryTransfer(Slot slot, Player player) {
            ItemStack itemstack = slot.getItem();
            int i = this.getMaxAmountToAdd(itemstack);
            return this.tryInsert(slot.safeTake(itemstack.getCount(), i, player));
        }

        @Nullable
        public ItemStack removeOne() {
            if (this.items.isEmpty()) {
                return null;
            } else {
                ItemStack itemstack = ((ItemStack)this.items.remove(0)).copy();
                return itemstack;
            }
        }


        public CardBundleContents toImmutable() {
            return new CardBundleContents(List.copyOf(this.items), this.weight);
        }
    }

}
