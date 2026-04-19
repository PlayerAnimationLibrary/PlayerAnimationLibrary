/*
 * MIT License
 *
 * Copyright (c) 2022 KosmX
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.zigythebird.playeranim.mixin;

import com.zigythebird.playeranim.accessors.IAnimatedAvatar;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranim.util.ClientUtil;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(Avatar.class)
public abstract class AvatarMixin extends LivingEntity implements IAnimatedAvatar {
    @Unique
    private final Map<Identifier, IAnimation> playerAnimLib$modAnimationData = new HashMap<>();
    @Unique
    private final AvatarAnimManager playerAnimLib$animationManager = playerAnimLib$createAnimationStack();

    protected AvatarMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private AvatarAnimManager playerAnimLib$createAnimationStack() {
        AvatarAnimManager manager = new AvatarAnimManager((Avatar) (Object) this);
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.prepareAnimations((Avatar) (Object) this, manager, playerAnimLib$modAnimationData);
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.invoker().registerAnimation((Avatar) (Object) this, manager);
        return manager;
    }

    @Override
    public AvatarAnimManager playerAnimLib$getAnimManager() {
        return playerAnimLib$animationManager;
    }

    @Override
    public IAnimation playerAnimLib$getAnimation(Identifier id) {
        if (playerAnimLib$modAnimationData.containsKey(id)) return playerAnimLib$modAnimationData.get(id);
        return null;
    }

    @Intrinsic
    @Override
    public void tick() {
        super.tick();
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Inject(method = {"tick", "method_5773"}, at = @At("TAIL"), remap = false)
    private void tick(CallbackInfo ci) {
        if (!this.level().isClientSide()) return;
        this.playerAnimLib$animationManager.handleAnimations(0, true, ClientUtil.shouldBeFirstPersonPass());
    }
}
