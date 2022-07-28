package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.daphysikist.leashableboats.mixin.interfaces.BoatsInterface;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.LeadItem;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;

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
        if (blockState.isIn(BlockTags.FENCES)) {
            PlayerEntity playerEntity = context.getPlayer();
            if (!world.isClient && playerEntity != null) {
                LeadItem.attachHeldMobsToBlock(playerEntity, world, blockPos);
                BoatsInterface.attachHeldBoatsToBlock(playerEntity, world, blockPos);
            }
            world.emitGameEvent(GameEvent.BLOCK_ATTACH, blockPos, GameEvent.Emitter.of(playerEntity));
            return ActionResult.success(world.isClient);
        }
        else if (blockState.isIn(BlockTags.WALLS)) {
            PlayerEntity playerEntity = context.getPlayer();
            if (!world.isClient && playerEntity != null) {
                LeadItem.attachHeldMobsToBlock(playerEntity, world, blockPos);
                BoatsInterface.attachHeldBoatsToBlock(playerEntity, world, blockPos);
            }
            world.emitGameEvent(GameEvent.BLOCK_ATTACH, blockPos, GameEvent.Emitter.of(playerEntity));
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }
}
