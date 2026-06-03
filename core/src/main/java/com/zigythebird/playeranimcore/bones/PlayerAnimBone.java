package com.zigythebird.playeranimcore.bones;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.Axis;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * This is the object that is directly modified by animations to handle movement
 */
public class PlayerAnimBone {
	private final String name;

	public float scaleX = 1;
	public float scaleY = 1;
	public float scaleZ = 1;

	public float positionX;
	public float positionY;
	public float positionZ;

	public float rotX;
	public float rotY;
	public float rotZ;

	public float bend;

	public PlayerAnimBone(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public float getRotX() {
		return this.rotX;
	}

	public float getRotY() {
		return this.rotY;
	}

	public float getRotZ() {
		return this.rotZ;
	}

	public float getPosX() {
		return this.positionX;
	}

	public float getPosY() {
		return this.positionY;
	}

	public float getPosZ() {
		return this.positionZ;
	}

	public float getScaleX() {
		return this.scaleX;
	}

	public float getScaleY() {
		return this.scaleY;
	}

	public float getScaleZ() {
		return this.scaleZ;
	}

	public float getBend() {
		return this.bend;
	}

	public void setRotX(float value) {
		this.rotX = value;
	}

	public void setRotY(float value) {
		this.rotY = value;
	}

	public void setRotZ(float value) {
		this.rotZ = value;
	}

	public void updateRotation(float xRot, float yRot, float zRot) {
		setRotX(xRot);
		setRotY(yRot);
		setRotZ(zRot);
	}

	public void setPosX(float value) {
		this.positionX = value;
	}

	public void setPosY(float value) {
		this.positionY = value;
	}

	public void setPosZ(float value) {
		this.positionZ = value;
	}

	public void updatePosition(float posX, float posY, float posZ) {
		setPosX(posX);
		setPosY(posY);
		setPosZ(posZ);
	}

	public void setScaleX(float value) {
		this.scaleX = value;
	}

	public void setScaleY(float value) {
		this.scaleY = value;
	}

	public void setScaleZ(float value) {
		this.scaleZ = value;
	}

	public void updateScale(float scaleX, float scaleY, float scaleZ) {
		setScaleX(scaleX);
		setScaleY(scaleY);
		setScaleZ(scaleZ);
	}

	public void setBend(float value) {
		this.bend = value;
	}

	public void setToInitialPose() {
		this.positionX = 0;
		this.positionY = 0;
		this.positionZ = 0;

		this.rotX = 0;
		this.rotY = 0;
		this.rotZ = 0;

		this.scaleX = 1;
		this.scaleY = 1;
		this.scaleZ = 1;

		this.bend = 0;
	}

	public Vec3f getPositionVector() {
		return new Vec3f(getPosX(), getPosY(), getPosZ());
	}

	public Vec3f getRotationVector() {
		return new Vec3f(getRotX(), getRotY(), getRotZ());
	}

	public Vec3f getScaleVector() {
		return new Vec3f(getScaleX(), getScaleY(), getScaleZ());
	}

	public void addRotationOffsetFromBone(PlayerAnimBone source) {
		setRotX(getRotX() + source.getRotX());
		setRotY(getRotY() + source.getRotY());
		setRotZ(getRotZ() + source.getRotZ());
	}

	public PlayerAnimBone scale(float value) {
		this.positionX *= value;
		this.positionY *= value;
		this.positionZ *= value;

		this.rotX *= value;
		this.rotY *= value;
		this.rotZ *= value;

		this.scaleX *= value;
		this.scaleY *= value;
		this.scaleZ *= value;

		this.bend *= value;

		return this;
	}

	public PlayerAnimBone add(PlayerAnimBone bone) {
		this.positionX += bone.positionX;
		this.positionY += bone.positionY;
		this.positionZ += bone.positionZ;

		this.rotX += bone.rotX;
		this.rotY += bone.rotY;
		this.rotZ += bone.rotZ;

		this.scaleX += bone.scaleX;
		this.scaleY += bone.scaleY;
		this.scaleZ += bone.scaleZ;

		this.bend += bone.bend;

		return this;
	}

	public PlayerAnimBone applyOtherBone(PlayerAnimBone bone) {
		this.positionX += bone.positionX;
		this.positionY += bone.positionY;
		this.positionZ += bone.positionZ;

		this.rotX += bone.rotX;
		this.rotY += bone.rotY;
		this.rotZ += bone.rotZ;

		this.scaleX *= bone.scaleX;
		this.scaleY *= bone.scaleY;
		this.scaleZ *= bone.scaleZ;

		this.bend += bone.bend;

		return this;
	}

	public PlayerAnimBone addPos(float value) {
		return addPos(value, value, value);
	}

	public PlayerAnimBone mulPos(float value) {
		return mulPos(value, value, value);
	}

	public PlayerAnimBone divPos(float value) {
		return divPos(value, value, value);
	}

	public PlayerAnimBone addRot(float value) {
		return addRot(value, value, value);
	}

	public PlayerAnimBone mulRot(float value) {
		return mulRot(value, value, value);
	}

	public PlayerAnimBone divRot(float value) {
		return divRot(value, value, value);
	}

	public PlayerAnimBone addScale(float value) {
		return addScale(value, value, value);
	}

	public PlayerAnimBone mulScale(float value) {
		return mulScale(value, value, value);
	}

	public PlayerAnimBone divScale(float value) {
		return divScale(value, value, value);
	}

	public PlayerAnimBone addPos(float x, float y, float z) {
		this.positionX += x;
		this.positionY += y;
		this.positionZ += z;

		return this;
	}

	public PlayerAnimBone addRot(float x, float y, float z) {
		this.rotX += x;
		this.rotY += y;
		this.rotZ += z;

		return this;
	}

	public PlayerAnimBone addScale(float x, float y, float z) {
		this.scaleX += x;
		this.scaleY += y;
		this.scaleZ += z;

		return this;
	}

	public PlayerAnimBone mulPos(float x, float y, float z) {
		this.positionX *= x;
		this.positionY *= y;
		this.positionZ *= z;
		return this;
	}

	public PlayerAnimBone mulRot(float x, float y, float z) {
		this.rotX *= x;
		this.rotY *= y;
		this.rotZ *= z;
		return this;
	}

	public PlayerAnimBone mulScale(float x, float y, float z) {
		this.scaleX *= x;
		this.scaleY *= y;
		this.scaleZ *= z;
		return this;
	}

	public PlayerAnimBone divPos(float x, float y, float z) {
		this.positionX /= x;
		this.positionY /= y;
		this.positionZ /= z;
		return this;
	}

	public PlayerAnimBone divRot(float x, float y, float z) {
		this.rotX /= x;
		this.rotY /= y;
		this.rotZ /= z;
		return this;
	}

	public PlayerAnimBone divScale(float x, float y, float z) {
		this.scaleX /= x;
		this.scaleY /= y;
		this.scaleZ /= z;
		return this;
	}

	public PlayerAnimBone copyOtherBone(PlayerAnimBone bone) {
		this.positionX = bone.positionX;
		this.positionY = bone.positionY;
		this.positionZ = bone.positionZ;

		this.rotX = bone.rotX;
		this.rotY = bone.rotY;
		this.rotZ = bone.rotZ;

		this.scaleX = bone.scaleX;
		this.scaleY = bone.scaleY;
		this.scaleZ = bone.scaleZ;

		this.bend = bone.bend;
		return this;
	}

	public PlayerAnimBone copyOtherBoneIfNotDisabled(PlayerAnimBone bone) {
		if (bone instanceof IBoneEnabled advancedBone) {
			if (advancedBone.isPositionXEnabled())
				this.positionX = bone.positionX;
			if (advancedBone.isPositionYEnabled())
				this.positionY = bone.positionY;
			if (advancedBone.isPositionZEnabled())
				this.positionZ = bone.positionZ;

			if (advancedBone.isRotXEnabled())
				this.rotX = bone.rotX;
			if (advancedBone.isRotYEnabled())
				this.rotY = bone.rotY;
			if (advancedBone.isRotZEnabled())
				this.rotZ = bone.rotZ;

			if (advancedBone.isScaleXEnabled())
				this.scaleX = bone.scaleX;
			if (advancedBone.isScaleYEnabled())
				this.scaleY = bone.scaleY;
			if (advancedBone.isScaleZEnabled())
				this.scaleZ = bone.scaleZ;

			if (advancedBone.isBendEnabled())
				this.bend = bone.bend;

			return this;
		}
		return copyOtherBone(bone);
	}

	@ApiStatus.Internal
	public PlayerAnimBone beginOrEndTickLerp(AdvancedPlayerAnimBone bone, float animTime, Animation animation) {
		if (bone.positionXEnabled)
			this.positionX = beginOrEndTickLerp(positionX, bone.positionX, bone.positionXTransitionLength, animTime, animation, TransformType.POSITION, Axis.X);
		if (bone.positionYEnabled)
			this.positionY = beginOrEndTickLerp(positionY, bone.positionY, bone.positionYTransitionLength, animTime, animation, TransformType.POSITION, Axis.Y);
		if (bone.positionZEnabled)
			this.positionZ = beginOrEndTickLerp(positionZ, bone.positionZ, bone.positionZTransitionLength, animTime, animation, TransformType.POSITION, Axis.Z);

		if (bone.rotXEnabled)
			this.rotX = beginOrEndTickLerp(rotX, bone.rotX, bone.rotXTransitionLength, animTime, animation, TransformType.ROTATION, Axis.X);
		if (bone.rotYEnabled)
			this.rotY = beginOrEndTickLerp(rotY, bone.rotY, bone.rotYTransitionLength, animTime, animation, TransformType.ROTATION, Axis.Y);
		if (bone.rotZEnabled)
			this.rotZ = beginOrEndTickLerp(rotZ, bone.rotZ, bone.rotZTransitionLength, animTime, animation, TransformType.ROTATION, Axis.Z);

		if (bone.scaleXEnabled)
			this.scaleX = beginOrEndTickLerp(scaleX, bone.scaleX, bone.scaleXTransitionLength, animTime, animation, TransformType.SCALE, Axis.X);
		if (bone.scaleYEnabled)
			this.scaleY = beginOrEndTickLerp(scaleY, bone.scaleY, bone.scaleYTransitionLength, animTime, animation, TransformType.SCALE, Axis.Y);
		if (bone.scaleZEnabled)
			this.scaleZ = beginOrEndTickLerp(scaleZ, bone.scaleZ, bone.scaleZTransitionLength, animTime, animation, TransformType.SCALE, Axis.Z);

		if (bone.bendEnabled)
			this.bend = beginOrEndTickLerp(bend, bone.bend, bone.bendTransitionLength, animTime, animation, TransformType.BEND, Axis.Y);

		return this;
	}
	
	private float beginOrEndTickLerp(float startValue, float endValue, Float transitionLength, float animTime, Animation animation, TransformType type, Axis axis) {
		EasingType easingType = EasingType.EASE_IN_OUT_SINE;
		if (animation != null) {
			float temp = startValue;
			startValue = endValue;
			endValue = temp;

			if (transitionLength == null) transitionLength = animation.length() - (float) animation.data().getRaw(ExtraAnimationData.END_TICK_KEY);

			if (animation.data().has(ExtraAnimationData.EASING_BEFORE_KEY) && !(boolean) animation.data().getRaw(ExtraAnimationData.EASING_BEFORE_KEY)) {
				BoneAnimation boneAnimation = animation.getBone(getName());
				KeyframeStack keyframeStack = boneAnimation == null ? null : switch (type) {
					case BEND -> {
						List<Keyframe> bendKeyFrames = boneAnimation.bendKeyFrames();
						if (!bendKeyFrames.isEmpty()) easingType = bendKeyFrames.getLast().easingType();
						yield null;
					}
					case ROTATION -> boneAnimation.rotationKeyFrames();
					case SCALE -> boneAnimation.scaleKeyFrames();
					case POSITION -> boneAnimation.positionKeyFrames();
				};
				if (keyframeStack != null) {
					List<Keyframe> keyFrames = keyframeStack.getKeyFramesForAxis(axis);
					if (!keyFrames.isEmpty()) easingType = keyFrames.getLast().easingType();
				}
			}
			if (easingType == EasingType.BEZIER || easingType == EasingType.CATMULLROM)
				easingType = EasingType.EASE_IN_OUT_SINE;
		}
		if (transitionLength == null) return endValue;
		return easingType.apply(startValue, endValue, animTime / transitionLength);
	}

	public void copySnapshot(BoneSnapshot snapshot) {
		this.positionX = snapshot.getOffsetX();
		this.positionY = snapshot.getOffsetY();
		this.positionZ = snapshot.getOffsetZ();

		this.rotX = snapshot.getRotX();
		this.rotY = snapshot.getRotY();
		this.rotZ = snapshot.getRotZ();

		this.scaleX = snapshot.getScaleX();
		this.scaleY = snapshot.getScaleY();
		this.scaleZ = snapshot.getScaleZ();

		this.bend = snapshot.getBend();
	}
	
	public PlayerAnimBone copySnapshotSafe(AdvancedBoneSnapshot snapshot) {
		if (snapshot.positionXEnabled)
			this.positionX = snapshot.getOffsetX();
		if (snapshot.positionYEnabled)
			this.positionY = snapshot.getOffsetY();
		if (snapshot.positionZEnabled)
			this.positionZ = snapshot.getOffsetZ();

		if (snapshot.rotXEnabled)
			this.rotX = snapshot.getRotX();
		if (snapshot.rotYEnabled)
			this.rotY = snapshot.getRotY();
		if (snapshot.rotZEnabled)
			this.rotZ = snapshot.getRotZ();

		if (snapshot.scaleXEnabled)
			this.scaleX = snapshot.getScaleX();
		if (snapshot.scaleYEnabled)
			this.scaleY = snapshot.getScaleY();
		if (snapshot.scaleZEnabled)
			this.scaleZ = snapshot.getScaleZ();

		if (snapshot.bendEnabled)
			this.bend = snapshot.getBend();

		return this;
	}

	public BoneSnapshot saveSnapshot() {
		return new BoneSnapshot(this);
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || getClass() != obj.getClass())
			return false;

		return hashCode() == obj.hashCode();
	}

	public int hashCode() {
		return getName().hashCode();
	}
}
