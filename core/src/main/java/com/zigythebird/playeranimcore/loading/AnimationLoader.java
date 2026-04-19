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

package com.zigythebird.playeranimcore.loading;

import com.google.gson.*;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.Axis;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import com.zigythebird.playeranimcore.util.JsonUtil;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import team.unnamed.mocha.parser.ast.AccessExpression;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.parser.ast.FloatExpression;
import team.unnamed.mocha.parser.ast.IdentifierExpression;
import team.unnamed.mocha.runtime.IsConstantExpression;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zigythebird.playeranimcore.molang.MolangLoader.MOCHA_ENGINE;

public class AnimationLoader implements JsonDeserializer<Animation> {
	@Override
	public Animation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject animationObj = json.getAsJsonObject();

		float length = animationObj.has("animation_length") ? JsonUtil.getAsFloat(animationObj, "animation_length") * 20f : -1;
		Map<String, BoneAnimation> boneAnimations = bakeBoneAnimations(JsonUtil.getAsJsonObject(animationObj, "bones", new JsonObject()));
		if (length == -1) length = calculateAnimationLength(boneAnimations);

		Animation.LoopType loopType = readLoopType(animationObj, length);
		Animation.Keyframes keyframes = context.deserialize(animationObj, Animation.Keyframes.class);

		Map<String, String> parents = UniversalAnimLoader.getParents(JsonUtil.getAsJsonObject(animationObj, "parents", new JsonObject()));
		Map<String, Vec3f> bones = UniversalAnimLoader.getModel(JsonUtil.getAsJsonObject(animationObj, "model", new JsonObject()));

		// Extra data
		ExtraAnimationData extraData = new ExtraAnimationData();
		if (animationObj.has(PlayerAnimLib.MOD_ID)) {
			extraData.fromJson(animationObj.getAsJsonObject(PlayerAnimLib.MOD_ID), false);
		}

