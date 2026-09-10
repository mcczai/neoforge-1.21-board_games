package net.mcczai.cardduel.block;

import com.mojang.serialization.MapCodec;
import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.mcczai.cardduel.duel.DuelEngine;
import net.mcczai.cardduel.init.ModBlocks;
import net.mcczai.cardduel.items.CardBagItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class DuelTableBlock extends BaseEntityBlock {

    public static final MapCodec<DuelTableBlock> CODEC = simpleCodec(DuelTableBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty DOUBLE = BooleanProperty.create("double");

    public DuelTableBlock(@NotNull Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(
            this.stateDefinition
                    .any()
                    .setValue(FACING,Direction.NORTH)
                    .setValue(DOUBLE, false)
        );
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockGetter blockGetter = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockState northState = blockGetter.getBlockState(blockPos.relative(Direction.NORTH));
        BlockState southState = blockGetter.getBlockState(blockPos.relative(Direction.SOUTH));
        if (northState.is(ModBlocks.DUELTABLE_BLOCK.get())) {
            return this.defaultBlockState()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(DOUBLE, true);
        }
        if (southState.is(ModBlocks.DUELTABLE_BLOCK.get())) {
            return this.defaultBlockState()
                    .setValue(FACING, Direction.SOUTH)
                    .setValue(DOUBLE, true);
        }
        return this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(DOUBLE, false);
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction,
                                              @NotNull BlockState neighborState, @NotNull LevelAccessor level,
                                              @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        BlockState northState = level.getBlockState(pos.relative(Direction.NORTH));
        BlockState southState = level.getBlockState(pos.relative(Direction.SOUTH));
        boolean northTable = northState.is(ModBlocks.DUELTABLE_BLOCK.get());
        boolean southTable = southState.is(ModBlocks.DUELTABLE_BLOCK.get());
        if (northTable && !southTable) {
            return state.setValue(FACING, Direction.NORTH).setValue(DOUBLE, true);
        }
        if (southTable && !northTable) {
            return state.setValue(FACING, Direction.SOUTH).setValue(DOUBLE, true);
        }
        return state.setValue(FACING, Direction.NORTH).setValue(DOUBLE, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING,DOUBLE);
    }

    /**
     * 解析双桌对的"主桌"方块实体：所有右键交互（入座/离座/提交牌组/破坏清座）
     * 一律以主桌 BE 为唯一状态载体。规则：点击格无房主且配对格有房主 → 配对格；
     * 其余情况（单桌 / 点击格已有房主 / 两格都无房主）→ 点击格自身。
     * FACING 恒指向配桌方向（updateShape 保证），故可从任意一格确定性地定位配对格。
     */
    @Nullable
    public static DuelTableBlockEntity resolvePrimary(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.DUELTABLE_BLOCK.get())
                || !(level.getBlockEntity(pos) instanceof DuelTableBlockEntity clicked)) {
            return null;
        }
        if (state.getValue(DOUBLE) && clicked.getHostUuid() == null) {
            BlockPos partnerPos = pos.relative(state.getValue(FACING));
            if (level.getBlockState(partnerPos).is(ModBlocks.DUELTABLE_BLOCK.get())
                    && level.getBlockEntity(partnerPos) instanceof DuelTableBlockEntity partner
                    && partner.getHostUuid() != null) {
                return partner;
            }
        }
        return clicked;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                            @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            DuelTableBlockEntity table = resolvePrimary(level, pos);
            if (table != null) {
                table.clearSeatsOnBreak(level.getServer());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                        @NotNull BlockPos pos, @NotNull Player player,
                                                        @NotNull BlockHitResult hitResult) {
        // 手持卡牌袋时把交互让给 CardBagItem.useOn（提交/取消牌组）。
        // 1.21 的交互顺序是 方块useItemOn → 方块useWithoutItem → 物品useOn：
        // 这里若消费掉交互，物品的 useOn 永远轮不到，卡包右键牌桌会变成"空手点桌"。
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof CardBagItem) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        DuelTableBlockEntity table = resolvePrimary(level, pos);
        if (player instanceof ServerPlayer serverPlayer && table != null) {
            if (player.isShiftKeyDown()) {
                return DuelEngine.handleLeave(serverPlayer, table);
            }
            return DuelEngine.handleTableUse(serverPlayer, table);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new DuelTableBlockEntity(blockPos,blockState);
    }
}
