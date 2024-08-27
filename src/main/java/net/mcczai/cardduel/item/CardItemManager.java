package net.mcczai.cardduel.item;

import com.google.common.collect.Maps;
import net.mcczai.cardduel.CardduelMod;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;
import java.util.Map;

public class CardItemManager {
    public static final Map<String, DeferredHolder<? extends AbstractCardItem,CardItem>> CARD_ITEM_MAP = Maps.newHashMap();

    public static void registerCardItem(String name, DeferredHolder<? extends AbstractCardItem,CardItem> registryObject){
        CARD_ITEM_MAP.put(name,registryObject);
    }

    public static DeferredHolder<? extends AbstractCardItem,CardItem> getCardItemRegistryObject(String key){
        return CARD_ITEM_MAP.get(key);
    }

    public static Collection<DeferredHolder<? extends AbstractCardItem,CardItem>> getAllCardItems(){
        return CARD_ITEM_MAP.values();
    }
}
