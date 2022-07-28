package net.daphysikist.leashableboats.mixin.interfaces;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface BoatsInterface {
    public double getBoatYaw();
    public double getPrevBoatYaw();
    public boolean isLeashed();
    @Nullable
    public Entity getHoldingEntity();
    public void attachLeash(Entity entity, boolean sendPacket);

    public void setHoldingEntityId(int id);

    public void detachLeash(boolean sendPacket, boolean dropItem);

    public static ActionResult attachHeldBoatsToBlock(PlayerEntity player, World world, BlockPos pos) {
        LeashKnotEntity leashKnotEntity2 = null;
        boolean b2 = false;
        int l = pos.getX();
        int m = pos.getY();
        int n = pos.getZ();
        List<BoatEntity> boatlist = world.getNonSpectatingEntities(BoatEntity.class, new Box((double)l - 7.0, (double)m - 7.0, (double)n - 7.0, (double)l + 7.0, (double)m + 7.0, (double)n + 7.0));
        for (BoatEntity boatEntity : boatlist) {
            if (((BoatsInterface)(Object) boatEntity).getHoldingEntity() != player) continue;
            if (leashKnotEntity2 == null) {
                leashKnotEntity2 = LeashKnotEntity.getOrCreate(world, pos);
                leashKnotEntity2.onPlace();
            }
            ((BoatsInterface)(Object) boatEntity).attachLeash(leashKnotEntity2, true);
            b2 = true;
        }
        return b2 ? ActionResult.SUCCESS : ActionResult.PASS;
    }
}
