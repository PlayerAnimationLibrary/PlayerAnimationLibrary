/*
 * MIT License
 *
 * Copyright (c) 2024 GeckoLib
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.zigythebird.playeranimcore.animation;

import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.keyframe.*;
import com.zigythebird.playeranimcore.animation.keyframe.event.CustomKeyFrameEvents;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.KeyFrameData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.ParticleKeyframeData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.SoundKeyframeData;
import com.zigythebird.playeranimcore.animation.layered.AnimationContainer;
import com.zigythebird.playeranimcore.animation.layered.AnimationSnapshot;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.*;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.AnimationStage;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.enums.State;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.event.EventResult;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import com.zigythebird.playeranimcore.util.MatrixUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.parser.ast.FloatExpression;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The actual controller that handles the playing and usage of animations, including their various keyframes and instruction markers
 * <p>
 * Each controller can only play a single animation at a time - for example, you may have one controller to animate walking,
 * one to control attacks, one to control size, etc.
 */
public abstract class AnimationController implements IAnimation {
	public static KeyframeLocation<Keyframe> EMPTY_KEYFRAME_LOCATION = new KeyframeLocation<>(new Keyframe(0), 0);
	public static KeyframeLocation<Keyframe> EMPTY_SCALE_KEYFRAME_LOCATION = new KeyframeLocation<>(new Keyframe(0, Collections.singletonList(FloatExpression.ONE), Collections.singletonList(FloatExpression.ONE)), 0);

	protected final AnimationStateHandler stateHandler;
	protected final Map<String, Vec3f> bonePositions;
	protected final Map<String, AdvancedPlayerAnimBone> bones = new Object2ObjectOpenHashMap<>();
	protected final Map<String, PlayerAnimBone> activeBones = new Object2ObjectOpenHashMap<>();
	protected final Map<String, PivotBone> pivotBones = new Object2ObjectOpenHashMap<>();
	protected Queue<QueuedAnimation> animationQueue = new LinkedList<>();
	protected final MochaEngine<AnimationController> molangRuntime;

	protected boolean needsAnimationReload = false;

	protected CustomKeyFrameEvents.CustomKeyFrameHandler<SoundKeyframeData> soundKeyframeHandler = null;
	protected CustomKeyFrameEvents.CustomKeyFrameHandler<ParticleKeyframeData> particleKeyframeHandler = null;
	protected CustomKeyFrameEvents.CustomKeyFrameHandler<CustomInstructionKeyframeData> customKeyframeHandler = null;

	protected RawAnimation triggeredAnimation = null;
	protected boolean handlingTriggeredAnimations = false;

	protected RawAnimation currentRawAnimation;
	protected QueuedAnimation currentAnimation;
	protected int tick;
	protected float startAnimFrom;
	protected State animationState = State.STOPPED;
	protected boolean isLoopStarted = false;
	protected Consumer<Function<String, AdvancedPlayerAnimBone>> postAnimationSetupConsumer = function -> {};
	protected Function<AnimationController, EasingType> overrideEasingTypeFunction = controller -> null;
	private final Set<KeyFrameData> executedKeyFrames = new ObjectOpenHashSet<>();
	protected AnimationData animationData;

	protected Function<AnimationController, FirstPersonMode> firstPersonMode = null;
	protected Function<AnimationController, FirstPersonConfiguration> firstPersonConfiguration = null;
	private final List<AbstractModifier> modifiers = new ArrayList<>();

	private final InternalAnimationAccessor internalAnimationAccessor = new InternalAnimationAccessor(this);

	/**
	 * Instantiates a new {@code AnimationController}
	 *
	 * @param animationHandler The {@link AnimationStateHandler} animation state handler responsible for deciding which animations to play
	 * @param bonePositions    Map of bones and their pivots
	 * @param molangRuntime    A function that provides the MoLang runtime engine for this animation controller when applied
	 */
	public AnimationController(AnimationStateHandler animationHandler, Map<String, Vec3f> bonePositions, Function<AnimationController, MochaEngine<AnimationController>> molangRuntime) {
		this.stateHandler = animationHandler;
		this.bonePositions = bonePositions;
		this.molangRuntime = molangRuntime.apply(this);

		registerBones();
	}

	public abstract void registerBones();

	/**
	 * Applies the given {@link CustomKeyFrameEvents.CustomKeyFrameHandler} to this controller, for handling {@link SoundKeyframeData sound keyframe instructions}
	 */
	public AnimationController setSoundKeyframeHandler(CustomKeyFrameEvents.CustomKeyFrameHandler<SoundKeyframeData> soundHandler) {
		this.soundKeyframeHandler = soundHandler;

		return this;
	}

	/**
	 * Applies the given {@link CustomKeyFrameEvents.CustomKeyFrameHandler} to this controller, for handling {@link ParticleKeyframeData particle keyframe instructions}
	 */
	public AnimationController setParticleKeyframeHandler(CustomKeyFrameEvents.CustomKeyFrameHandler<ParticleKeyframeData> particleHandler) {
		this.particleKeyframeHandler = particleHandler;

		return this;
	}

	/**
	 * Applies the given {@link CustomKeyFrameEvents.CustomKeyFrameHandler} to this controller, for handling {@link CustomInstructionKeyframeData sound keyframe instructions}
	 */
	public AnimationController setCustomInstructionKeyframeHandler(CustomKeyFrameEvents.CustomKeyFrameHandler<CustomInstructionKeyframeData> customInstructionHandler) {
		this.customKeyframeHandler = customInstructionHandler;

		return this;
	}

