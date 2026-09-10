package net.mcczai.cardduel.client.duel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 对局视图的"伪屏幕"：不绘制任何内容、不暂停游戏、不遮挡世界。
 *
 * <p>打开期间原版会释放鼠标——显示光标、停止鼠标转视角、不再发出十字准星命中，
 * 而世界照常渲染、HUD 各层照常绘制，卡牌点击仍由 {@link HudClickManager} 的
 * {@code InputEvent.MouseButton.Pre} 处理。
 *
 * <p>生命周期由 {@link DuelCameraManager#onClientTick} 按对局阶段自动管理；
 * ESC 不直接关闭（onClose 空实现），避免光标每帧被抓回。
 */
@OnlyIn(Dist.CLIENT)
public class DuelHudScreen extends Screen {

    public DuelHudScreen() {
        super(Component.empty());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        // 由 DuelCameraManager 的 tick 钩子在对局结束时关闭，这里不响应 ESC
    }
}
