package com.zigythebird.playeranimcore.bones;

import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.Axis;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.math.MathHelper;
import com.zigythebird.playeranimcore.math.Vec3f;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3f;

import java.util.List;

/**
 * This is the object that is directly modified by animations to handle movement
 */
public class PlayerAnimBone {
	public final String name;
	public final Vector3f position;
	public final Vector3f rotation;
	public final Vector3f scale;

	public PlayerAnimBone(String name) {
		this.name = name;
		this.position = new Vector3f();
		this.rotation = new Vector3f();
		this.scale = new Vector3f(1);
	}

	public PlayerAnimBone(PlayerAnimBone bone) {
		this.name = bone.getName();
		this.position = new Vector3f(bone.position);
		this.rotation = new Vector3f(bone.rotation);
		this.scale = new Vector3f(bone.scale);
	}

	public String getName() {
		return this.name;
	}

	public void setToInitialPose() {
		this.position.set(0, 0, 0);
		this.rotation.set(0, 0, 0);
		this.scale.set(1, 1, 1);
	}

	public PlayerAnimBone scale(float value) {
		this.position.mul(value);
		this.rotation.mul(value);
		this.scale.mul(value);

		return this;
	}

	public PlayerAnimBone add(PlayerAnimBone bone) {
		this.position.add(bone.position);
		this.rotation.add(bone.rotation);
		this.scale.add(bone.scale);

		return this;
	}

	public PlayerAnimBone applyOtherBone(PlayerAnimBone bone) {
		this.position.add(bone.position);
		this.rotation.add(bone.rotation);
		this.scale.mul(bone.scale);

		return this;
	}

	public PlayerAnimBone copyOtherBone(PlayerAnimBone bone) {
		this.position.set(bone.position);
		this.rotation.set(bone.rotation);
		this.scale.set(bone.scale);

		return this;
	}

	public PlayerAnimBone copyOtherBoneIfNotDisabled(PlayerAnimBone bone) {
		if (bone instanceof ToggleablePlayerAnimBone toggleableBone) {
			if (toggleableBone.isPositionXEnabled())
				this.position.x = bone.position.x;
			if (toggleableBone.isPositionYEnabled())
				this.position.y = bone.position.y;
			if (toggleableBone.isPositionZEnabled())
				this.position.z = bone.position.z;

			if (toggleableBone.isRotXEnabled())
				this.rotation.x = bone.rotation.x;
			if (toggleableBone.isRotYEnabled())
				this.rotation.y = bone.rotation.y;
			if (toggleableBone.isRotZEnabled())
				this.rotation.z = bone.rotation.z;

			if (toggleableBone.isScaleXEnabled())
				this.scale.x = bone.scale.x;
			if (toggleableBone.isScaleYEnabled())
				this.scale.y = bone.scale.y;
			if (toggleableBone.isScaleZEnabled())
				this.scale.z = bone.scale.z;

			return this;
		}
		return copyOtherBone(bone);
	}

	@ApiStatus.Internal
	public void beginOrEndTickLerp(AdvancedPlayerAnimBone bone, float animTime, Animation animation) {
		if (bone.positionXEnabled)
			this.position.x = beginOrEndTickLerp(position.x, bone.position.x, bone.positionXTransitionLength, animTime, animation, TransformType.POSITION, Axis.X);
		if (bone.positionYEnabled)
			this.position.y = beginOrEndTickLerp(position.y, bone.position.y, bone.positionYTransitionLength, animTime, animation, TransformType.POSITION, Axis.Y);
		if (bone.positionZEnabled)
			this.position.z = beginOrEndTickLerp(position.z, bone.position.z, bone.positionZTransitionLength, animTime, animation, TransformType.POSITION, Axis.Z);

		if (bone.rotXEnabled)
			this.rotation.z = beginOrEndTickLerp(rotation.x, bone.rotation.x, bone.rotXTransitionLength, animTime, animation, TransformType.ROTATION, Axis.X);
		if (bone.rotYEnabled)
			this.rotation.y = beginOrEndTickLerp(rotation.y, bone.rotation.y, bone.rotYTransitionLength, animTime, animation, TransformType.ROTATION, Axis.Y);
		if (bone.rotZEnabled)
			this.rotation.z = beginOrEndTickLerp(rotation.z, bone.rotation.z, bone.rotZTransitionLength, animTime, animation, TransformType.ROTATION, Axis.Z);

		if (bone.scaleXEnabled)
			this.scale.x = beginOrEndTickLerp(scale.x, bone.scale.x, bone.scaleXTransitionLength, animTime, animation, TransformType.SCALE, Axis.X);
		if (bone.scaleYEnabled)
			this.scale.y = beginOrEndTickLerp(scale.y, bone.scale.y, bone.scaleYTransitionLength, animTime, animation, TransformType.SCALE, Axis.Y);
		if (bone.scaleZEnabled)
			this.scale.z = beginOrEndTickLerp(scale.z, bone.scale.z, bone.scaleZTransitionLength, animTime, animation, TransformType.SCALE, Axis.Z);
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || getClass() != obj.getClass())
			return false;

		return hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
