package net.mcczai.cardduel;

import net.mcczai.cardduel.init.ModBlock;
import net.mcczai.cardduel.init.ModDataComponents;
import net.mcczai.cardduel.init.ModItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CardduelMod.MODID)
public class CardduelMod {

    public static final String MODID = "cardduel";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public CardduelMod(IEventBus Bus, ModContainer modContainer){
        ModBlock.BLOCKS.register(Bus);
        ModItem.ITEMS.register(Bus);
        ModDataComponents.DATA_COMPONENTS.register(Bus);
        CardduelCreativeTab.CARDDUEL_TABS.register(Bus);
    }
}
