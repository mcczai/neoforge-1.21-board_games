package net.mcczai.cardduel.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class getPlayerSight {
    public double Pix = 0.03125;

    public static boolean getPlayerLookBlockDirection(BlockPos pos,BlockHitResult result){
        Direction hitFace = result.getDirection();
        Vec3 viewPose = result.getLocation();
        viewPose = new Vec3(viewPose.x - pos.getX(), viewPose.y - pos.getY(), viewPose.z - pos.getZ());
        return false;
    }
}
