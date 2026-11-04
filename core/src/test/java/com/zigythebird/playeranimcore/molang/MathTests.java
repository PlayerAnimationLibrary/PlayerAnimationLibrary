package com.zigythebird.playeranimcore.molang;

import com.zigythebird.playeranimcore.easing.EasingType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.mocha.runtime.MolangInterpreter;
import org.redlance.mocha.runtime.standard.MochaMath;

public class MathTests {
    private static final MolangInterpreter<?> ENGINE = MolangLoader.createNewEngine();

    @Test
    public void testMocha() {
        Assertions.assertEquals(Math.abs(-10F), ENGINE.eval("math.abs(-10);"));
        Assertions.assertEquals(Math.acos(1), ENGINE.eval("math.acos(1);"));
        Assertions.assertEquals(MochaMath.PI, ENGINE.eval("math.PI;"));
        Assertions.assertEquals(MochaMath.d2r(10), ENGINE.eval("math.d2r(10);"));
    }

    @Test
    public void unboundedMolangIsAborted() {
        // keyframe expressions a client could send over the network; the step limit set in
        // createBaseEngine aborts them to 0 instead of letting them hang the thread
        Assertions.assertEquals(0F, ENGINE.eval("loop(2147483647, {loop(2147483647, {1})})"));
        Assertions.assertEquals(0F, ENGINE.eval("math.die_roll(2147483647, 0, 1)"));
    }

    @Test
    public void testEasing() {
        for (float i = 0; i <= 1.0F; i += 0.1F) {
            float real = EasingType.EASE_OUT_BOUNCE.apply(1F, 10F, i);
            float mocha = ENGINE.eval(String.format("math.ease_out_bounce(1, 10, %s);", i));
            System.out.println(real + " vs " + mocha);
            Assertions.assertEquals(real, mocha);
        }
    }
}
