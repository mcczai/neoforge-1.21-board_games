package net.mcczai.cardduel.client.gui.screens.duel;

import net.mcczai.cardduel.network.payload.ServerboundLeavePayload;
import net.mcczai.cardduel.network.payload.ServerboundSetupPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 对局设置界面：房主输入法力封顶与生命上限。
 */
@OnlyIn(Dist.CLIENT)
public class DuelSetupScreen extends Screen {

    private final BlockPos tablePos;
    private EditBox manaBox;
    private EditBox hpBox;

    public DuelSetupScreen(BlockPos tablePos) {
        super(Component.translatable("cardduel.duel.setup.title"));
        this.tablePos = tablePos;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;

        this.manaBox = new EditBox(this.font, x, 58, 200, 20, Component.translatable("cardduel.duel.setup.mana"));
        this.manaBox.setFilter(text -> text.matches("\\d*"));
        this.manaBox.setMaxLength(3);
        this.manaBox.setValue("10");

        this.hpBox = new EditBox(this.font, x, 96, 200, 20, Component.translatable("cardduel.duel.setup.hp"));
        this.hpBox.setFilter(text -> text.matches("\\d*"));
        this.hpBox.setMaxLength(4);
        this.hpBox.setValue("30");

        this.addRenderableWidget(this.manaBox);
        this.addRenderableWidget(this.hpBox);
        this.addRenderableWidget(Button.builder(Component.translatable("cardduel.duel.setup.confirm"), this::onConfirm)
                .bounds(x, 126, 95, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("cardduel.duel.setup.cancel"), this::onCancel)
                .bounds(x + 105, 126, 95, 20)
                .build());

        this.setInitialFocus(this.manaBox);
    }

    private void onConfirm(Button button) {
        int manaCap = Mth.clamp(parseInt(this.manaBox.getValue(), 10), 1, 99);
        int hpCap = Mth.clamp(parseInt(this.hpBox.getValue(), 30), 1, 999);
        PacketDistributor.sendToServer(new ServerboundSetupPayload(this.tablePos, manaCap, hpCap));
        this.onClose();
    }

    /**
     * 取消：关闭界面并离座（退出准备）。
     */
    private void onCancel(Button button) {
        PacketDistributor.sendToServer(new ServerboundLeavePayload());
        this.onClose();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);

        // 输入框标签：EditBox 的 Component 只是空框时的占位提示，
        // 预填默认值后不再显示，因此这里固定画在框上方。
        int x = this.width / 2 - 100;
        guiGraphics.drawString(this.font, Component.translatable("cardduel.duel.setup.mana"), x, 58 - 12, 0xFFA0A0A0);
        guiGraphics.drawString(this.font, Component.translatable("cardduel.duel.setup.hp"), x, 96 - 12, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
