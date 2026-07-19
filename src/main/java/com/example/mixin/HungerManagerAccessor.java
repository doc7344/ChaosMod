package com.example.mixin;

import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HungerManager.class)
public interface HungerManagerAccessor {
    @Accessor("foodTickTimer")
    int chaos$getFoodTickTimer();

    @Accessor("foodTickTimer")
    void chaos$setFoodTickTimer(int value);

    @Accessor("prevFoodLevel")
    void chaos$setPrevFoodLevel(int value);
}
