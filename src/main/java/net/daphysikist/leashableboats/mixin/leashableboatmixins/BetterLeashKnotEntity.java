package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.daphysikist.leashableboats.mixin.interfaces.BoatsInterface;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import java.util.List;

@Mixin(LeashKnotEntity.class)
public abstract class BetterLeashKnotEntity extends AbstractDecorationEntity {
    protected BetterLeashKnotEntity(EntityType<? extends LeashKnotEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getNonSpectatingEntities(Ljava/lang/Class;Lnet/minecraft/util/math/Box;)Ljava/util/List;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    protected void boatInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir, boolean bl, double d) {
        List<BoatEntity> boatlist = getWorld().getNonSpectatingEntities(BoatEntity.class, new Box(this.getX()  - 7.0, this.getY()  - 7.0, this.getZ()  - 7.0, this.getX()  + 7.0, this.getY()  + 7.0, this.getZ()  + 7.0));
        for (BoatEntity boatEntity : boatlist) {
            if (((BoatsInterface) boatEntity).getHoldingEntity() != player) continue;
            ((BoatsInterface) boatEntity).attachLeash(this, true);
            bl = true;
        }
    }

    @Inject(method = "canStayAttached", at = @At("RETURN"), cancellable = true)
    protected void wallStayAttached(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.getWorld().getBlockState(this.attachmentPos).isIn(BlockTags.FENCES) || this.getWorld().getBlockState(this.attachmentPos).isIn(BlockTags.WALLS));
    }
}
