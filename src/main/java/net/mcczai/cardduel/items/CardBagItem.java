package net.mcczai.cardduel.items;

import net.mcczai.cardduel.block.DuelTableBlock;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.duel.DuelEngine;
import net.mcczai.cardduel.init.ModBlocks;
import net.mcczai.cardduel.init.ModDataComponents;
import net.mcczai.cardduel.items.inventory.CardBagContents;
import net.mcczai.cardduel.items.inventory.CardBagMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleMenuProvider;

public class CardBagItem extends Item {

    public CardBagItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (usedHand == InteractionHand.OFF_HAND || player.isCrouching()) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new CardBagMenu(containerId, inventory, itemStack),
                    itemStack.getHoverName()));
        }
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (!state.is(ModBlocks.DUELTABLE_BLOCK.get())) {
            return InteractionResult.PASS;
        }
        DuelTableBlockEntity table = DuelTableBlock.resolvePrimary(level, context.getClickedPos());
        if (table == null) {
            return InteractionResult.PASS;
        }

        if (player.isCrouching()) {
            if (!level.isClientSide) {
                // 仅在已入座时取消准备并提示，避免旁观玩家刷屏
                if (table.getDataFor(player) != null) {
                    table.cancelDeck(player);
                    player.displayClientMessage(Component.translatable("cardduel.bag.cancel_ready"), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide) {
            if (table.getDataFor(player) == null) {
                player.displayClientMessage(Component.translatable("cardduel.bag.not_seated"), true);
                return InteractionResult.sidedSuccess(false);
            }
            CardBagContents contents = context.getItemInHand().getOrDefault(ModDataComponents.CARD_BAG, CardBagContents.EMPTY);
            if (contents.size() < CardBagContents.MAX_SIZE) {
                player.displayClientMessage(Component.translatable("cardduel.bag.not_enough"), true);
            } else {
                table.submitDeck(player, contents.items());
                player.displayClientMessage(Component.translatable("cardduel.bag.ready"), true);
                DuelEngine.notifyDeckReady(table);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
