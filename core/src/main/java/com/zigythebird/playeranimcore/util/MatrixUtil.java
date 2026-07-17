package com.zigythebird.playeranimcore.util;

import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.math.ModMatrix4f;
import com.zigythebird.playeranimcore.math.Vec3f;

/**
 * Does NOT divide pos 16.
 * Used for applying custom pivot bones to player bones
 */
public class MatrixUtil {
    public static void translateMatrixForBone(ModMatrix4f matrix, PlayerAnimBone bone) {
        matrix.translate(-bone.positionX, bone.positionY, -bone.positionZ);
    }

    public static void rotateMatrixAroundBone(ModMatrix4f matrix, PlayerAnimBone bone) {
        if (bone.getRotZ() != 0 || bone.getRotY() != 0 || bone.getRotX() != 0)
            matrix.rotateZ(bone.getRotZ()).rotateY(bone.getRotY()).rotateX(bone.getRotX());
    }

    public static void scaleMatrixForBone(ModMatrix4f matrix, PlayerAnimBone bone) {
        matrix.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    public static void translateToPivotPoint(ModMatrix4f matrix, Vec3f pivot) {
        matrix.translate(pivot.x(), pivot.y(), pivot.z());
    }

    public static void translateAwayFromPivotPoint(ModMatrix4f matrix, Vec3f pivot) {
        matrix.translate(-pivot.x(), -pivot.y(), -pivot.z());
    }

    public static void prepMatrixForBone(ModMatrix4f matrix, PlayerAnimBone bone, Vec3f pivot) {
        translateToPivotPoint(matrix, pivot);
        translateMatrixForBone(matrix, bone);
        rotateMatrixAroundBone(matrix, bone);
        scaleMatrixForBone(matrix, bone);
        translateAwayFromPivotPoint(matrix, pivot);
    }
}
