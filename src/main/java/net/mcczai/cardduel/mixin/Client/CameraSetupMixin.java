package net.mcczai.cardduel.mixin.Client;

import net.mcczai.cardduel.client.duel.ClientDuelState;
import net.mcczai.cardduel.client.duel.DuelCameraManager;
import net.mcczai.cardduel.network.payload.ClientboundDuelSyncPayload;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 对局俯视相机（关键修复）。
 *
 * <p>1.21 的渲染流程里，{@code RenderLevelStageEvent}（含 AFTER_SKY）触发时，
 * 地形渲染使用的视图矩阵已经用"旧"相机参数构建完毕——在事件里改相机为时已晚，
 * 视图不会变化。必须把相机改写放在 {@code Camera.setup} 完成之后、渲染开始之前，
 * 这里用 mixin 注入 setup 的 TAIL 实现。
 */
@Mixin(Camera.class)
public class CameraSetupMixin {

    @Inject(method = "setup", at = @At("TAIL"))
    private void cardduel$forceDuelTopDown(BlockGetter level, Entity entity, boolean detached,
                                           boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        ClientboundDuelSyncPayload sync = ClientDuelState.get();
        if (sync == null || !DuelCameraManager.isActivePhase(sync.phase())) {
            return;
        }
        Direction facing = Direction.byName(sync.facing());
        if (facing == null) {
            return;
        }
        // 双桌中心 = 主桌中心 + 朝向方向半步（双桌沿 facing 排列）
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 center = sync.tablePos().getCenter().add(dir.scale(0.5D));

        Camera camera = (Camera) (Object) this;
        camera.setPosition(center.x, center.y + DuelCameraManager.CAMERA_HEIGHT, center.z);
        camera.setRotation(0.0F, 90.0F, 0.0F);
    }
}
