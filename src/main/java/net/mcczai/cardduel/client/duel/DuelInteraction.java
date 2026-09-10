package net.mcczai.cardduel.client.duel;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

/**
 * 对局中的客户端选中状态：
 *  - selectedHand：选中的手牌索引（出牌）
 *  - selectedBoard：选中的己方战场槽位（攻击）
 *  - mulliganSelection：换牌阶段选中的手牌索引（≤2）
 */
@OnlyIn(Dist.CLIENT)
public final class DuelInteraction {

    private static int selectedHand = -1;
    private static int selectedBoard = -1;
    private static boolean mulliganDone;
    /** 拖拽出牌：按下手牌/己方召唤物后进入拖拽，松开时结算 */
    private static int draggingHand = -1;
    private static int draggingBoard = -1;
    private static final Set<Integer> MULLIGAN_SELECTION = new HashSet<>();

    private DuelInteraction() {
    }

    public static int getSelectedHand() {
        return selectedHand;
    }

    public static void toggleHand(int index) {
        selectedHand = selectedHand == index ? -1 : index;
        selectedBoard = -1;
    }

    public static int getSelectedBoard() {
        return selectedBoard;
    }

    public static void toggleBoard(int slot) {
        selectedBoard = selectedBoard == slot ? -1 : slot;
        selectedHand = -1;
    }

    public static Set<Integer> getMulliganSelection() {
        return MULLIGAN_SELECTION;
    }

    public static void toggleMulligan(int index) {
        if (MULLIGAN_SELECTION.contains(index)) {
            MULLIGAN_SELECTION.remove(index);
        } else if (MULLIGAN_SELECTION.size() < 2) {
            MULLIGAN_SELECTION.add(index);
        }
    }

    public static void clearMulligan() {
        MULLIGAN_SELECTION.clear();
    }

    /** 本回合换牌是否已确认（客户端状态：确认后按钮/提示切换为等待对手） */
    public static boolean isMulliganDone() {
        return mulliganDone;
    }

    public static void setMulliganDone(boolean done) {
        mulliganDone = done;
    }

    // ==================== 拖拽出牌 ====================

    public static int getDraggingHand() {
        return draggingHand;
    }

    public static int getDraggingBoard() {
        return draggingBoard;
    }

    public static boolean isDragging() {
        return draggingHand >= 0 || draggingBoard >= 0;
    }

    public static void startDragHand(int index) {
        draggingHand = index;
        draggingBoard = -1;
    }

    public static void startDragBoard(int slot) {
        draggingBoard = slot;
        draggingHand = -1;
    }

    public static void stopDrag() {
        draggingHand = -1;
        draggingBoard = -1;
    }

    /** 把拖拽目标临时设为选中（供 handleTableClick 复用整套出牌/攻击分支） */
    public static void selectHand(int index) {
        selectedHand = index;
        selectedBoard = -1;
    }

    public static void selectBoard(int slot) {
        selectedBoard = slot;
        selectedHand = -1;
    }

    public static void clear() {
        selectedHand = -1;
        selectedBoard = -1;
        MULLIGAN_SELECTION.clear();
        draggingHand = -1;
        draggingBoard = -1;
        // 注意：不能在这里重置 mulliganDone——
        // 点击空白处会走 clear()，一旦重置，确认换牌后点别处就能重新改选/重复发送；
        // mulliganDone 的复位统一由 BattleBoardHud 在离开 MULLIGAN 阶段时完成。
    }
}
