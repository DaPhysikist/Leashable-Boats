package net.daphysikist.paritymod.mixin.features;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(BatEntity.class)
public abstract class LeashableBats extends Entity{
    private static final String LEASH_KEY = "Leash";

    @Nullable
    private Entity holdingEntity;
    private int holdingEntityId;

    @Nullable
    private NbtCompound leashNbt;

    private BlockPos positionTarget = BlockPos.ORIGIN;
    private float positionTargetRange = -1.0f;

    public LeashableBats(EntityType<?> type, World world) {
        super(type, world);
    }


    @Inject(method = "tick", at = @At("TAIL"))
    public void injectTick(CallbackInfo cir) {
        if (!this.world.isClient) {
            this.updateLeash();
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void injectWriteCustomDataToNbt(NbtCompound nbt, CallbackInfo cir) {
        if (this.holdingEntity != null) {
            Object nbtCompound2 = new NbtCompound();
            if (this.holdingEntity instanceof LivingEntity) {
                UUID uUID = this.holdingEntity.getUuid();
                ((NbtCompound)nbtCompound2).putUuid("UUID", uUID);
            } else if (this.holdingEntity instanceof AbstractDecorationEntity) {
                BlockPos blockPos = ((AbstractDecorationEntity)this.holdingEntity).getDecorationBlockPos();
                ((NbtCompound)nbtCompound2).putInt("X", blockPos.getX());
                ((NbtCompound)nbtCompound2).putInt("Y", blockPos.getY());
                ((NbtCompound)nbtCompound2).putInt("Z", blockPos.getZ());
            }
            nbt.put(LEASH_KEY, (NbtElement)nbtCompound2);
        } else if (this.leashNbt != null) {
            nbt.put(LEASH_KEY, this.leashNbt.copy());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void injectReadCustomDataFromNbt(NbtCompound nbt, CallbackInfo cir) {
        if (nbt.contains(LEASH_KEY, NbtElement.COMPOUND_TYPE)) {
            this.leashNbt = nbt.getCompound(LEASH_KEY);
        }
    }

    public ActionResult interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (this.getHoldingEntity() == player) {
            this.detachLeash(true, !player.getAbilities().creativeMode);
        }
        return ActionResult.PASS;
    }

    public void setPositionTarget(BlockPos target, int range) {
        this.positionTarget = target;
        this.positionTargetRange = range;
    }

    private ActionResult interactWithItem(PlayerEntity player, Hand hand) {
        ActionResult actionResult;
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isOf(Items.LEAD) && this.canBeLeashedBy(player)) {
            this.attachLeash(player, true);
            itemStack.decrement(1);
            return ActionResult.success(this.world.isClient);
        }
        return ActionResult.PASS;
    }
    protected void updateLeash() {
        if (this.leashNbt != null) {
            this.readLeashNbt();
        }
        if (this.holdingEntity == null) {
            return;
        }
        if (!this.isAlive() || !this.holdingEntity.isAlive()) {
            this.detachLeash(true, true);
        }
        Entity entity = this.getHoldingEntity();
        if (entity != null && entity.world == this.world) {
            this.setPositionTarget(entity.getBlockPos(), 5);
            float f = this.distanceTo(entity);
            this.updateForLeashLength(f);
            if (f > 10.0f) {
                this.detachLeash(true, true);
            } else if (f > 6.0f) {
                double d = (entity.getX() - this.getX()) / (double)f;
                double e = (entity.getY() - this.getY()) / (double)f;
                double g = (entity.getZ() - this.getZ()) / (double)f;
                this.setVelocity(this.getVelocity().add(Math.copySign(d * d * 0.4, d), Math.copySign(e * e * 0.4, e), Math.copySign(g * g * 0.4, g)));
            }
        }
    }

    public void detachLeash(boolean sendPacket, boolean dropItem) {
        if (this.holdingEntity != null) {
            this.holdingEntity = null;
            this.leashNbt = null;
            if (!this.world.isClient && dropItem) {
                this.dropItem(Items.LEAD);
            }
            if (!this.world.isClient && sendPacket && this.world instanceof ServerWorld) {
                ((ServerWorld)this.world).getChunkManager().sendToOtherNearbyPlayers(this, new EntityAttachS2CPacket(this, null));
            }
        }
    }

    protected boolean canBeLeashedBy(PlayerEntity player) {
        return !this.isLeashed();
    }

    public boolean isLeashed() {
        return this.holdingEntity != null;
    }

    @Nullable
    public Entity getHoldingEntity() {
        if (this.holdingEntity == null && this.holdingEntityId != 0 && this.world.isClient) {
            this.holdingEntity = this.world.getEntityById(this.holdingEntityId);
        }
        return this.holdingEntity;
    }

    public void attachLeash(Entity entity, boolean sendPacket) {
        this.holdingEntity = entity;
        this.leashNbt = null;
        if (!this.world.isClient && sendPacket && this.world instanceof ServerWorld) {
            ((ServerWorld)this.world).getChunkManager().sendToOtherNearbyPlayers(this, new EntityAttachS2CPacket(this, this.holdingEntity));
        }
        if (this.hasVehicle()) {
            this.stopRiding();
        }
    }

    public void setHoldingEntityId(int id) {
        this.holdingEntityId = id;
        this.detachLeash(false, false);
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        boolean bl = super.startRiding(entity, force);
        if (bl && this.isLeashed()) {
            this.detachLeash(true, true);
        }
        return bl;
    }

    private void readLeashNbt() {
        if (this.leashNbt != null && this.world instanceof ServerWorld) {
            if (this.leashNbt.containsUuid("UUID")) {
                UUID uUID = this.leashNbt.getUuid("UUID");
                Entity entity = ((ServerWorld)this.world).getEntity(uUID);
                if (entity != null) {
                    this.attachLeash(entity, true);
                    return;
                }
            } else if (this.leashNbt.contains("X", NbtElement.NUMBER_TYPE) && this.leashNbt.contains("Y", NbtElement.NUMBER_TYPE) && this.leashNbt.contains("Z", NbtElement.NUMBER_TYPE)) {
                BlockPos blockPos = NbtHelper.toBlockPos(this.leashNbt);
                this.attachLeash(LeashKnotEntity.getOrCreate(this.world, blockPos), true);
                return;
            }
            if (this.age > 100) {
                this.dropItem(Items.LEAD);
                this.leashNbt = null;
            }
        }
    }

    @Override
    protected void removeFromDimension() {
        super.removeFromDimension();
        this.detachLeash(true, false);
    }

    protected boolean shouldFollowLeash() {
        return true;
    }

    protected double getFollowLeashSpeed() {
        return 1.0;
    }

    protected void updateForLeashLength(float leashLength) {
    }
}
