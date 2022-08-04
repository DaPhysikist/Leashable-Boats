package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.daphysikist.leashableboats.mixin.interfaces.BoatsInterface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayNetworkHandler.class)
@Environment(value= EnvType.CLIENT)
public abstract class BetterClientHandler implements ClientPlayPacketListener {
    @Shadow
    private MinecraftClient client;

    @Shadow
    private ClientWorld world;

    @Override
    public void onEntityAttach(EntityAttachS2CPacket packet) {
        NetworkThreadUtils.forceMainThread(packet, this, this.client);
        Entity entity = this.world.getEntityById(packet.getAttachedEntityId());
        if (entity instanceof MobEntity) {
            ((MobEntity)entity).setHoldingEntityId(packet.getHoldingEntityId());
        }
        else if (entity instanceof BoatEntity) {
            ((BoatsInterface)entity).setHoldingEntityId(packet.getHoldingEntityId());
        }
    }
}
