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
    private static final VarHandle IS_INFINITE = ReflectUtils.uncheck(() ->
            ReflectUtils.TRUSTED_LOOKUP.findVarHandle(KeyframeAnimation.class, "isInfinite", boolean.class));

    private final KeyframeAnimationPlayer player;

    /**
     * Strips begin/stop-tick and loop behavior so the player matches
     * {@code TestAnimationController}'s tick-guard-free, non-looping playback.
     */
    public LegacyPlayerAdapter(KeyframeAnimation animation) {
        BEGIN_TICK.set(animation, 0);
        IS_INFINITE.set(animation, false);
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

        // Mirror {@code LegacyAnimationBinary.readKeyframes}' per-bone adjustments,
        // which KAP's reader does not apply: body position uses block scale (×16),
        // and item/cape/body get per-axis sign flips (plus Y↔Z swap for items).
        String name = bone.getName();
        boolean isBody = name.equals("body");
        boolean isCape = name.equals("cape");
        boolean isItem = name.equals("right_item") || name.equals("left_item");

        float px = pos.getX() - posDef.x();
        float py = pos.getY() - posDef.y();
        float pz = pos.getZ() - posDef.z();
        if (isBody) { px *= 16f; py *= 16f; pz *= 16f; }
        if (isItem || isCape || isBody) px = -px;
        if (!isBody) py = -py;
        if (isCape) pz = -pz;
        if (isItem) { float tmp = py; py = pz; pz = tmp; }

        float rx = rot.getX();
        float ry = rot.getY();
        float rz = rot.getZ();
        if (isItem || isCape || isBody) rx = -rx;
        if (isItem || isBody) ry = -ry;
        if (isItem || isCape) rz = -rz;
        if (isItem) { float tmp = ry; ry = rz; rz = tmp; }

        bone.position.set(px, py, pz);
        bone.rotation.set(rx, ry, rz);
        bone.scale.set(scale.getX(), scale.getY(), scale.getZ());
    }
}
