package net.daphysikist.leashableboats.mixin.leashableboatmixins;

import net.daphysikist.leashableboats.mixin.interfaces.BoatsInterface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
@Environment(value= EnvType.CLIENT)
public abstract class BetterClientHandler implements ClientPlayPacketListener {
    @Inject(method = "onEntityAttach", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    protected void boatEntityAttach(EntityAttachS2CPacket packet, CallbackInfo ci, Entity entity) {
        if (entity instanceof BoatEntity) {
            ((BoatsInterface)entity).setHoldingEntityId(packet.getHoldingEntityId());
        }
    }
}
