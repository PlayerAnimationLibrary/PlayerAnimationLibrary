package com.zigythebird.playeranim.mixin.cmm;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zigythebird.playeranim.animation.cmm.MinecraftCustomBone;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(UnbakedCuboidGeometry.class)
public class UnbakedCuboidGeometryMixin {
    @WrapOperation(
            method = "bake(Ljava/util/List;Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/sprite/MaterialBaker;resolveSlot(Lnet/minecraft/client/resources/model/sprite/TextureSlots;Ljava/lang/String;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/sprite/Material$Baked;"
            )
    )
    private static Material.Baked pal$resolveCustomBoneSlot(MaterialBaker instance, TextureSlots slots, String id, ModelDebugName name, Operation<Material.Baked> original) {
        if (name instanceof MinecraftCustomBone customBone) {
            return new Material.Baked(customBone.getSprite(), true);
        }
        return original.call(instance, slots, id, name);
    }
}