		return new Animation(extraData, length, loopType, boneAnimations, keyframes, bones, parents);
	}

	private static Animation.LoopType readLoopType(JsonObject animationObj, float length) throws JsonParseException {
		if (animationObj.has("loopTick")) {
			float returnTick = JsonUtil.getAsFloat(animationObj, "loopTick") * 20f;
			if (returnTick > length || returnTick < 0) {
				throw new JsonParseException("The returnTick has to be a non-negative value smaller than the endTick value");
			}
			return Animation.LoopType.returnToTickLoop(returnTick);
		}
		return Animation.LoopType.fromJson(animationObj.get("loop"));
	}

	private static Map<String, BoneAnimation> bakeBoneAnimations(JsonObject bonesObj) {
		Map<String, BoneAnimation> animations = new HashMap<>(bonesObj.size());

		for (Map.Entry<String, JsonElement> entry : bonesObj.entrySet()) {
			JsonObject entryObj = entry.getValue().getAsJsonObject();
			KeyframeStack scaleFrames = buildKeyframeStack(getKeyframes(entryObj.get("scale")), TransformType.SCALE);
			KeyframeStack positionFrames = buildKeyframeStack(getKeyframes(entryObj.get("position")), TransformType.POSITION);
			KeyframeStack rotationFrames = buildKeyframeStack(getKeyframes(entryObj.get("rotation")), TransformType.ROTATION);
			KeyframeStack bendFrames = buildKeyframeStack(getKeyframes(entryObj.get("bend")), TransformType.BEND);
			animations.put(
					UniversalAnimLoader.getCorrectPlayerBoneName(entry.getKey()),
					new BoneAnimation(rotationFrames, positionFrames, scaleFrames, bendFrames.xKeyframes())
			);
		}

		return animations;
	}

	private static List<FloatObjectPair<JsonElement>> getKeyframes(JsonElement element) {
		if (element == null)
			return List.of();

		if (element instanceof JsonPrimitive primitive) {
			JsonArray array = new JsonArray(3);

			array.add(primitive);
			array.add(primitive);
			array.add(primitive);

			element = array;
		}

		if (element instanceof JsonArray array)
			return ObjectArrayList.of(FloatObjectPair.of(0, array));

		if (element instanceof JsonObject obj) {
			if (obj.has("vector"))
				return ObjectArrayList.of(FloatObjectPair.of(0, obj));

			if (obj.has("value")) {
				JsonArray array = new JsonArray(3);
				array.add(obj.get("value").getAsFloat());
				array.add(0);
				array.add(0);
				obj.add("vector", array);
				return ObjectArrayList.of(FloatObjectPair.of(0, obj));
			}

			List<FloatObjectPair<JsonElement>> list = new ObjectArrayList<>();

			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				float timestamp = readTimestamp(entry.getKey());

				if (timestamp == 0 && !list.isEmpty())
					throw new JsonParseException("Invalid keyframe data - multiple starting keyframes?" + entry.getKey());

				if (entry.getValue() instanceof JsonObject entryObj) {
					if (entryObj.has("value")) {
						JsonArray array = new JsonArray(3);
						array.add(entryObj.get("value").getAsFloat());
						array.add(0);
						array.add(0);
						entryObj.add("vector", array);
						list.add(FloatObjectPair.of(timestamp, entryObj));
					}
					else if (!entryObj.has("vector")) {
						addBedrockKeyframes(timestamp, entryObj, list);
						continue;
					}
				}

				list.add(FloatObjectPair.of(timestamp, entry.getValue()));
			}

			return list;
		}

		throw new JsonParseException("Invalid object type provided to getTripletObj, got: " + element);
	}

	// Blockbench is just getting silly now
	private static JsonArray extractBedrockKeyframe(JsonElement keyframe) {
		if (keyframe.isJsonArray())
			return keyframe.getAsJsonArray();

		// For bends
		if (keyframe.isJsonPrimitive()) {
			JsonArray array = new JsonArray(3);
			array.add(keyframe.getAsFloat());
			array.add(0);
			array.add(0);
			return array;
		}

		if (!keyframe.isJsonObject())
			throw new JsonParseException("Invalid keyframe data - expected array or object, found " + keyframe);

		JsonObject keyframeObj = keyframe.getAsJsonObject();

		if (keyframeObj.has("vector"))
			return keyframeObj.get("vector").getAsJsonArray();

		if (keyframeObj.has("pre"))
			return keyframeObj.get("pre").getAsJsonArray();

		return keyframeObj.get("post").getAsJsonArray();
	}

	private static void addBedrockKeyframes(float timestamp, JsonObject keyframe, List<FloatObjectPair<JsonElement>> keyframes) {
		boolean addedFrame = false;

		if (keyframe.has("pre")) {
			addedFrame = true;

			JsonArray value = extractBedrockKeyframe(keyframe.get("pre"));
			JsonObject result = null;
			if (keyframe.has("easing")) {
				result = new JsonObject();
				result.add("vector", value);
				result.add("easing", keyframe.get("easing"));
				if (keyframe.has("easingArgs")) result.add("easingArgs", keyframe.get("easingArgs"));
			}

			keyframes.add(FloatObjectPair.of(timestamp == 0 ? timestamp : timestamp - 0.001f, result != null ? result : value));
		}

		if (keyframe.has("post")) {
			JsonArray values = extractBedrockKeyframe(keyframe.get("post"));

			if (keyframe.has("lerp_mode")) {
				JsonObject keyframeObj = new JsonObject();

				keyframeObj.add("vector", values);
				keyframeObj.add("easing", keyframe.get("lerp_mode"));

				keyframes.add(FloatObjectPair.of(timestamp, keyframeObj));
			}
			else {
				keyframes.add(FloatObjectPair.of(timestamp, values));
			}

			return;
		}

		if (!addedFrame)
			throw new JsonParseException("Invalid keyframe data - expected array, found " + keyframe);
	}

	private static KeyframeStack buildKeyframeStack(List<FloatObjectPair<JsonElement>> entries, TransformType type) {
		if (entries.isEmpty()) return new KeyframeStack();

		List<Keyframe> xFrames = new ObjectArrayList<>();
		List<Keyframe> yFrames = new ObjectArrayList<>();
		List<Keyframe> zFrames = new ObjectArrayList<>();

		List<Expression> xPrev = null;
		List<Expression> yPrev = null;
		List<Expression> zPrev = null;

		float prevTimeX = 0;
		float prevTimeY = 0;
		float prevTimeZ = 0;

		for (FloatObjectPair<JsonElement> entry : entries) {
			JsonElement element = entry.right();

			float curTime = entry.leftFloat();

			boolean isForRotation = type == TransformType.ROTATION || type == TransformType.BEND;
			Expression defaultValue = type == TransformType.SCALE ? FloatExpression.ONE : FloatExpression.ZERO;

			JsonArray keyFrameVector = element instanceof JsonArray array ? array : JsonUtil.getAsJsonArray(element.getAsJsonObject(), "vector");
			List<Expression> xValue = MolangLoader.parseJson(isForRotation, keyFrameVector.get(0), defaultValue);
			List<Expression> yValue = MolangLoader.parseJson(isForRotation, keyFrameVector.get(1), defaultValue);
			List<Expression> zValue = MolangLoader.parseJson(isForRotation, keyFrameVector.get(2), defaultValue);

			JsonObject entryObj = element instanceof JsonObject obj ? obj : null;
			EasingType easingType = getEasingForAxis(entryObj, null, EasingType.LINEAR);
			List<List<Expression>> easingArgs = getEasingArgsForAxis(entryObj, null, new ObjectArrayList<>());

			if (isEnabled(xValue)) {
				xFrames.add(new Keyframe((curTime - prevTimeX) * 20, xPrev == null ? xValue : xPrev, xValue, getEasingForAxis(entryObj, Axis.X, easingType), getEasingArgsForAxis(entryObj, Axis.X, easingArgs)));
				xPrev = xValue;
				prevTimeX = curTime;
			}
			if (isEnabled(yValue)) {
				yFrames.add(new Keyframe((curTime - prevTimeY) * 20, yPrev == null ? yValue : yPrev, yValue, getEasingForAxis(entryObj, Axis.Y, easingType), getEasingArgsForAxis(entryObj, Axis.Y, easingArgs)));
				yPrev = yValue;
				prevTimeY = curTime;
			}
			if (isEnabled(zValue)) {
				zFrames.add(new Keyframe((curTime - prevTimeZ) * 20, zPrev == null ? zValue : zPrev, zValue, getEasingForAxis(entryObj, Axis.Z, easingType), getEasingArgsForAxis(entryObj, Axis.Z, easingArgs)));
				zPrev = zValue;
				prevTimeZ = curTime;
			}
		}

		return new KeyframeStack(addArgsForKeyframes(xFrames, type), addArgsForKeyframes(yFrames, type), addArgsForKeyframes(zFrames, type));
	}

	private static EasingType getEasingForAxis(JsonObject entryObj, Axis axis, EasingType easingType) {
		String memberName = "easing";
		if (axis != null) memberName += axis.name();
		return entryObj != null && entryObj.has(memberName) ? EasingType.fromJson(entryObj.get(memberName)) : easingType;
	}

	private static List<List<Expression>> getEasingArgsForAxis(JsonObject entryObj, Axis axis, List<List<Expression>> easingArg) {
		String memberName = "easingArgs";
		if (axis != null) memberName += axis.name();
		return entryObj != null && entryObj.has(memberName) ?
				JsonUtil.jsonArrayToList(JsonUtil.getAsJsonArray(entryObj, memberName), ele -> Collections.singletonList(FloatExpression.of(ele.getAsFloat()))) :
				easingArg;
	}

	private static List<Keyframe> addArgsForKeyframes(List<Keyframe> frames, TransformType type) {
		if (frames.getFirst().startValue().getFirst() instanceof AccessExpression accessExpression
				&& "disabled".equals(accessExpression.property()) && accessExpression.object() instanceof IdentifierExpression identifierExpression
				&& "pal".equals(identifierExpression.name()))
			return Collections.emptyList();

		if (frames.size() == 1) {
			Keyframe frame = frames.getFirst();

			if (frame.easingType() != EasingType.LINEAR) {
				frames.set(0, new Keyframe(frame.length(), frame.startValue(), frame.endValue()));

				return frames;
			}
		}

		for (int i = 0; i < frames.size(); i++) {
			Keyframe frame = frames.get(i);

			if (frame.easingType() == EasingType.CATMULLROM) {
				frames.set(i, new Keyframe(frame.length(), frame.startValue(), frame.endValue(), frame.easingType(), ObjectArrayList.of(
						i == 0 ? frame.startValue() : frames.get(i - 1).endValue(),
						i + 1 >= frames.size() ? frame.endValue() : frames.get(i + 1).endValue()
				)));
			}
			else if (frame.easingType() == EasingType.BEZIER) {
				List<Expression> leftValue = frame.easingArgs().getFirst();
				List<Expression> rightValue = frame.easingArgs().get(2);
				List<Expression> rightTime = frame.easingArgs().get(3);
				if (type == TransformType.ROTATION) {
					rightValue = toRadiansForBezier(rightValue);
					leftValue = toRadiansForBezier(leftValue);
				}
				frames.set(i, new Keyframe(frame.length(), frame.startValue(), frame.endValue(), frame.easingType(),
						ObjectArrayList.of(leftValue, frame.easingArgs().get(1))));
				if (frame.easingArgs().size() > 4) {
					frames.get(i).easingArgs().add(frame.easingArgs().get(4));
					frames.get(i).easingArgs().add(frame.easingArgs().get(5));
				}
				if (frames.size() > i + 1) {
					Keyframe nextKeyframe = frames.get(i + 1);
					if (nextKeyframe.easingType() != EasingType.BEZIER) {
						frames.set(i + 1, new Keyframe(nextKeyframe.length(), nextKeyframe.startValue(), nextKeyframe.endValue(),
								EasingType.BEZIER, ObjectArrayList.of(PlayerAnimatorLoader.ZERO, PlayerAnimatorLoader.ZERO, rightValue, rightTime))); //TODO Maybe move the ZERO field to UniversalAnimLoader
					}
					else {
						nextKeyframe.easingArgs().add(rightValue);
						nextKeyframe.easingArgs().add(rightTime);
					}
				}
			}
		}

		return frames;
	}


	private static boolean isEnabled(List<Expression> expressions) {
		if (expressions.size() == 1
				&& expressions.getFirst() instanceof AccessExpression access
				&& access.object() instanceof IdentifierExpression id
				&& "pal".equals(id.name())) {

			return !"disabled".equals(access.property()) && !"skip".equals(access.property());
		}

		return true;
	}

	private static List<Expression> toRadiansForBezier(List<Expression> expressions) {
		if (expressions.size() == 1 && IsConstantExpression.test(expressions.getFirst())) {
			return Collections.singletonList(FloatExpression.of(Math.toRadians(MOCHA_ENGINE.eval(expressions))));
		}
		PlayerAnimLib.LOGGER.warn("Invalid easing arguments for bezier: {}\nFor rotations bezier args can only be floats.", expressions);
		return expressions;
	}

	public static float calculateAnimationLength(Map<String, BoneAnimation> boneAnimations) {
		float length = 0;

		for (BoneAnimation animation : boneAnimations.values()) {
			length = Math.max(length, animation.rotationKeyFrames().getLastKeyframeTime());
			length = Math.max(length, animation.positionKeyFrames().getLastKeyframeTime());
			length = Math.max(length, animation.scaleKeyFrames().getLastKeyframeTime());
		}

		return length == 0 ? Float.MAX_VALUE : length;
	}

	private static float readTimestamp(String timestamp) {
		try {
			return Float.parseFloat(timestamp);
		} catch (Throwable th) {
			return 0;
		}
	}
}
