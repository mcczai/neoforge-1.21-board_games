package net.mcczai.cardduel.client.gui.screens.inventory;

import net.mcczai.cardduel.CardduelMod;
import net.mcczai.cardduel.items.inventory.CardBundleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class CardBundleScreen extends AbstractContainerScreen<CardBundleMenu> {

    private static final ResourceLocation CARD_BUNDLE_LOCATION = ResourceLocation.fromNamespaceAndPath( CardduelMod.MODID, "textures/gui/container/card_bundle.png");

    public CardBundleScreen(CardBundleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int  mouseX, int  mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CARD_BUNDLE_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
