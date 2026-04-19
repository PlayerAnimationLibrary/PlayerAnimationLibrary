package com.zigythebird.playeranim.mixin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerCapeModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerCapeModel.class)
public interface CapeModelAccessor {
    @Accessor
    ModelPart getCape();
}
