package com.zigythebird.playeranimcore.util;

import com.zigythebird.playeranimcore.bones.PivotBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class MatrixUtilTest {
    /**
     * The scale of a parent used to be read as a part of its rotation, down to NaN angles for a scale above 1.
     */
    @Test
    public void scaledParentKeepsItsRotation() {
        PivotBone parent = new PivotBone("parent", new Vec3f(0, 12, 0));
        parent.rotation.set(0.7F, 1.05F, 0.35F);
        parent.scale.set(2F, 0.5F, 1.5F);

        PlayerAnimBone child = new PlayerAnimBone("child");
        MatrixUtil.applyParentsToChild(child, Collections.singletonList(parent), name -> Vec3f.ZERO);

        Assertions.assertTrue(child.rotation.equals(parent.rotation, 1.0E-4F), "rotation " + child.rotation);
        Assertions.assertTrue(child.scale.equals(parent.scale, 1.0E-4F), "scale " + child.scale);
    }
}
