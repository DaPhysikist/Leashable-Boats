package net.daphysikist.leashableboats.mixin.interfaces;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface BoatsInterface {
    double getBoatYaw();
    double getPrevBoatYaw();
    boolean isLeashed();
    @Nullable
    Entity getHoldingEntity();
    void attachLeash(Entity entity, boolean sendPacket);
    void setHoldingEntityId(int id);
    void detachLeash(boolean sendPacket, boolean dropItem);
}
