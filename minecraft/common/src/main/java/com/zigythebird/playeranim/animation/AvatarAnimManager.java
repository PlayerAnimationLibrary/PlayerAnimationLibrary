package com.zigythebird.playeranim.animation;

import com.zigythebird.playeranim.accessors.IAnimatedAvatar;
import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.AnimationStack;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

/**
 * The animation data collection for a given player instance
 * <p>
 * Generally speaking, a single working-instance of a player will have a single instance of {@code PlayerAnimManager} associated with it
 */
public class AvatarAnimManager extends AnimationStack {
	private final Avatar avatar;

	private float lastUpdateTime;
	private boolean isFirstTick = true;
	private float tickDelta;
	private float firstPersonTransitionProgress = 0;
	private boolean firstPersonTransitioningToPAL;

	public AvatarAnimManager(Avatar avatar) {
		this.avatar = avatar;
	}

    /**
	 * Tick and apply transformations to the model based on the current state of the {@link com.zigythebird.playeranimcore.animation.layered.AnimationContainer}
	 *
	 * @param playerAnimManager The PlayerAnimManager instance being used for this animation processor
	 * @param state	            An {@link AnimationData} instance applied to this render frame
	 */
	public void tickAnimation(AnimationStack playerAnimManager, AnimationData state) {
		playerAnimManager.getLayers().removeIf(pair -> pair.right() == null || pair.right().canRemove());
		for (Pair<Integer, IAnimation> pair : playerAnimManager.getLayers()) {
			IAnimation animation = pair.right();

			if (animation.isActive())
				animation.setupAnim(state.copy());
		}
		finishFirstTick();
	}

	public float getLastUpdateTime() {
		return this.lastUpdateTime;
	}

	public void updatedAt(float updateTime) {
		this.lastUpdateTime = updateTime;
	}

	public boolean isFirstTick() {
		return this.isFirstTick;
	}

	protected void finishFirstTick() {
		this.isFirstTick = false;
	}

	public float getTickDelta() {
		return this.tickDelta;
	}

	public float getFirstPersonTransitionProgress() {
		return firstPersonTransitionProgress;
	}

	public boolean isFirstPersonTransitioningToPAL() {
		return firstPersonTransitioningToPAL;
	}

	/**
	 * If you touch this, you're a horrible person.
	 */
	@ApiStatus.Internal
	public void setTickDelta(float tickDelta) {
		this.tickDelta = tickDelta;
	}

	public void updatePart(ModelPart part, PlayerAnimBone bone) {
		PartPose initialPose = part.getInitialPose();
		bone = this.get3DTransform(bone);
		RenderUtil.translatePartToBone(part, bone, initialPose);
	}

	public void handleAnimations(float partialTick, boolean fullTick, boolean isFirstPersonPass) {
		Vec3 velocity = avatar.getDeltaMovement();

		AvatarAnimManager animatableManager = ((IAnimatedAvatar)avatar).playerAnimLib$getAnimManager();
		int currentTick = avatar.tickCount;

		float currentFrameTime = currentTick + partialTick;

		AnimationData animationData = new AnimationData((float) ((Math.abs(velocity.x) + Math.abs(velocity.z)) / 2f), partialTick, isFirstPersonPass);

		if (fullTick) animatableManager.tick(animationData.copy());

		if (!animatableManager.isFirstTick() && currentFrameTime == animatableManager.getLastUpdateTime())
			return;

		if (!Minecraft.getInstance().isPaused()) {
			animatableManager.updatedAt(currentFrameTime);
		}

		this.tickAnimation(animatableManager, animationData);
	}

	public Avatar getAvatar() {
		return avatar;
	}

	@Override
	public void tick(AnimationData state) {
		super.tick(state);

		int firstPersonTransitionLength = getFirstPersonTransitionLength();
		float target = getFirstPersonMode() == FirstPersonMode.THIRD_PERSON_MODEL ? 1.0f : 0.0f;
		if (firstPersonTransitionLength <= 0) firstPersonTransitionProgress = target;
		else {
			float step = 1.0f / firstPersonTransitionLength;
			if (firstPersonTransitionProgress < target) {
				firstPersonTransitionProgress += step;
				firstPersonTransitioningToPAL = true;
				if (firstPersonTransitionProgress > target) firstPersonTransitionProgress = target;
			} else if (firstPersonTransitionProgress > target) {
				firstPersonTransitionProgress -= step;
				firstPersonTransitioningToPAL = false;
				if (firstPersonTransitionProgress < target) firstPersonTransitionProgress = target;
			}
		}
	}
}
