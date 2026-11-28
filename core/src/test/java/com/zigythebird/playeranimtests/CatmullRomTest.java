package com.zigythebird.playeranimtests;

import com.google.gson.JsonParser;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.easing.CatmullRomEasing;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import com.zigythebird.playeranimtests.framework.TestAnimationController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Catmull-Rom keyframes have to curve through every key using the keys on both sides of a stretch,
 * the way Blockbench previews them and GeckoLib plays them: the stretch from key {@code i - 1} to key
 * {@code i} uses key {@code i - 2} and key {@code i + 1} as its outer points.
 */
public class CatmullRomTest {
    private static final float EPSILON = 1.0E-4F;

    /**
     * Evenly spaced keys on a straight line: the spline is that line, so halfway between 10 and 20 is 15.
     * Using the stretch's own start as the previous point gave 14.375.
     */
    @Test
    public void straightKeysStayStraight() {
        float[] keys = {0, 10, 20, 30};

        Assertions.assertEquals(10F, rotationAt(keys, 20), EPSILON);
        Assertions.assertEquals(15F, rotationAt(keys, 30), EPSILON);
        Assertions.assertEquals(20F, rotationAt(keys, 40), EPSILON);
    }

    /**
     * A symmetric zigzag passes its middle stretch at the midpoint, 5. The old previous point gave 4.375.
     */
    @Test
    public void zigzagCrossesHalfway() {
        float[] keys = {0, 10, 0, 10};

        Assertions.assertEquals(5F, rotationAt(keys, 30), EPSILON);
    }

    /**
     * Every tick of the middle stretch against the four-point spline built from the neighbouring keys.
     */
    @Test
    public void middleStretchUsesBothNeighbours() {
        float[] keys = {0, 30, -15, 45};

        for (int tick = 20; tick <= 40; tick++) {
            float t = (tick - 20) / 20F;
            float expected = CatmullRomEasing.getPointOnSpline(t, keys[0], keys[1], keys[2], keys[3]);
            Assertions.assertEquals(expected, rotationAt(keys, tick), EPSILON, "tick " + tick);
        }
    }

    /** One right arm X rotation key per second, all catmullrom. */
    private static Animation animation(float... keys) {
        StringBuilder rotation = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) rotation.append(',');
            rotation.append('"').append(i).append(".0\": {\"post\": [").append(keys[i]).append(", 0, 0], \"lerp_mode\": \"catmullrom\"}");
        }
        String json = "{\"format_version\": \"1.8.0\", \"animations\": {\"catmullrom\": {\"animation_length\": " + (keys.length - 1)
                + ", \"bones\": {\"right_arm\": {\"rotation\": {" + rotation + "}}}}}}";

        return UniversalAnimLoader.loadAnimations(JsonParser.parseString(json).getAsJsonObject()).get("catmullrom");
    }

    /** The right arm's X rotation in degrees after {@code ticks} ticks of playing {@code keys}. */
    private static float rotationAt(float[] keys, int ticks) {
        AnimationData data = new AnimationData(0f, 0f, false);
        TestAnimationController fresh = TestAnimationController.playing(animation(keys));
        for (int tick = 0; tick < ticks; tick++) {
            fresh.setupAnim(data);
            fresh.tick(data);
        }
        fresh.setupAnim(data);
        return (float) Math.toDegrees(fresh.get3DTransform("right_arm").rotation.x);
    }
}
