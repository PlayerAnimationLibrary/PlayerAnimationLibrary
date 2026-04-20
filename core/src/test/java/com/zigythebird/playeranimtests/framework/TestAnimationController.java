package com.zigythebird.playeranimtests.framework;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.HumanoidAnimationController;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.molang.MolangLoader;

import java.util.EnumSet;

/**
 * Test-only subclass of {@link HumanoidAnimationController} that drives the
 * controller through an animation and collects per-tick bone transforms,
 * mirroring how {@code PlayerAnimationController} in the Minecraft module
 * extends {@code HumanoidAnimationController} with its own integration hooks.
 * <p>
 * {@code activeBones} is {@code protected} on the parent, so the capture logic
 * lives here — there's no need to expose the field through a getter.
 */
public class TestAnimationController extends HumanoidAnimationController {
    public TestAnimationController() {
        super((_, _, _) -> PlayState.STOP, MolangLoader::createNewEngine);
    }

    /** Non-looping playback of {@code animation}. */
    public static TestAnimationController playing(Animation animation) {
        TestAnimationController controller = new TestAnimationController();
        controller.triggerAnimation(RawAnimation.begin().then(animation, Animation.LoopType.PLAY_ONCE));
        return controller;
    }

    /**
     * Tick this controller and {@code target} in lockstep for {@code length}
     * ticks, asserting per-tick that every bone this controller considers
     * active matches the transform produced by the target.
     */
    public void captureAgainst(IAnimation target, float length, String label, EnumSet<TransformType> toAssert) {
        AnimationData data = new AnimationData(0f, 0f, false);
        // AnimationLoader.calculateAnimationLength returns Float.MAX_VALUE for
        // animations without temporally-distributed keyframes (e.g. static bends
        // on bend_test.json: one keyframe at t=0 → keyframe deltas sum to 0).
        // The controller treats this as "no natural end" at play time; for a
        // binary-roundtrip test a single tick is enough to verify the static
        // pose is preserved.
        float limit = length == Float.MAX_VALUE ? 1f : length;
        for (float tick = 0f; tick < limit; tick += 1f) {
            this.setupAnim(data);
            target.setupAnim(data);

            System.out.println(length);
            System.out.println(tick);

            for (String name : this.activeBones.keySet()) {
                PlayerAnimBone expected = this.get3DTransform(name);
                PlayerAnimBone actual = target.get3DTransform(name);
                Snapshots.assertBonesEqual(expected, actual, label, tick, toAssert);
            }

            this.tick(data);
            target.tick(data);
        }
    }
}
