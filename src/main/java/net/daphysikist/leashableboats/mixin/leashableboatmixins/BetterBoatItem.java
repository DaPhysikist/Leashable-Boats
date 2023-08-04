package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.item.BoatItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

@Mixin(BoatItem.class)
public class BetterBoatItem extends Item {
    @Shadow
    private static final Predicate<Entity> RIDERS;

    @Shadow
    private final BoatEntity.Type type;

    @Shadow
    private final boolean chest;

    public BetterBoatItem(boolean chest, BoatEntity.Type type, Item.Settings settings) {
        super(settings);
        this.chest = chest;
        this.type = type;
    }

    @Overwrite
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        HitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(itemStack);
        } else {
            Vec3d vec3d = user.getRotationVec(1.0F);
            double d = 5.0;
            List<Entity> list = world.getOtherEntities(user, user.getBoundingBox().stretch(vec3d.multiply(5.0)).expand(1.0), RIDERS);
            if (!list.isEmpty()) {
                Vec3d vec3d2 = user.getEyePos();
                Iterator var11 = list.iterator();

                while(var11.hasNext()) {
                    Entity entity = (Entity)var11.next();
                    Box box = entity.getBoundingBox().expand((double)entity.getTargetingMargin());
                    if (box.contains(vec3d2)) {
                        return TypedActionResult.pass(itemStack);
                    }
                }
            }

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BoatEntity boatEntity = this.createEntity(world, hitResult);
                boatEntity.setVariant(this.type);
                boatEntity.setYaw(user.getYaw());
                if (!world.isSpaceEmpty(boatEntity, boatEntity.getBoundingBox())) {
                    return TypedActionResult.fail(itemStack);
                } else {
                    if (!world.isClient) {
                        if (itemStack.hasCustomName()) {
                            boatEntity.setCustomName(itemStack.getName());
                            boatEntity.setCustomNameVisible(true);
                        }
                        world.spawnEntity(boatEntity);
                        world.emitGameEvent(user, GameEvent.ENTITY_PLACE, hitResult.getPos());
                        if (!user.getAbilities().creativeMode) {
                            itemStack.decrement(1);
                        }
                    }
                    user.incrementStat(Stats.USED.getOrCreateStat((Item)(Object)this));
                    return TypedActionResult.success(itemStack, world.isClient());
                }
            } else {
                return TypedActionResult.pass(itemStack);
            }
        }
    }

    @Overwrite
    private BoatEntity createEntity(World world, HitResult hitResult) {
        return (BoatEntity)(this.chest ? new ChestBoatEntity(world, hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z) : new BoatEntity(world, hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z));
    }

    static {
        RIDERS = EntityPredicates.EXCEPT_SPECTATOR.and(Entity::canHit);
    }
}
