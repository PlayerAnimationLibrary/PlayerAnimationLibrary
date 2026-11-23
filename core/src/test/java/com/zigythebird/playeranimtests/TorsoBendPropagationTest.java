package com.zigythebird.playeranimtests;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimtests.framework.AnimationsProvider;
import com.zigythebird.playeranimtests.framework.Snapshots;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class TorsoBendPropagationTest {
    /**
     * With {@link ExtraAnimationData#APPLY_BEND_TO_OTHER_BONES_KEY} the torso bend has to move the other bones like
     * a pivot bone in the middle of the torso does, which is what the reference animations have instead - both for
     * bones parented to the torso and for ones next to it. The bones above them are scaled and rotated, including
     * by 90 degrees where the euler angles hit a gimbal lock.
     */
    @DisplayName("Torso bend is applied to other bones in the space of the torso")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"apply_bend_to_torso_children_test", "apply_bend_to_other_bones_parent_test"})
    public void bendFollowsTorso(String name) throws IOException {
        Map<String, Animation> animations;
        try (InputStream is = getClass().getResourceAsStream(AnimationsProvider.RESOURCE_PATH + "/apply_bend_to_other_bones_parent_test.json")) {
            animations = UniversalAnimLoader.loadAnimations(is);
        }

        Animation animation = animations.get(name);
        TestAnimationController reference = TestAnimationController.playing(animations.get(name + "_reference"));
        TestAnimationController.playing(animation).captureAgainst(reference, name, Snapshots.ALL, animation.length(), 0.01F);
    }
}
