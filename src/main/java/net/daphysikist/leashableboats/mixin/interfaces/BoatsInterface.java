package net.daphysikist.leashableboats.mixin.interfaces;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface BoatsInterface {
    public double getBoatYaw();
    public double getPrevBoatYaw();
    public boolean isLeashed();
    @Nullable
    public Entity getHoldingEntity();
    public void attachLeash(Entity entity, boolean sendPacket);
    public void setHoldingEntityId(int id);
    public void detachLeash(boolean sendPacket, boolean dropItem);
}
