package com.zigythebird.playeranim.mixin.cmm;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zigythebird.playeranim.animation.cmm.MinecraftCustomBone;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CuboidModelElement.Deserializer.class)
public class CuboidModelElementDeserializerMixin {
    @Shadow
    private static Vector3f getVector3f(JsonObject object, String key) {
        throw new AssertionError();
    }

    @WrapOperation(
            method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/resources/model/cuboid/CuboidModelElement;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/cuboid/CuboidModelElement$Deserializer;getPosition(Lcom/google/gson/JsonObject;Ljava/lang/String;)Lorg/joml/Vector3f;"
            )
    )
    private Vector3f pal$getCustomBonePosition(JsonObject object, String key, Operation<Vector3f> original, @Local(argsOnly = true) JsonDeserializationContext context) {
        // Custom models span the whole player, past the block model bounds
        if (context instanceof MinecraftCustomBone) {
            return getVector3f(object, key);
        }
        return original.call(object, key);
    }
}
