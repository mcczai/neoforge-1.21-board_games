package net.mcczai.cardduel.client.resource.serialize;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Objects;

public class ItemStackSerializer implements JsonDeserializer<ItemStack> {

    @Override
    public ItemStack deserialize(@NotNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonObject()){
            JsonObject jsonObject = json.getAsJsonObject();
            String itemName = GsonHelper.getAsString(jsonObject,"item");
            Item item = getItem(itemName,false);
            // TODO:这里应该返回个物品，在1.20.1是CraftingHelper.getItemStack
            return new ItemStack(item,GsonHelper.getAsInt(jsonObject,"count",1));
        }else {
            throw new JsonSyntaxException("Expected" + json + "to be a ItemStack because it's not an object");
        }
    }

    public static Item getItem(String itemName, boolean disallowsAirInRecipe)
    {
        ResourceLocation itemKey =  ResourceLocation.tryBySeparator(itemName,':');
        if (!BuiltInRegistries.ITEM.containsKey(itemKey))
            throw new JsonSyntaxException("Unknown item '" + itemName + "'");

        Item item = BuiltInRegistries.ITEM.get(itemKey);
        if (disallowsAirInRecipe && item == Items.AIR)
            throw new JsonSyntaxException("Invalid item: " + itemName);
        return Objects.requireNonNull(item);
    }

}
