package com.zigythebird.playeranim.animation;

import com.zigythebird.playeranim.util.RenderUtil;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.AnimationStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.ApiStatus;

/**
 * The animation data collection for a given player instance
 * <p>
 * Generally speaking, a single working-instance of a player will have a single instance of {@code PlayerAnimManager} associated with it
 */
public class PlayerAnimManager extends AnimationStack {
	private final AbstractClientPlayer player;

	private float lastUpdateTime;
	private boolean isFirstTick = true;
	private float tickDelta;
	private float firstPersonTransitionProgress = 0;
	private boolean firstPersonTransitioningToPAL;

	public PlayerAnimManager(AbstractClientPlayer player) {
		this.player = player;
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

	/**
	 * If you touch this, you're a horrible person.
	 */
	@ApiStatus.Internal
	public void setTickDelta(float tickDelta) {
		this.tickDelta = tickDelta;
	}

	public AbstractClientPlayer getPlayer() {
		return player;
	}

	public float getFirstPersonTransitionProgress() {
		return firstPersonTransitionProgress;
	}

	public boolean isFirstPersonTransitioningToPAL() {
		return firstPersonTransitioningToPAL;
	}

	public void updatePart(ModelPart part, ModelPart secondLayer, PlayerAnimBone bone) {
		PartPose initialPose = part.getInitialPose();
		bone = this.get3DTransform(bone);
		RenderUtil.translatePartToBone(part, bone, initialPose);
		RenderUtil.translatePartToBone(secondLayer, bone, initialPose);
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
