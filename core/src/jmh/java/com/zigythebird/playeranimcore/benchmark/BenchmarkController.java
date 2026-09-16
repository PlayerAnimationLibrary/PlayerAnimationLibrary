package com.zigythebird.playeranimcore.benchmark;

import com.zigythebird.playeranimcore.animation.HumanoidAnimationController;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.molang.MolangLoader;

import java.util.List;

/**
 * Benchmark-only subclass of {@link HumanoidAnimationController}, mirroring how
 * {@code PlayerAnimationController} in the Minecraft module extends it with its own
 * integration hooks.
 * <p>
 * {@code bones} is {@code protected} on the parent, so the bone-set factory lives here
 * instead of behind a getter added to the library for the benchmarks' sake.
 */
public class BenchmarkController extends HumanoidAnimationController {
    public BenchmarkController() {
        super((_, _, _) -> PlayState.STOP, MolangLoader::createNewEngine);
    }

    /**
     * One bone per registered bone, standing in for the reusable {@link PlayerAnimBone}
     * fields the model mixins hold and hand to {@link #get3DTransform(PlayerAnimBone)}
     * on every render frame.
     */
    public List<PlayerAnimBone> newBoneSet() {
        return this.bones.keySet().stream().map(PlayerAnimBone::new).toList();
    }
}
