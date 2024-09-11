package net.mcczai.cardduel.items;

import net.mcczai.cardduel.init.ModDataComponents;
import net.mcczai.cardduel.items.inventory.CardBundleContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CardBundleItem extends Item {

    public static final int MAX_ROW = 5;

    public CardBundleItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }



//    类似收纳袋的方式，在背包内右键牌进行收纳与取出
    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (stack.getCount() != 1 || action != ClickAction.SECONDARY) {
            return false;
        } else {
            CardBundleContents cardbundlecontents = stack.get(ModDataComponents.CARD_BUNDLE);
            if (cardbundlecontents == null) {
                return false;
            } else {
                ItemStack itemstack = slot.getItem();
                CardBundleContents.Mutable bundlecontents$mutable = new CardBundleContents.Mutable(cardbundlecontents);
                if (itemstack.isEmpty()) {
                    this.playRemoveOneSound(player);
                    ItemStack itemstack1 = bundlecontents$mutable.removeOne();
                    if (itemstack1 != null) {
                        ItemStack itemstack2 = slot.safeInsert(itemstack1);
                        bundlecontents$mutable.tryInsert(itemstack2);
                    }
                } else if (itemstack.getItem().canFitInsideContainerItems()) {
                    int i = bundlecontents$mutable.tryTransfer(slot, player);
                    if (i > 0) {
                        this.playInsertSound(player);
                    }
                }

                stack.set(ModDataComponents.CARD_BUNDLE, bundlecontents$mutable.toImmutable());
                return true;
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (usedHand == InteractionHand.OFF_HAND || player.isCrouching()){
            return  InteractionResultHolder.fail(itemStack);
        }

        if (!level.isClientSide){

        }
        return super.use(level, player, usedHand);
    }

    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private void playDropContentsSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

}
