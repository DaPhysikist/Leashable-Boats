package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
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
                return ActionResult.success(this.world.isClient);
            }
            else {
                return this.open(this::emitGameEvent, player);
            }
            return ActionResult.PASS;
        }
        if (!this.canAddPassenger(player))
        {
            return this.open(this::emitGameEvent, player);
        }
        else if (this.ticksUnderwater < 60.0f) {
            if (!this.world.isClient) {
                if (this.getHoldingEntity() == player) {
                    this.detachLeash(true, !player.getAbilities().creativeMode);
                }
                return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