	/**
	 * Gives you each bone after it has been set up for a new animation.
	 * Useful for disabling certain bone axes, or all of them if you want.
	 */
	public AnimationController setPostAnimationSetupConsumer(Consumer<Function<String, AdvancedPlayerAnimBone>> postAnimationSetupConsumer) {
		this.postAnimationSetupConsumer = postAnimationSetupConsumer;

		return this;
	}

	/**
	 * Sets the controller's {@link EasingType} override for animations
	 * This lets you change all the easings in an animation to your desired easing.
	 * By default, the controller will use whatever {@code EasingType} was defined in the animation json
	 */
	public AnimationController setOverrideEasingType(EasingType easingTypeFunction) {
		return setOverrideEasingTypeFunction(animatable -> easingTypeFunction);
	}

	/**
	 * Sets the controller's {@link EasingType} override function for animations
	 * This lets you change all the easings in an animation to your desired easing.
	 * By default, the controller will use whatever {@code EasingType} was defined in the animation json
	 */
	public AnimationController setOverrideEasingTypeFunction(Function<AnimationController, EasingType> easingType) {
		this.overrideEasingTypeFunction = easingType;

		return this;
	}

	/**
	 * Tells the AnimationController that you want to receive the {@link AnimationController.AnimationStateHandler} while a triggered animation is playing
	 * <p>
	 * This has no effect if no triggered animation has been registered, or one isn't currently playing
	 * <p>
	 * If a triggered animation is playing, it can be checked in your AnimationStateHandler via {@link #isPlayingTriggeredAnimation()}
	 */
	public AnimationController receiveTriggeredAnimations() {
		this.handlingTriggeredAnimations = true;

		return this;
	}

	/**
	 * Gets the currently loaded {@link Animation}, if present
	 * <p>
	 * An animation returned here does not guarantee it is currently playing, just that it is the currently loaded animation for this controller
	 */
	@Nullable
	public QueuedAnimation getCurrentAnimation() {
		return this.currentAnimation;
	}

    @Nullable
    public Animation getCurrentAnimationInstance() {
        QueuedAnimation queuedAnimation = getCurrentAnimation();
        if (queuedAnimation != null) {
            Animation animation = queuedAnimation.animation();
            if (animation != null) {
                return animation;
            }
        }

        RawAnimation rawAnimation = getTriggeredAnimation();
        if (rawAnimation != null) {
            List<RawAnimation.Stage> stages = rawAnimation.getAnimationStages();
            if (!stages.isEmpty()) {
                return stages.getFirst().animation();
            }
        }

        return null;
    }

	/**
	 * Gets the currently playing {@link RawAnimation triggered animation}, if present
	 */
	@Nullable
	public RawAnimation getTriggeredAnimation() {
		return this.triggeredAnimation;
	}

	/**
	 * Returns the current state of this controller.
	 */
	public @NotNull State getAnimationState() {
		return this.animationState;
	}

	/**
	 * Has the animation looped at least once?
	 */
	public boolean isLoopStarted() {
		return this.isLoopStarted;
	}

	@Override
	public boolean isActive() {
		return this.animationState.isActive();
	}

	public AnimationData getAnimationData() {
		return animationData;
	}

	/**
	 * Marks the controller as needing to reset its animation and state the next time {@link #handleAnimation} is called
	 * <p>
	 * Use this if you have a {@link RawAnimation} with multiple stages and you want it to start again from the first stage, or if you want to reset the currently playing animation to the start
	 */
	public void forceAnimationReset() {
		this.needsAnimationReload = true;
	}

	/**
	 * Tells the controller to stop all animations until told otherwise
	 * <p>
	 * Calling this will prevent the controller from continuing to play the currently loaded animation until
	 * either {@link #forceAnimationReset()} is called, or a different animation is triggered
	 */
	public void stop() {
		this.animationState = State.STOPPED;
		resetEventKeyFrames();
	}

	/**
	 * Pauses the current animation
	 */
	public void pause() {
		this.animationState = State.PAUSED;
	}

	/**
	 * Unpauses the current animation if it's paused
	 */
	public void unpause() {
		if (this.animationState == State.PAUSED)
			this.animationState = State.RUNNING;
	}

	/**
	 * Checks whether the last animation that was playing on this controller has finished or not
	 * <p>
	 * This will return true if the controller has had an animation set previously, and it has finished playing
	 * and isn't going to loop or proceed to another animation
	 *
	 * @return Whether the previous animation finished or not
	 */
	public boolean hasAnimationFinished() {
		return this.currentRawAnimation != null && this.animationState == State.STOPPED;
	}

	/**
	 * Returns the currently cached {@link RawAnimation}
	 * <p>
	 * This animation may or may not still be playing, but it is the last one to be set in {@link #handleAnimation}
	 */
	public RawAnimation getCurrentRawAnimation() {
		return this.currentRawAnimation;
	}

	/**
	 * Used for custom handling if {@link #receiveTriggeredAnimations()} was marked
	 */
	public boolean isPlayingTriggeredAnimation() {
		return this.triggeredAnimation != null && !hasAnimationFinished();
	}

