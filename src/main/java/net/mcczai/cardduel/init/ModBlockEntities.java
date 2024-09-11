package net.mcczai.cardduel.init;

import net.mcczai.cardduel.block.entity.DuelTableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.mcczai.cardduel.CardduelMod.MODID;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DuelTableBlockEntity>> DUELTABLE =
            BLOCK_ENTITIES.register("dueltable",
            ()-> BlockEntityType.Builder.of(
                    DuelTableBlockEntity::new,
                    ModBlocks.DUELTABLE_BLOCK.get())
                    .build(null));
}
