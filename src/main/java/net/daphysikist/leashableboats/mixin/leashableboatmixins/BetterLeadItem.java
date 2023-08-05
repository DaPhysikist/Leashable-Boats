package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.daphysikist.leashableboats.mixin.interfaces.BoatsInterface;
import net.minecraft.block.BlockState;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.LeadItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(LeadItem.class)
public abstract class BetterLeadItem extends Item {
    public BetterLeadItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockPos blockPos;
        World world = context.getWorld();
        BlockState blockState = world.getBlockState(blockPos = context.getBlockPos());
        if (blockState.isIn(BlockTags.FENCES) || blockState.isIn(BlockTags.WALLS)) {
            PlayerEntity playerEntity = context.getPlayer();
            if (!world.isClient && playerEntity != null) {
                LeadItem.attachHeldMobsToBlock(playerEntity, world, blockPos);
            }
            world.emitGameEvent(GameEvent.BLOCK_ATTACH, blockPos, GameEvent.Emitter.of(playerEntity));
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Inject(method = "attachHeldMobsToBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getNonSpectatingEntities(Ljava/lang/Class;Lnet/minecraft/util/math/Box;)Ljava/util/List;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void attachHeldBoatstoBlock(PlayerEntity player, World world, BlockPos pos, CallbackInfoReturnable<ActionResult> cir, LeashKnotEntity leashKnotEntity, boolean bl, double d, int i, int j, int k) {
        List<BoatEntity> boatlist = world.getNonSpectatingEntities(BoatEntity.class, new Box((double)i - 7.0, (double)j - 7.0, (double)k - 7.0, (double)i + 7.0, (double)j + 7.0, (double)k + 7.0));
        for (BoatEntity boatEntity : boatlist) {
            if (((BoatsInterface)(Object) boatEntity).getHoldingEntity() != player) continue;
            if (leashKnotEntity == null) {
                leashKnotEntity = LeashKnotEntity.getOrCreate(world, pos);
                leashKnotEntity.onPlace();
            }
            ((BoatsInterface)(Object) boatEntity).attachLeash(leashKnotEntity, true);
            bl = true;
        }
    }
}
