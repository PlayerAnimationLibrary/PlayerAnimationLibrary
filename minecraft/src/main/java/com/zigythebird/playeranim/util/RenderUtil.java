package com.zigythebird.playeranim.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/**
 * Helper class for various methods and functions useful while rendering
 */
public final class RenderUtil {
	public static void rotateMatrixAroundBone(PoseStack poseStack, PlayerAnimBone bone) {
		rotateZYX(poseStack.last(), bone.rotation.z, bone.rotation.y, bone.rotation.x);
	}

	/**
	 * Here we do nothing with rotation because it is unnecessary.
	 */
	public static void translatePartToCape(ModelPart part, PlayerAnimBone bone, PartPose initialPose) {
		part.x = bone.position.x + initialPose.x();
		part.y = -(bone.position.y + initialPose.y());
		part.z = bone.position.z + initialPose.z();

		part.xRot = bone.rotation.x;
		part.yRot = bone.rotation.y;
		part.zRot = bone.rotation.z;

		part.xScale = bone.scale.x;
		part.yScale = bone.scale.y;
		part.zScale = bone.scale.z;
	}

    /**
     * Initial pose only applied to yRot and position because that's all that's needed for vanilla parts.
     */
    public static void translatePartToBone(ModelPart part, PlayerAnimBone bone, PartPose initialPose) {
		part.x = bone.position.x + initialPose.x();
		part.y = -bone.position.y + initialPose.y();
		part.z = bone.position.z + initialPose.z();

		part.xRot = bone.rotation.x;
		part.yRot = bone.rotation.y + initialPose.yRot();
		part.zRot = bone.rotation.z;

		part.xScale = bone.scale.x;
		part.yScale = bone.scale.y;
		part.zScale = bone.scale.z;
    }

	public static void translateMatrixToBone(PoseStack poseStack, PlayerAnimBone bone) {
		poseStack.translate(bone.position.x / 16, bone.position.y / 16, bone.position.z / 16);
		RenderUtil.rotateMatrixAroundBone(poseStack, bone);
		poseStack.scale(bone.scale.x, bone.scale.y, bone.scale.z);
	}

	public static PlayerAnimBone copyVanillaPart(ModelPart part, PlayerAnimBone bone) {
		PartPose initialPose = part.getInitialPose();

		bone.position.set(part.x - initialPose.x(), -(part.y - initialPose.y()), part.z - initialPose.z());
		bone.rotation.set(part.xRot, part.yRot, part.zRot);
		bone.scale.set(part.xScale, part.yScale, part.zScale);

        return bone;
    }

	public static void rotateZYX(PoseStack.Pose matrices, float angleZ, float angleY, float angleX) {
		matrices.pose().rotateZYX(angleZ, angleY, angleX);
		matrices.normal().rotateZYX(angleZ, angleY, angleX);
	}
}
