package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBoatEntity.class)
public abstract class LeashableChestBoats extends LeashableBoats implements RideableInventory,
        VehicleInventory {
    private float ticksUnderwater = getTicksUnderwater();

    public LeashableChestBoats(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ActionResult actionResult;
        ItemStack itemStack = player.getStackInHand(hand);
        if (player.shouldCancelInteraction()) {
            if (itemStack.isOf(Items.LEAD) && this.getHoldingEntity() == player) {
                this.detachLeash(true, !player.getAbilities().creativeMode);
            }
            else if (itemStack.isOf(Items.LEAD) && this.canBeLeashedBy(player)) {
                this.attachLeash(player, true);
                itemStack.decrement(1);
                return ActionResult.success(this.getWorld().isClient);
            }
            else {
                actionResult = this.open(player);
                if (actionResult.isAccepted()) {
                    this.emitGameEvent(GameEvent.CONTAINER_OPEN, player);
                    PiglinBrain.onGuardedBlockInteracted(player, true);
                }
                return actionResult;
            }
        }
        else if (!this.canAddPassenger(player))
        {
            actionResult = this.open(player);
            if (actionResult.isAccepted()) {
                this.emitGameEvent(GameEvent.CONTAINER_OPEN, player);
                PiglinBrain.onGuardedBlockInteracted(player, true);
            }
            return actionResult;
        }
        else if (this.ticksUnderwater < 60.0f) {
            if (!this.getWorld().isClient) {
                if (this.getHoldingEntity() == player) {
                    this.detachLeash(true, !player.getAbilities().creativeMode);
                }
                return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
            }
            return ActionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    public void dropItems(DamageSource source) {
        super.dropItems(source);
        this.onBroken(source, this.getWorld(), this);
    }
}
