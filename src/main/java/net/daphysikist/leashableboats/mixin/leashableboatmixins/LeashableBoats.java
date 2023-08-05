package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.daphysikist.leashableboats.mixin.interfaces.BoatsInterface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(value = BoatEntity.class, priority = 800)
    public abstract class LeashableBoats extends Entity implements BoatsInterface {
        private static final String LEASH_KEY = "Leash";

        @Nullable
        private Entity holdingEntity;
        private int holdingEntityId;

        @Shadow
        private double boatYaw;

        private double prevBoatYaw;

        @Shadow
        private float ticksUnderwater;

        @Nullable
        private NbtCompound leashNbt;

        private BlockPos positionTarget = BlockPos.ORIGIN;
        private float positionTargetRange = -1.0f;

        public LeashableBoats(EntityType<?> type, World world) {
            super(type, world);
        }

        public boolean damage(DamageSource source, float amount) {
            if (!this.getWorld().isClient && !this.isRemoved()) {
                if (this.isInvulnerableTo(source)) {
                    return false;
                } else {
                    ((BoatEntity)(Object)this).setDamageWobbleSide(-((BoatEntity)(Object)this).getDamageWobbleSide());
                    ((BoatEntity)(Object)this).setDamageWobbleTicks(10);
                    this.scheduleVelocityUpdate();
                    ((BoatEntity)(Object)this).setDamageWobbleStrength(((BoatEntity)(Object)this).getDamageWobbleStrength() + amount * 10.0F);
                    this.emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());
                    boolean bl = source.getAttacker() instanceof PlayerEntity && ((PlayerEntity)source.getAttacker()).getAbilities().creativeMode;
                    if (bl || ((BoatEntity)(Object)this).getDamageWobbleStrength() > 40.0F) {
                        this.removeAllPassengers();
                        if (bl && !this.hasCustomName()) {
                            this.discard();
                        } else {
                            this.dropItems(source);
                        }
                    }

                    return true;
                }
            } else {
                return true;
            }
        }

        public void dropItems(DamageSource damageSource) {
            this.kill();
            if (this.getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                ItemStack itemStack = new ItemStack(((BoatEntity)(Object)this).asItem());
                if (this.hasCustomName()) {
                    itemStack.setCustomName(this.getCustomName());
                }
                this.dropStack(itemStack);
            }
        }

        @Inject(method = "tick", at = @At("TAIL"))
        public void injectTick(CallbackInfo cir) {
            if (!this.getWorld().isClient) {
                this.updateLeash();
            }
        }

        @Inject (method = "updateTrackedPositionAndAngles", at = @At("TAIL"))
        public void injectUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate, CallbackInfo cir){
            prevBoatYaw = yaw;
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
                    actionResult = this.interactWithItem(player, hand);
                    if (actionResult.isAccepted()) {
                        return actionResult;
                    } else {
                        return ActionResult.PASS;
                    }
                }
            }
            if (this.ticksUnderwater < 60.0f) {
                if (!this.getWorld().isClient) {
                    if (this.getHoldingEntity() == player) {
                        this.detachLeash(true, !player.getAbilities().creativeMode);
                    }
                    return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
                }
                return ActionResult.SUCCESS;
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
                return ActionResult.success(this.getWorld().isClient);
            }
            else {
                return ActionResult.PASS;
            }
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
            if (entity != null && entity.getWorld() == this.getWorld()) {
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

        @Override
        public void detachLeash(boolean sendPacket, boolean dropItem) {
            if (this.holdingEntity != null) {
                this.holdingEntity = null;
                this.leashNbt = null;
                if (!this.getWorld().isClient && dropItem) {
                    this.dropItem(Items.LEAD);
                }
                if (!this.getWorld().isClient && sendPacket && this.getWorld() instanceof ServerWorld) {
                    ((ServerWorld) this.getWorld()).getChunkManager().sendToOtherNearbyPlayers(this, new EntityAttachS2CPacket(this, null));
                }
            }
        }

        protected boolean canBeLeashedBy(PlayerEntity player) {
            return !this.isLeashed();
        }

        @Override
        public boolean isLeashed() {
            return this.holdingEntity != null;
        }

        @Override
        @Nullable
        public Entity getHoldingEntity() {
            if (this.holdingEntity == null && this.holdingEntityId != 0 && this.getWorld().isClient) {
                this.holdingEntity = this.getWorld().getEntityById(this.holdingEntityId);
            }
            return this.holdingEntity;
        }

        public void attachLeash(Entity entity, boolean sendPacket) {
            this.holdingEntity = entity;
            this.leashNbt = null;
            if (!this.getWorld().isClient && sendPacket && this.getWorld() instanceof ServerWorld) {
                ((ServerWorld) this.getWorld()).getChunkManager().sendToOtherNearbyPlayers(this, new EntityAttachS2CPacket(this, this.holdingEntity));
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
            if (this.leashNbt != null && this.getWorld() instanceof ServerWorld) {
                if (this.leashNbt.containsUuid("UUID")) {
                    UUID uUID = this.leashNbt.getUuid("UUID");
                    Entity entity = ((ServerWorld) this.getWorld()).getEntity(uUID);
                    if (entity != null) {
                        this.attachLeash(entity, true);
                        return;
                    }
                } else if (this.leashNbt.contains("X", NbtElement.NUMBER_TYPE) && this.leashNbt.contains("Y", NbtElement.NUMBER_TYPE) && this.leashNbt.contains("Z", NbtElement.NUMBER_TYPE)) {
                    BlockPos blockPos = NbtHelper.toBlockPos(this.leashNbt);
                    this.attachLeash(LeashKnotEntity.getOrCreate(this.getWorld(), blockPos), true);
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

        public double getBoatYaw(){
            return boatYaw;
        }

        public double getPrevBoatYaw(){
            return prevBoatYaw;
        }
    }