	/**
	 * Use {@link #triggerAnimation(RawAnimation)} ()} instead if outside state handler.
	 * Sets the currently loaded animation to the one provided
	 * <p>
	 * This method may be safely called every render frame, as passing the same builder that is already loaded will do nothing
	 * <p>
	 * Pass null to this method to tell the controller to stop
	 * <p>
	 * If {@link #forceAnimationReset()} has been called prior to this, the controller will reload the animation regardless of whether it matches the currently loaded one or not
	 *
	 * @param startAnimFrom Where to start the animation from in ticks
	 */
	protected void setAnimation(RawAnimation rawAnimation, float startAnimFrom) {
		if (rawAnimation == null || rawAnimation.getAnimationStages().isEmpty()) {
			stop();

			return;
		}

		if (this.needsAnimationReload || !rawAnimation.equals(this.currentRawAnimation)) {
			Queue<QueuedAnimation> animations = getQueuedAnimations(rawAnimation);

			if (animations != null) {
				this.animationQueue = animations;
				this.currentRawAnimation = rawAnimation;
				this.startAnimFrom = startAnimFrom;
				this.tick = 0;
				this.animationState = State.RUNNING;
				this.currentAnimation = this.animationQueue.poll();
				setupNewAnimation();
				this.needsAnimationReload = false;

				return;
			}

			stop();
		}
	}

	protected void setAnimation(RawAnimation rawAnimation) {
		setAnimation(rawAnimation, 0);
	}

	protected Queue<QueuedAnimation> getQueuedAnimations(RawAnimation rawAnimation) {
		LinkedList<QueuedAnimation> animations = new LinkedList<>();
		for (RawAnimation.Stage stage : rawAnimation.getAnimationStages()) {
			Animation animation;
			if (stage.stage() == AnimationStage.WAIT) { // This is intentional. Do not change this or T̶s̶l̶a̶t̶ I will be unhappy!!!
				animation = Animation.generateWaitAnimation(stage.additionalTicks());
			} else {
				animation = stage.animation();
			}

			if (animation != null) animations.add(new QueuedAnimation(animation, stage.loopType()));
		}
		return animations;
	}

	/**
	 * Main method used to set the currently playing animation.
	 * @param newAnimation The animation you want to play.
	 * @param startAnimFrom Where to start the animation from in ticks.
	 */
	public void triggerAnimation(RawAnimation newAnimation, float startAnimFrom) {
		if (newAnimation == null)
			return;

		stop();
		this.triggeredAnimation = newAnimation;

		this.needsAnimationReload = true;
		this.animationState = State.RUNNING;
		this.tick = 0;
		this.startAnimFrom = startAnimFrom;
	}

	public void triggerAnimation(RawAnimation newAnimation) {
		triggerAnimation(newAnimation, 0);
	}

	public void triggerAnimation(Animation newAnimation, float startAnimFrom) {
		triggerAnimation(RawAnimation.begin().then(newAnimation, Animation.LoopType.DEFAULT), startAnimFrom);
	}

	public void triggerAnimation(Animation newAnimation) {
		triggerAnimation(RawAnimation.begin().then(newAnimation, Animation.LoopType.DEFAULT), 0);
	}

