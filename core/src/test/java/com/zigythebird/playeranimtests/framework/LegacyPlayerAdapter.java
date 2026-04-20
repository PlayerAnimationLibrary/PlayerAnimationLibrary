package com.zigythebird.playeranimtests.framework;

import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.loading.PlayerAnimatorLoader;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import dev.kosmx.playerAnim.api.PartKey;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import org.jetbrains.annotations.NotNull;
import org.redlance.common.utils.ReflectUtils;

import java.lang.invoke.VarHandle;

/**
 * Exposes an older {@link KeyframeAnimationPlayer} (playeranimator pre-fork) as
 * an {@link IAnimation}, so a {@link TestAnimationController} can drive it
 * through {@link TestAnimationController#captureAgainst}.
 * <p>
 * Bone names are snake_case on our side; {@link PartKey} keys are camelCase —
 * the conversion uses {@link UniversalAnimLoader#restorePlayerBoneName}.
 */
public final class LegacyPlayerAdapter implements IAnimation {
    private static final Vec3f ZERO = new Vec3f(0f, 0f, 0f);
    private static final Vec3f ONE = new Vec3f(1f, 1f, 1f);

    private static final VarHandle BEGIN_TICK = ReflectUtils.uncheck(() ->
            ReflectUtils.TRUSTED_LOOKUP.findVarHandle(KeyframeAnimation.class, "beginTick", int.class));
    private static final VarHandle END_TICK = ReflectUtils.uncheck(() ->
            ReflectUtils.TRUSTED_LOOKUP.findVarHandle(KeyframeAnimation.class, "endTick", int.class));
    private static final VarHandle STOP_TICK = ReflectUtils.uncheck(() ->
            ReflectUtils.TRUSTED_LOOKUP.findVarHandle(KeyframeAnimation.class, "stopTick", int.class));

    private final KeyframeAnimationPlayer player;

    /**
     * Strips begin/stop-tick behavior so the player matches {@code TestAnimationController}'s tick-guard-free playback.
     */
    public LegacyPlayerAdapter(KeyframeAnimation animation) {
        BEGIN_TICK.set(animation, 0);
        if (animation.stopTick != animation.endTick) {
            STOP_TICK.set(animation, animation.endTick);
            END_TICK.set(animation, animation.endTick);
        }
        this.player = new KeyframeAnimationPlayer(animation);
    }

    @Override
    public boolean isActive() {
        return this.player.isActive();
    }

    @Override
    public void tick(AnimationData state) {
        this.player.tick();
    }

    @Override
    public void setupAnim(AnimationData state) {
        this.player.setupAnim(state.getPartialTick());
    }

    @Override
    public void get3DTransform(@NotNull PlayerAnimBone bone) {
        PartKey key = PartKey.keyForId(UniversalAnimLoader.restorePlayerBoneName(bone.getName()));

        // KAP returns position as absolute model-space (includes model defaults,
        // e.g. right_arm.x = -5); our controller returns deltas relative to the
        // bone's rest pose. Pass the defaults as the initial value and subtract
        // them back from the result so both sides speak the same delta language.
        com.zigythebird.playeranimcore.math.Vec3f posDef = PlayerAnimatorLoader.getDefaultValues(bone.getName());
        Vec3f posBase = new Vec3f(posDef.x(), posDef.y(), posDef.z());

        Vec3f pos = this.player.get3DTransform(key, TransformType.POSITION, 0f, posBase);
        Vec3f rot = this.player.get3DTransform(key, TransformType.ROTATION, 0f, ZERO);
        Vec3f scale = this.player.get3DTransform(key, TransformType.SCALE, 0f, ONE);

        bone.position.set(pos.getX() - posDef.x(), pos.getY() - posDef.y(), pos.getZ() - posDef.z());
        bone.rotation.set(rot.getX(), rot.getY(), rot.getZ());
        bone.scale.set(scale.getX(), scale.getY(), scale.getZ());
    }
}
