package com.zigythebird.playeranimtests.framework;

import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.enums.TransformType;
import org.joml.Vector3f;
import org.junit.jupiter.api.Assertions;

import java.util.EnumSet;

/**
 * Per-axis float-tolerant bone equality used by
 * {@link TestAnimationController#captureAgainst}.
 */
public final class Snapshots {
    public static final float DEFAULT_EPSILON = 0.1f;

    public static void assertBonesEqual(PlayerAnimBone expected, PlayerAnimBone actual, String label, float tick, EnumSet<TransformType> toAssert) {
        assertBonesEqual(expected, actual, DEFAULT_EPSILON, label, tick, toAssert);
    }

    public static void assertBonesEqual(PlayerAnimBone expected, PlayerAnimBone actual, float epsilon, String label, float tick, EnumSet<TransformType> toAssert) {
        if (toAssert.contains(TransformType.POSITION)) assertVectorEqual(epsilon, expected.position, actual.position, TransformType.POSITION, label, tick, expected.name);
        if (toAssert.contains(TransformType.ROTATION)) assertVectorEqual(epsilon, expected.rotation, actual.rotation, TransformType.ROTATION, label, tick, expected.name);
        if (toAssert.contains(TransformType.SCALE))    assertVectorEqual(epsilon, expected.scale,    actual.scale,    TransformType.SCALE,    label, tick, expected.name);
        // if (toAssert.contains(TransformType.BEND)) TODO
    }

    public static void assertVectorEqual(float epsilon, Vector3f expected, Vector3f actual, TransformType type, String label, float tick, String bone) {
        String name = type.name().toLowerCase();
        assertAxis(epsilon, expected.x, actual.x, type, label, tick, bone, name + ".x");
        assertAxis(epsilon, expected.y, actual.y, type, label, tick, bone, name + ".y");
        assertAxis(epsilon, expected.z, actual.z, type, label, tick, bone, name + ".z");
    }

    private static void assertAxis(float epsilon, float expected, float actual, TransformType type, String label, float tick, String bone, String axis) {
        float delta = type == TransformType.ROTATION ? angleDelta(expected, actual) : actual - expected;
        if (Math.abs(delta) > epsilon) {
            Assertions.fail(label + " tick " + tick + " bone " + bone + " " + axis
                    + ": expected " + expected + " but was " + actual
                    + " (delta=" + delta + ", eps=" + epsilon + ")");
        }
    }

    /** Shortest signed distance between two angles, so e.g. -π and +π compare equal. */
    private static float angleDelta(float expected, float actual) {
        float d = (actual - expected) % (2f * (float) Math.PI);
        if (d > Math.PI) d -= 2f * (float) Math.PI;
        if (d < -Math.PI) d += 2f * (float) Math.PI;
        return d;
    }

    private Snapshots() {}
}
