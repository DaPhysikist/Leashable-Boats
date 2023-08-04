package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.daphysikist.leashableboats.mixin.interfaces.BoatsInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(LeashKnotEntity.class)
public abstract class BetterLeashKnotEntity extends AbstractDecorationEntity {
    protected BetterLeashKnotEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        }
        boolean bl = false;
        double d = 7.0;
        List<MobEntity> list = this.world.getNonSpectatingEntities(MobEntity.class, new Box(this.getX() - 7.0, this.getY() - 7.0, this.getZ() - 7.0, this.getX() + 7.0, this.getY() + 7.0, this.getZ() + 7.0));
        List<BoatEntity> boatlist = world.getNonSpectatingEntities(BoatEntity.class, new Box(this.getX()  - 7.0, this.getY()  - 7.0, this.getZ()  - 7.0, this.getX()  + 7.0, this.getY()  + 7.0, this.getZ()  + 7.0));

        for (MobEntity mobEntity : list) {
            if (mobEntity.getHoldingEntity() != player) continue;
            mobEntity.attachLeash(this, true);
            bl = true;
        }

       for (BoatEntity boatEntity : boatlist) {
            if (((BoatsInterface) boatEntity).getHoldingEntity() != player) continue;
           ((BoatsInterface) boatEntity).attachLeash(this, true);
            bl = true;
        }

        if (!bl) {
            this.discard();
            if (player.getAbilities().creativeMode) {
                for (MobEntity mobEntity : list) {
                    if (!mobEntity.isLeashed() || mobEntity.getHoldingEntity() != this) continue;
                    mobEntity.detachLeash(true, false);
                }
            }

            if (player.getAbilities().creativeMode) {
                for (BoatEntity boatEntity : boatlist) {
                    if (!((BoatsInterface) boatEntity).isLeashed() || ((BoatsInterface) boatEntity).getHoldingEntity() != this) continue;
                    ((BoatsInterface) boatEntity).detachLeash(true, false);
                }
            }
        }
        return ActionResult.CONSUME;
    }

    @Override
    public boolean canStayAttached() {
        if (this.world.getBlockState(this.attachmentPos).isIn(BlockTags.FENCES)){
            return true;
        }
        else return this.world.getBlockState(this.attachmentPos).isIn(BlockTags.WALLS);
    }
}
