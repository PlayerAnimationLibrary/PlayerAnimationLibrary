package com.zigythebird.playeranimcore.util;

import com.zigythebird.playeranimcore.bones.PivotBone;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * Does NOT divide pos 16.
 * Used for applying custom pivot bones to player bones
 */
public class MatrixUtil {
    public static void translateMatrixForBone(Matrix4f matrix, PlayerAnimBone bone) {
        matrix.translate(-bone.getPosX(), bone.getPosY(), -bone.getPosZ());
    }

    public static void rotateMatrixAroundBone(Matrix4f matrix, PlayerAnimBone bone) {
        if (bone.getRotZ() != 0 || bone.getRotY() != 0 || bone.getRotX() != 0)
            matrix.rotateZ(bone.getRotZ()).rotateY(bone.getRotY()).rotateX(bone.getRotX());
    }

    public static void scaleMatrixForBone(Matrix4f matrix, PlayerAnimBone bone) {
        matrix.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    public static void translateToPivotPoint(Matrix4f matrix, Vec3f pivot) {
        matrix.translate(pivot.x(), pivot.y(), pivot.z());
    }

    public static void translateAwayFromPivotPoint(Matrix4f matrix, Vec3f pivot) {
        matrix.translate(-pivot.x(), -pivot.y(), -pivot.z());
    }

    public static void prepMatrixForBone(Matrix4f matrix, PlayerAnimBone bone, Vec3f pivot) {
        translateToPivotPoint(matrix, pivot);
        translateMatrixForBone(matrix, bone);
        rotateMatrixAroundBone(matrix, bone);
        scaleMatrixForBone(matrix, bone);
        translateAwayFromPivotPoint(matrix, pivot);
    }

    public static void applyParentsToChild(PlayerAnimBone child, Iterable<? extends PlayerAnimBone> parents, Function<String, Vec3f> positions) {
        Matrix4f matrix = new Matrix4f();

        for (PlayerAnimBone parent : parents) {
            Vec3f pivot = parent instanceof PivotBone pivotBone ? pivotBone.getPivot() : positions.apply(parent.getName());
            MatrixUtil.prepMatrixForBone(matrix, parent, pivot);
        }

        applyMatrixToBone(child, matrix, positions.apply(child.getName()));
    }

    public static void applyMatrixToBone(PlayerAnimBone bone, Matrix4f matrix, Vec3f pivot) {
        matrix.translate(pivot.x(), pivot.y(), pivot.z());
        MatrixUtil.rotateMatrixAroundBone(matrix, bone);
        bone.addPos(
                -matrix.m30() + pivot.x(),
                matrix.m31() - pivot.y(),
                -matrix.m32() - pivot.z()
        );

        Vector3f rotation = matrix.getEulerAnglesZYX(new Vector3f());
        bone.updateRotation(rotation.x(), rotation.y(), rotation.z());

        Vector3f scale = matrix.getScale(new Vector3f());
        bone.mulScale(scale.x(), scale.y(), scale.z());
    }
}