	/**
	 * Fade out from current to a new animation
	 * @param fadeModifier    Fade modifier, use {@link AbstractFadeModifier#standardFadeIn(int, EasingType)} for simple fade.
	 * @param newAnimation    The animation you want to play.
	 * @param fadeFromNothing Do fade even if we go from nothing. (for KeyframeAnimation, it can be false by default)
	 */
	public void replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable RawAnimation newAnimation, boolean fadeFromNothing) {
		if (fadeFromNothing || this.isActive()) {
			if (this.isActive()) {
				Map<String, AdvancedBoneSnapshot> snapshots = new HashMap<>();
				for (PlayerAnimBone bone : activeBones.values()) {
					snapshots.put(bone.getName(), new AdvancedBoneSnapshot(bone));
				}
				fadeModifier.setTransitionAnimation(new AnimationSnapshot(snapshots));
			}
			addModifierLast(fadeModifier);
		}
		this.triggerAnimation(newAnimation);
	}

	public void replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable RawAnimation newAnimation) {
		replaceAnimationWithFade(fadeModifier, newAnimation, true);
	}

	public void replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable Animation newAnimation, boolean fadeFromNothing) {
		replaceAnimationWithFade(fadeModifier, RawAnimation.begin().then(newAnimation, Animation.LoopType.DEFAULT), fadeFromNothing);
	}

	public void replaceAnimationWithFade(@NotNull AbstractFadeModifier fadeModifier, @Nullable Animation newAnimation) {
		replaceAnimationWithFade(fadeModifier, newAnimation, true);
	}

	/**
	 * Stops and removes a previously triggered animation, effectively ending it immediately.
	 *
	 * @return true if a triggered animation was stopped
	 */
	public boolean stopTriggeredAnimation() {
		if (this.triggeredAnimation == null)
			return false;

		if (this.currentRawAnimation == this.triggeredAnimation) {
			this.currentAnimation = null;
			this.currentRawAnimation = null;
		}

		this.triggeredAnimation = null;
		this.needsAnimationReload = true;

		return true;
	}

	protected PlayState handleAnimation(AnimationData state) {
		if (this.triggeredAnimation != null) {
			if (this.currentRawAnimation != this.triggeredAnimation)
				this.currentAnimation = null;

			setAnimation(this.triggeredAnimation, startAnimFrom);

			if (!hasAnimationFinished() && !this.handlingTriggeredAnimations)
				return PlayState.CONTINUE;

			this.triggeredAnimation = null;
			this.needsAnimationReload = true;
		}

		return this.stateHandler.handle(this, state, (animation, startTick) -> {
			this.setAnimation(animation, startTick - state.getPartialTick());
			return PlayState.CONTINUE;
		});
	}

	/**
	 * This method is called every frame in order to populate the animation point
	 * queues, and process animation state logic
	 *
	 * @param state                 The animation test state
	 */
	public void process(AnimationData state) {
		float adjustedTick = state.getPartialTick() + this.startAnimFrom + tick;

		PlayState playState = handleAnimation(state);

		if (playState == PlayState.STOP || (this.currentAnimation == null && this.animationQueue.isEmpty())) {
			this.animationState = State.STOPPED;

			return;
		}

		if (getAnimationState() == State.RUNNING) {
			processCurrentAnimation(adjustedTick, state);
		}
	}

	/**
	 * Gets the current animation speed modifier
	 * <p>
	 * This modifier defines the relative speed in which animations will be played based on the current state of the game
	 *
	 * @return The computed current animation speed modifier
	 */
	public float getAnimationSpeed() {
		float speed = 1;
		for (AbstractModifier modifier : modifiers) {
			if (modifier instanceof SpeedModifier speedModifier)
				speed *= speedModifier.speed;
		}
		return speed;
	}

	/**
	 * Handle the current animation's state modifications and translations
	 *
	 * @param adjustedTick The controller-adjusted tick for animation purposes
	 */
	private void processCurrentAnimation(float adjustedTick, AnimationData animationData) {
		Animation animation = this.currentAnimation.animation();

		if (adjustedTick >= animation.length()) {
			if (this.currentAnimation.loopType().shouldPlayAgain(this, animation)) {
				if (this.animationState != State.PAUSED) {
					this.tick = 0;
					this.startAnimFrom = this.currentAnimation.loopType().restartFromTick(this, animation);
					adjustedTick = this.startAnimFrom;
					this.startAnimFrom -= animationData.getPartialTick();
					resetEventKeyFrames();
					this.isLoopStarted = true;
				}
			} else {
				QueuedAnimation nextAnimation = this.animationQueue.peek();

				resetEventKeyFrames();

				if (nextAnimation == null) {
					this.animationState = State.STOPPED;
					this.currentAnimation = null;
					for (AdvancedPlayerAnimBone bone : this.bones.values()) {
						bone.setToInitialPose();
					}
					for (PlayerAnimBone bone : this.pivotBones.values()) {
						bone.setToInitialPose();
					}

					return;
				} else {
					this.animationState = State.RUNNING;
					this.tick = 0;
					this.startAnimFrom = -animationData.getPartialTick();
					adjustedTick = 0;
					this.currentAnimation = this.animationQueue.poll();
					setupNewAnimation();
				}
			}
		}

		if (this.currentAnimation == null) return;
		for (PlayerAnimBone bone : this.bones.values()) {
			bone.setToInitialPose();
		}
		for (PlayerAnimBone bone : this.pivotBones.values()) {
			bone.setToInitialPose();
		}

		for (Map.Entry<String, BoneAnimation> entry : animation.boneAnimations().entrySet()) {
			PlayerAnimBone bone = this.bones.getOrDefault(entry.getKey(), null);
			boolean isAdvancedBone = false;
			AdvancedPlayerAnimBone advancedBone = null;
			if (bone == null) bone = this.pivotBones.getOrDefault(entry.getKey(), null);
			else {
				advancedBone = (AdvancedPlayerAnimBone) bone;
				isAdvancedBone = true;
			}
			if (bone == null) continue;

			BoneAnimation boneAnimation = entry.getValue();
			KeyframeStack rotationKeyFrames = boneAnimation.rotationKeyFrames();
			KeyframeStack positionKeyFrames = boneAnimation.positionKeyFrames();
			KeyframeStack scaleKeyFrames = boneAnimation.scaleKeyFrames();
			List<Keyframe> bendKeyFrames = boneAnimation.bendKeyFrames();

			AnimationPoint rotXPoint = getAnimationPointAtTick(rotationKeyFrames.xKeyframes(), adjustedTick, TransformType.ROTATION, isAdvancedBone ? advancedBone::setRotXTransitionLength : null);
			AnimationPoint rotYPoint = getAnimationPointAtTick(rotationKeyFrames.yKeyframes(), adjustedTick, TransformType.ROTATION, isAdvancedBone ? advancedBone::setRotYTransitionLength : null);
			AnimationPoint rotZPoint = getAnimationPointAtTick(rotationKeyFrames.zKeyframes(), adjustedTick, TransformType.ROTATION, isAdvancedBone ? advancedBone::setRotZTransitionLength : null);
			AnimationPoint posXPoint = getAnimationPointAtTick(positionKeyFrames.xKeyframes(), adjustedTick, TransformType.POSITION, isAdvancedBone ? advancedBone::setPositionXTransitionLength : null);
			AnimationPoint posYPoint = getAnimationPointAtTick(positionKeyFrames.yKeyframes(), adjustedTick, TransformType.POSITION, isAdvancedBone ? advancedBone::setPositionYTransitionLength : null);
			AnimationPoint posZPoint = getAnimationPointAtTick(positionKeyFrames.zKeyframes(), adjustedTick, TransformType.POSITION, isAdvancedBone ? advancedBone::setPositionZTransitionLength : null);
			AnimationPoint scaleXPoint = getAnimationPointAtTick(scaleKeyFrames.xKeyframes(), adjustedTick, TransformType.SCALE, isAdvancedBone ? advancedBone::setScaleXTransitionLength : null);
			AnimationPoint scaleYPoint = getAnimationPointAtTick(scaleKeyFrames.yKeyframes(), adjustedTick, TransformType.SCALE, isAdvancedBone ? advancedBone::setScaleYTransitionLength : null);
			AnimationPoint scaleZPoint = getAnimationPointAtTick(scaleKeyFrames.zKeyframes(), adjustedTick, TransformType.SCALE, isAdvancedBone ? advancedBone::setScaleZTransitionLength : null);
			AnimationPoint bendPoint = getAnimationPointAtTick(bendKeyFrames, adjustedTick, TransformType.BEND, isAdvancedBone ? advancedBone::setBendTransitionLength : null);
			EasingType easingType = this.overrideEasingTypeFunction.apply(this);

            bone.setRotX(EasingType.lerpWithOverride(this.molangRuntime, rotXPoint, easingType));
            bone.setRotY(EasingType.lerpWithOverride(this.molangRuntime, rotYPoint, easingType));
            bone.setRotZ(EasingType.lerpWithOverride(this.molangRuntime, rotZPoint, easingType));

            bone.setPosX(EasingType.lerpWithOverride(this.molangRuntime, posXPoint, easingType));
            bone.setPosY(EasingType.lerpWithOverride(this.molangRuntime, posYPoint, easingType));
            bone.setPosZ(EasingType.lerpWithOverride(this.molangRuntime, posZPoint, easingType));

            bone.setScaleX(EasingType.lerpWithOverride(this.molangRuntime, scaleXPoint, easingType));
            bone.setScaleY(EasingType.lerpWithOverride(this.molangRuntime, scaleYPoint, easingType));
            bone.setScaleZ(EasingType.lerpWithOverride(this.molangRuntime, scaleZPoint, easingType));

            bone.setBend(EasingType.lerpWithOverride(this.molangRuntime, bendPoint, easingType));
        }

		applyCustomPivotPoints();

		handleCustomKeyframe(
				animation.keyFrames().sounds(),
				this.soundKeyframeHandler, CustomKeyFrameEvents.SOUND_KEYFRAME_EVENT.invoker(),
				adjustedTick, animationData
		);

		handleCustomKeyframe(
				animation.keyFrames().particles(),
				this.particleKeyframeHandler, CustomKeyFrameEvents.PARTICLE_KEYFRAME_EVENT.invoker(),
				adjustedTick, animationData
		);

		handleCustomKeyframe(
				animation.keyFrames().customInstructions(),
				this.customKeyframeHandler, CustomKeyFrameEvents.CUSTOM_INSTRUCTION_KEYFRAME_EVENT.invoker(),
				adjustedTick, animationData
		);
	}

	protected void applyCustomPivotPoints() {
		if (this.currentAnimation == null) return;
		Map<String, String> parentsMap = this.currentAnimation.animation().parents();
		if (parentsMap.isEmpty()) return;

		Set<String> processedBones = new HashSet<>();
		for (PlayerAnimBone bone : this.bones.values()) {
			processBoneHierarchy(bone, parentsMap, processedBones);
		}
		for (PlayerAnimBone bone : this.pivotBones.values()) {
			processBoneHierarchy(bone, parentsMap, processedBones);
		}
	}

	private void processBoneHierarchy(PlayerAnimBone bone, Map<String, String> parentsMap, Set<String> processedBones) {
		String boneName = bone.getName();
		if (processedBones.contains(boneName)) return;

		String parentName = parentsMap.get(boneName);
		if (parentName == null) {
			processedBones.add(boneName);
			return;
		}

		PlayerAnimBone parent = this.pivotBones.get(parentName);
		if (parent == null) parent = this.bones.get(parentName);
		if (parent == null) {
			PlayerAnimLib.LOGGER.error("Parent {} not found for {}", parentName, boneName);
			return;
		}

		processBoneHierarchy(parent, parentsMap, processedBones);

		this.activeBones.put(boneName, bone);
		MatrixUtil.applyParentsToChild(bone, Collections.singletonList(parent), this::getBonePosition);

		processedBones.add(boneName);
	}

	protected  <T extends KeyFrameData> void handleCustomKeyframe(T[] keyframes, @Nullable CustomKeyFrameEvents.CustomKeyFrameHandler<T> main, CustomKeyFrameEvents.CustomKeyFrameHandler<T> event, float animationTick, AnimationData animationData) {
		for (T keyframeData : keyframes) {
			if (animationTick >= keyframeData.getStartTick() && this.executedKeyFrames.add(keyframeData)) {

				EventResult result = main == null ? EventResult.PASS : main.handle(animationTick, this, keyframeData, animationData);
				if (result == EventResult.PASS) {
					result = event.handle(animationTick, this, keyframeData, animationData);
				}

				if (result == EventResult.FAIL) {
					return;
				}
			}
		}
	}

	/**
	 * Duration of the animation spent in seconds
	 */
	public float getAnimationTime() {
		return getAnimationTicks() / 20;
	}

	/**
	 * Duration of the animation spent in ticks
	 * tick + tick delta
	 */
	public float getAnimationTicks() {
		if (this.animationData == null) return 0F;
		return this.tick + this.startAnimFrom + this.animationData.getPartialTick();
	}

	public boolean hasBeginTick() {
		return this.currentAnimation.animation().data().has(ExtraAnimationData.BEGIN_TICK_KEY);
	}

	public boolean hasEndTick() {
		Animation animation = this.currentAnimation.animation();
		return !animation.loopType().shouldPlayAgain(null, animation) && animation.data().has(ExtraAnimationData.END_TICK_KEY);
	}

	public boolean isDisableAxisIfNotModified() {
		return this.currentAnimation != null && this.currentAnimation.animation().data().isDisableAxisIfNotModified();
	}

	public boolean isAnimationPlayerAnimatorFormat() {
		return this.currentAnimation != null && this.currentAnimation.animation().data().isAnimationPlayerAnimatorFormat();
	}

	protected void setupNewAnimation() {
		this.isLoopStarted = false;
		if (currentAnimation == null) return;
		this.activeBones.clear();
		resetEventKeyFrames();
		for (AdvancedPlayerAnimBone bone : bones.values()) {
			bone.setEnabled(currentAnimation.animation().getBone(bone.getName()) != null);
		}
		for (Map.Entry<String, BoneAnimation> entry : currentAnimation.animation().boneAnimations().entrySet()) {
			if (bones.containsKey(entry.getKey())) {
				AdvancedPlayerAnimBone bone = bones.get(entry.getKey());
				this.activeBones.put(entry.getKey(), bone);
				if (isDisableAxisIfNotModified()) {
					BoneAnimation boneAnimation = entry.getValue();
					bone.positionXEnabled = !boneAnimation.positionKeyFrames().xKeyframes().isEmpty();
					bone.positionYEnabled = !boneAnimation.positionKeyFrames().yKeyframes().isEmpty();
					bone.positionZEnabled = !boneAnimation.positionKeyFrames().zKeyframes().isEmpty();

					bone.rotXEnabled = !boneAnimation.rotationKeyFrames().xKeyframes().isEmpty();
					bone.rotYEnabled = !boneAnimation.rotationKeyFrames().yKeyframes().isEmpty();
					bone.rotZEnabled = !boneAnimation.rotationKeyFrames().zKeyframes().isEmpty();

					bone.scaleXEnabled = !boneAnimation.scaleKeyFrames().xKeyframes().isEmpty();
					bone.scaleYEnabled = !boneAnimation.scaleKeyFrames().yKeyframes().isEmpty();
					bone.scaleZEnabled = !boneAnimation.scaleKeyFrames().zKeyframes().isEmpty();

					bone.bendEnabled = !boneAnimation.bendKeyFrames().isEmpty();
				} else bone.setEnabled(true);
			}
			else if (pivotBones.containsKey(entry.getKey()))
				this.activeBones.put(entry.getKey(), this.pivotBones.get(entry.getKey()));
		}

		for (String entry : currentAnimation.animation().parents().keySet()) {
			if (this.bones.containsKey(entry)) this.bones.get(entry).setEnabled(true);
		}

		this.pivotBones.clear();
		for (Map.Entry<String, Vec3f> entry : currentAnimation.animation().bones().entrySet()) {
			this.pivotBones.put(entry.getKey(), new PivotBone(entry.getKey(), entry.getValue()));
		}

		this.postAnimationSetupConsumer.accept(this.bones::get);
	}

	/**
	 * Convert a {@link KeyframeLocation} to an {@link AnimationPoint}
	 */
	private AnimationPoint getAnimationPointAtTick(List<Keyframe> frames, float tick, TransformType type, Consumer<Float> transitionLengthSetter) {
		Animation animation = this.currentAnimation.animation();
		float endTick = animation.data().<Float>get(ExtraAnimationData.END_TICK_KEY).orElse(animation.length()-1);

		KeyframeLocation<Keyframe> location = getCurrentKeyFrameLocation(frames, tick, type, this.isAnimationPlayerAnimatorFormat() && this.currentAnimation.loopType().shouldPlayAgain(null, animation), animation.length(), this.currentAnimation.loopType().restartFromTick(null, animation));
		Keyframe currentFrame = location.keyframe();
		float startValue = this.molangRuntime.eval(currentFrame.startValue());
		float endValue = this.molangRuntime.eval(currentFrame.endValue());

		if (type == TransformType.ROTATION || type == TransformType.BEND) {
			if (!(MolangLoader.isConstant(currentFrame.startValue()))) {
				startValue = (float) Math.toRadians(startValue);
			}

			if (!(MolangLoader.isConstant(currentFrame.endValue()))) {
				endValue = (float) Math.toRadians(endValue);
			}
		}

		if (transitionLengthSetter != null) {
			ExtraAnimationData extraData = animation.data();
			if (hasBeginTick() && !frames.isEmpty() && currentFrame == frames.getFirst() && extraData.<Float>get(ExtraAnimationData.BEGIN_TICK_KEY).get() > tick) {
				startValue = endValue;
				transitionLengthSetter.accept(currentFrame.length());
			} else if (hasEndTick() && !frames.isEmpty() && currentFrame == frames.getLast() && endTick <= tick) {
				transitionLengthSetter.accept(animation.length() - endTick);
			} else transitionLengthSetter.accept(null);
		}

		return new AnimationPoint(currentFrame.easingType(), currentFrame.easingArgs(), location.startTick(), currentFrame.length(), startValue, endValue);
	}

	/**
	 * Returns the {@link Keyframe} relevant to the current tick time
	 *
	 * @param frames The list of {@code KeyFrames} to filter through
	 * @param ageInTicks The current tick time
	 * @return A new {@code KeyFrameLocation} containing the current {@code KeyFrame} and the tick time used to find it
	 */
	private KeyframeLocation<Keyframe> getCurrentKeyFrameLocation(List<Keyframe> frames, float ageInTicks, TransformType type, boolean isPlayerAnimatorLoop, float animTime, float returnToTick) {
		if (frames.isEmpty())
			return type == TransformType.SCALE ? EMPTY_SCALE_KEYFRAME_LOCATION : EMPTY_KEYFRAME_LOCATION;

		Keyframe firstFrame = returnToTick == 0 ? frames.getFirst() : Keyframe.getKeyframeAtTime(frames, returnToTick);
		float totalFrameTime = 0;

		for (Keyframe frame : frames) {
			totalFrameTime += frame.length();

			if (totalFrameTime > ageInTicks) {
				if (isPlayerAnimatorLoop && isLoopStarted() && frame == firstFrame) {
					float stopTickMinusLastKeyframe = animTime - Keyframe.getLastKeyframeTime(frames);
					return new KeyframeLocation<>(new Keyframe(frame.length() + stopTickMinusLastKeyframe, frames.getLast().endValue(), frame.endValue(), frame.easingType(), frame.easingArgs()), ageInTicks + stopTickMinusLastKeyframe);
				}
				return new KeyframeLocation<>(frame, (ageInTicks - (totalFrameTime - frame.length())));
			}
		}

		if (isPlayerAnimatorLoop)
			return new KeyframeLocation<>(new Keyframe(firstFrame.length() + animTime - totalFrameTime, frames.getLast().endValue(), firstFrame.endValue(), firstFrame.easingType(), firstFrame.easingArgs()), ageInTicks - totalFrameTime);

		return new KeyframeLocation<>(frames.getLast(), ageInTicks);
	}

	/**
	 * Clear the {@link KeyFrameData} cache in preparation for the next animation
	 */
	protected void resetEventKeyFrames() {
		if (!this.executedKeyFrames.isEmpty()) {
			CustomKeyFrameEvents.RESET_KEYFRAMES_EVENT.invoker().handle(this, this.executedKeyFrames);
		}
		this.executedKeyFrames.clear();
	}

	public PlayerAnimBone get3DTransformRaw(@NotNull PlayerAnimBone bone) {
		if (activeBones.containsKey(bone.getName())) {
			PlayerAnimBone bone1 = activeBones.get(bone.getName());
			if (this.currentAnimation != null && bone1 instanceof AdvancedPlayerAnimBone advancedBone) {
				ExtraAnimationData extraData = this.currentAnimation.animation().data();
				if (hasBeginTick() && extraData.<Float>get(ExtraAnimationData.BEGIN_TICK_KEY).get() > this.getAnimationTicks()) {
					bone.beginOrEndTickLerp(advancedBone, this.getAnimationTicks(), null);
				}
				else if (hasEndTick() && extraData.<Float>get(ExtraAnimationData.END_TICK_KEY).get() <= this.getAnimationTicks()) {
					bone.beginOrEndTickLerp(advancedBone, this.getAnimationTicks() - extraData.<Float>get(ExtraAnimationData.END_TICK_KEY).get(), this.currentAnimation.animation());
				}
				else bone.copyOtherBoneIfNotDisabled(bone1);
			}
			else bone.copyOtherBoneIfNotDisabled(bone1);
		}
		return bone;
	}

	@Override
	public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
		if (!modifiers.isEmpty()) {
			return modifiers.getFirst().get3DTransform(bone);
		}
		return get3DTransformRaw(bone);
	}

	@Override
	public @NotNull FirstPersonMode getFirstPersonMode() {
		if (firstPersonMode != null) return firstPersonMode.apply(this);
		return FirstPersonMode.NONE;
	}

	@Override
	public @NotNull FirstPersonConfiguration getFirstPersonConfiguration() {
		if (firstPersonConfiguration != null) return firstPersonConfiguration.apply(this);
		return IAnimation.DEFAULT_FIRST_PERSON_CONFIG;
	}

	public AnimationController setFirstPersonMode(FirstPersonMode mode) {
		firstPersonMode = (controller) -> mode;

		return this;
	}

	public AnimationController setFirstPersonModeHandler(Function<AnimationController, FirstPersonMode> modeHandler) {
		firstPersonMode = modeHandler;

		return this;
	}

	public AnimationController setFirstPersonConfiguration(FirstPersonConfiguration config) {
		firstPersonConfiguration = (controller) -> config;

		return this;
	}

	public AnimationController setFirstPersonConfigurationHandler(Function<AnimationController, FirstPersonConfiguration> configHandler) {
		firstPersonConfiguration = configHandler;

		return this;
	}

	@Override
	public void tick(AnimationData state) {
		for (int i = 0; i < modifiers.size(); i++) {
			if (modifiers.get(i).canRemove()) {
				removeModifier(i--);
			}
		}
		if (!modifiers.isEmpty())
			modifiers.getFirst().tick(state);
		else {
			handleAnimation(state);
			if (this.animationState == State.RUNNING) {
				tick += 1;
			}
		}
	}

	@Override
	public void setupAnim(AnimationData state) {
		this.animationData = state;
		if (!modifiers.isEmpty())
			modifiers.getFirst().setupAnim(state);
		else process(state);
	}

	public Vec3f getBonePosition(String name) {
		if (bonePositions.containsKey(name)) return bonePositions.get(name);
		if (pivotBones.containsKey(name)) return pivotBones.get(name).getPivot();
		return Vec3f.ZERO;
	}

	/**
	 * PLEASE DON'T USE THIS UNLESS YOU KNOW WHAT YOU'RE DOING.
	 * THE {@link AnimationController#linkModifiers()} METHOD MUST BE CALLED EVERYTIME ANYTHING IN THE MODIFIER LIST IS CHANGED.
	 */
	@ApiStatus.Internal
	public List<AbstractModifier> getModifiers() {
		return modifiers;
	}

	public AnimationController addModifier(@NotNull AbstractModifier modifier, int idx) {
		modifier.setHost(this);
		modifiers.add(idx, modifier);
		linkModifiers();
		return this;
	}

	public AnimationController addModifierBefore(@NotNull AbstractModifier modifier) {
		this.addModifier(modifier, 0);
		return this;
	}

	public AnimationController addModifierLast(@NotNull AbstractModifier modifier) {
		this.addModifier(modifier, modifiers.size());
		return this;
	}

	public AnimationController removeModifier(int idx) {
		modifiers.remove(idx);
		linkModifiers();
		return this;
	}

	public AnimationController removeAllModifiers() {
		modifiers.clear();
		return this;
	}

	public int getModifierCount() {
		return modifiers.size();
	}

	public @Nullable AbstractModifier getModifier(int idx) {
		try {
			return modifiers.get(idx);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	public boolean removeModifierIf(Predicate<? super AbstractModifier> predicate) {
		boolean success = modifiers.removeIf(predicate);
		linkModifiers();
		return success;
	}

	protected void linkModifiers() {
		Iterator<AbstractModifier> modifierIterator = modifiers.iterator();
		if (modifierIterator.hasNext()) {
			AbstractModifier tmp = modifierIterator.next();
			while (modifierIterator.hasNext()) {
				AbstractModifier tmp2 = modifierIterator.next();
				tmp.setAnim(tmp2);
				tmp = tmp2;
			}
			tmp.setAnim(internalAnimationAccessor);
		}
	}

	public AdvancedPlayerAnimBone registerPlayerAnimBone(String name) {
		return registerPlayerAnimBone(new AdvancedPlayerAnimBone(name));
	}

	/**
	 * Adds the given bone to the bones list for this controller
	 * <p>
	 * This is normally handled automatically by the mod
	 */
	public AdvancedPlayerAnimBone registerPlayerAnimBone(AdvancedPlayerAnimBone bone) {
		this.bones.put(bone.getName(), bone);
		return bone;
	}

	/**
	 * Allows you to get a bone from the controller.
	 * This is used so you can disable/enable bone axes mid-animation,
	 * and you probably shouldn't touch anything other than that.
	 *
	 * @param name Name of the bone you want to get.
	 * @return The requested bone.
	 */
	public @Nullable AdvancedPlayerAnimBone getBone(String name) {
		return this.bones.get(name);
	}

	/**
	 * Every render frame, the {@code AnimationController} will call this handler for <u>each</u> animatable that is being rendered
	 * <p>
	 * This handler defines which animation should be currently playing, and returning a {@link PlayState} to tell the controller what to do next
	 * <p>
	 * Example Usage:
	 * <pre>{@code
	 * AnimationFrameHandler myIdleWalkHandler = state -> {
	 *	if (state.isMoving()) {
	 *		return animationSetter.apply(myWalkAnimation);
	 *	}
	 *  return animationSetter.apply(myIdleAnimation);
	 *};}</pre>
	 */
	@FunctionalInterface
	public interface AnimationStateHandler {
		/**
		 * The handling method, called each frame
		 * <p>
		 * Return {@link PlayState#CONTINUE} to tell the controller to continue animating,
		 * or return {@link PlayState#STOP} to tell it to stop playing all animations and wait for the next {@link PlayState#CONTINUE} return.
		 */
		PlayState handle(AnimationController controller, AnimationData state, AnimationSetter animationSetter);
	}

	@FunctionalInterface
	public interface AnimationSetter {
		default PlayState setAnimation(RawAnimation animation) {
			return setAnimation(animation, 0);
		}
		PlayState setAnimation(RawAnimation animation, int startFromTick);
	}

	@SuppressWarnings("ConstantConditions")
	private static class InternalAnimationAccessor extends AnimationContainer<AnimationController> {
		private InternalAnimationAccessor(AnimationController controller) {
			super(controller);
		}

		@Override
		public void tick(AnimationData state) {
			this.anim.handleAnimation(state);
			if (this.anim.animationState == State.RUNNING) this.anim.tick += 1;
		}

		@Override
		public void setupAnim(AnimationData state) {
			this.anim.process(state);
		}

		@Override
		public PlayerAnimBone get3DTransform(@NotNull PlayerAnimBone bone) {
			return this.anim.get3DTransformRaw(bone);
		}
	}

    @Override
    public String toString() {
        return "AnimationController{" +
                "currentAnimation=" + getCurrentAnimationInstance() +
                ", tick=" + getAnimationTicks() +
                ", modifiers=" + modifiers +
                '}';
    }
}
