package com.zigythebird.playeranimcore.loading;

import com.google.gson.*;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.ExtraAnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.BoneAnimation;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.animation.keyframe.KeyframeStack;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.ParticleKeyframeData;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.AnimationFormat;
import com.zigythebird.playeranimcore.enums.TransformType;
import com.zigythebird.playeranimcore.math.Vec3f;
import com.zigythebird.playeranimcore.util.ParticleEffectUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.redlance.mocha.parser.ast.Expression;
import org.redlance.mocha.parser.ast.FloatExpression;
import org.redlance.mocha.runtime.standard.MochaMath;

import java.lang.reflect.Type;
import java.util.*;

import static com.zigythebird.playeranimcore.loading.UniversalAnimLoader.NO_KEYFRAMES;

public class PlayerAnimatorLoader implements JsonDeserializer<Animation> {
    public static final List<Expression> ZERO = Collections.singletonList(FloatExpression.ZERO);
    public static final List<Expression> ONE = Collections.singletonList(FloatExpression.ONE);
    private static final int modVersion = 3;

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Animation.class, new PlayerAnimatorLoader())
            .create();

    protected PlayerAnimatorLoader() {}
    
    @Override
    public Animation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject node = json.getAsJsonObject();
        if (!node.has("emote")) {
            throw new JsonParseException("not an emotecraft animation");
        }

        int version = 1;
        if (node.has("version")) {
            version = node.get("version").getAsInt();
        }

        ExtraAnimationData extra = new ExtraAnimationData();
        extra.fromJson(node, true);
        extra.put(ExtraAnimationData.FORMAT_KEY, AnimationFormat.PLAYER_ANIMATOR);

        if (modVersion < version) {
            throw new JsonParseException(extra.name() + " is version " + version + ". Player Animation library can only process version " + modVersion + ".");
        }

        return emoteDeserializer(extra, node.getAsJsonObject("emote"), version);
    }

    private Animation emoteDeserializer(ExtraAnimationData extra, JsonObject node, int version) throws JsonParseException {
        if (version < 3) extra.put(ExtraAnimationData.APPLY_BEND_TO_OTHER_BONES_KEY, true);
        boolean easeBeforeKeyframe = node.has("easeBeforeKeyframe") && node.get("easeBeforeKeyframe").getAsBoolean();
        extra.put(ExtraAnimationData.EASING_BEFORE_KEY, easeBeforeKeyframe);
        float beginTick = 0;
        if (node.has("beginTick")) {
            beginTick = node.get("beginTick").getAsFloat();
            extra.put(ExtraAnimationData.BEGIN_TICK_KEY, beginTick);
        }
        float endTick = beginTick + 1;
        if (node.has("endTick")) {
            endTick = Math.max(node.get("endTick").getAsFloat(), endTick);
            extra.put(ExtraAnimationData.END_TICK_KEY, endTick);
        }
        if(endTick <= 0) throw new JsonParseException("endTick must be bigger than 0");
        Animation.LoopType loopType = Animation.LoopType.PLAY_ONCE;
        if(node.has("isLoop") && node.has("returnTick")) {
            boolean isLooped = node.get("isLoop").getAsBoolean();
            int returnTick = Math.max(node.get("returnTick").getAsInt() - 1, 0);
            if (isLooped) {
                if (returnTick > endTick || returnTick < 0) {
                    throw new JsonParseException("The returnTick has to be a non-negative value smaller than the endTick value");
                }
                if (returnTick == 0) loopType = Animation.LoopType.LOOP;
                else loopType = Animation.LoopType.returnToTickLoop(returnTick);
            }
        }

        float stopTick = node.has("stopTick") ? node.get("stopTick").getAsFloat() : 0;
        if (loopType == Animation.LoopType.PLAY_ONCE) {
            endTick = stopTick <= endTick ? endTick + 3 : stopTick; // https://github.com/KosmX/minecraftPlayerAnimator/blob/1.21/coreLib/src/main/java/dev/kosmx/playerAnim/core/data/KeyframeAnimation.java#L80
        }

        boolean degrees = !node.has("degrees") || node.get("degrees").getAsBoolean();
        Map<String, BoneAnimation> bones = moveDeserializer(node.getAsJsonArray("moves").asList(), degrees, version, endTick);

        //Also shifts all easings to the right by one if easeBeforeKeyframe is false
        //If easings are shifted in order for the last keyframe's easing to not be ignored a 0.001 tick long keyframe gets added at the end with that easing
        //The reason why the last easing can't be ignored is because it's used by endTick lerping
        for (Map.Entry<String, BoneAnimation> boneAnimation : bones.entrySet()) {
            if (!easeBeforeKeyframe) {
                correctEasings(boneAnimation.getValue().positionKeyFrames());
                correctEasings(boneAnimation.getValue().rotationKeyFrames());
                correctEasings(boneAnimation.getValue().scaleKeyFrames());
                correctEasings(boneAnimation.getValue().bendKeyFrames());
            }
            if (boneAnimation.getKey().equals("right_item") || boneAnimation.getKey().equals("left_item")) {
                swapTheZYAxis(boneAnimation.getValue().positionKeyFrames());
                swapTheZYAxis(boneAnimation.getValue().rotationKeyFrames());
            }
        }

        Animation.Keyframes keyframes = NO_KEYFRAMES;
        if (extra.has(ExtraAnimationData.PARTICLE_EFFECTS_KEY)) {
            String identifier = ParticleEffectUtils.parseIdentifier((String) extra.getRaw(ExtraAnimationData.PARTICLE_EFFECTS_KEY));
            keyframes = new Animation.Keyframes(keyframes.sounds(), new ParticleKeyframeData[] {
                    new ParticleKeyframeData(beginTick, identifier, "body", "")
            }, keyframes.customInstructions());
        }

        return new Animation(extra, endTick, loopType, bones, keyframes, new HashMap<>(), new HashMap<>());
    }

    public static void swapTheZYAxis(KeyframeStack rotationStack) {
        List<Keyframe> yKeyframes = new ArrayList<>(rotationStack.yKeyframes());
        rotationStack.yKeyframes().clear();
        rotationStack.yKeyframes().addAll(rotationStack.zKeyframes());
        rotationStack.zKeyframes().clear();
        rotationStack.zKeyframes().addAll(yKeyframes);
    }

    public static void correctEasings(KeyframeStack keyframeStack) {
        correctEasings(keyframeStack.xKeyframes());
        correctEasings(keyframeStack.yKeyframes());
        correctEasings(keyframeStack.zKeyframes());
    }

    public static void correctEasings(List<Keyframe> list) {
        EasingType previousEasing = EasingType.EASE_IN_OUT_SINE;
        List<List<Expression>> previousEasingArgs = new ObjectArrayList<>();
        Keyframe keyframe = null;
        for (int i=0;i<list.size();i++) {
            keyframe = list.get(i);
            list.set(i, new Keyframe(keyframe.length(), keyframe.startValue(), keyframe.endValue(), previousEasing, previousEasingArgs));
            previousEasing = keyframe.easingType();
            previousEasingArgs = keyframe.easingArgs();
        }
        //If the final easing is constant, it defaults to linear insteadAdd commentMore actions
        //If you don't want your anim to have endTick lerp then just set stopTick to endTick + 1
        if (keyframe != null)
            list.add(new Keyframe(0.001F, keyframe.endValue(), keyframe.endValue(), keyframe.easingType(), keyframe.easingArgs()));
    }

    private Map<String, BoneAnimation> moveDeserializer(List<JsonElement> node, boolean degrees, int version, float endTick) {
        Map<String, BoneAnimation> bones = new TreeMap<>();
        node.sort((e1, e2) -> {
            final int i1 = e1.getAsJsonObject().get("tick").getAsInt();
            final int i2 = e2.getAsJsonObject().get("tick").getAsInt();
            return Integer.compare(i1, i2);
        });
        for (JsonElement n : node) {
            JsonObject obj = n.getAsJsonObject();
            float tick = obj.get("tick").getAsFloat();
            if (tick > endTick) continue;
            EasingType easing = easingTypeFromString(obj.has("easing") ? obj.get("easing").getAsString() : "linear");
            int turn = obj.has("turn") ? obj.get("turn").getAsInt() : 0;
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()){
                if(entry.getKey().equals("tick") || entry.getKey().equals("comment") || entry.getKey().equals("easing") || entry.getKey().equals("turn")) {
                    continue;
                }

                String boneKey = UniversalAnimLoader.getCorrectPlayerBoneName(entry.getKey());
                if (version < 3 && boneKey.equals("torso")) boneKey = "body";// rename part

                BoneAnimation bone = bones.computeIfAbsent(UniversalAnimLoader.getCorrectPlayerBoneName(boneKey), boneName ->
                        new BoneAnimation(new KeyframeStack(), new KeyframeStack(), new KeyframeStack(), new ArrayList<>())
                );
                addBodyPartIfExists(boneKey, bone, entry.getValue(), degrees, tick, easing, turn);
            }
        }
        BoneAnimation body = bones.get("body");
        if (body != null && !body.bendKeyFrames().isEmpty()) {
            BoneAnimation torso = bones.computeIfAbsent("torso", name -> new BoneAnimation());
            torso.bendKeyFrames().addAll(body.bendKeyFrames());
            body.bendKeyFrames().clear();
            if (!body.hasKeyframes()) bones.remove("body");
        }
        return bones;
    }

    private void addBodyPartIfExists(String boneName, BoneAnimation bone, JsonElement node, boolean degrees, float tick, EasingType easing, int turn) {
        JsonObject partNode = node.getAsJsonObject();
        boolean isItem = boneName.equals("right_item") || boneName.equals("left_item");
        boolean isCape = boneName.equals("cape");
        boolean isBody = boneName.equals("body");
        fillKeyframeStack(bone.positionKeyFrames(), getDefaultValues(boneName), isBody ? TransformType.POSITION : null, "x", "y", "z", partNode, degrees, tick, easing, turn, isItem, isCape, isBody);
        fillKeyframeStack(bone.rotationKeyFrames(), Vec3f.ZERO, TransformType.ROTATION, "pitch", "yaw", "roll", partNode, degrees, tick, easing, turn, isItem, isCape, isBody);
        fillKeyframeStack(bone.scaleKeyFrames(), Vec3f.ZERO, TransformType.SCALE, "scaleX", "scaleY", "scaleZ", partNode, degrees, tick, easing, turn, false, false, false);
        addPartIfExists(Keyframe.getLastKeyframeTime(bone.bendKeyFrames()), bone.bendKeyFrames(), 0, TransformType.BEND, "bend", partNode, degrees, tick, easing, turn, false);
    }

    private void fillKeyframeStack(KeyframeStack stack, Vec3f def, TransformType transformType, String x, String y, @Nullable String z, JsonObject node, boolean degrees, float tick, EasingType easing, int turn, boolean isItem, boolean isCape, boolean isBody) {
        addPartIfExists(stack.getLastXAxisKeyframeTime(), stack.xKeyframes(), def.x(), transformType, x, node, degrees, tick, easing, turn, isItem || isCape || isBody);
        addPartIfExists(stack.getLastYAxisKeyframeTime(), stack.yKeyframes(), def.y(), transformType, y, node, degrees, tick, easing, turn, isItem || transformType == null || (isBody && transformType == TransformType.ROTATION));
        addPartIfExists(stack.getLastZAxisKeyframeTime(), stack.zKeyframes(), def.z(), transformType, z, node, degrees, tick, easing, turn, (isItem && transformType == TransformType.ROTATION) || isCape);
    }

    private void addPartIfExists(float lastTick, List<Keyframe> part, float def, TransformType transformType, String name, JsonObject node, boolean degrees, float tick, EasingType easing, int rotate, boolean shouldNegate) {
        if (!node.has(name)) return;

        Keyframe lastFrame = part.isEmpty() ? null : part.getLast();
        float prevTime = lastFrame != null ? lastTick : 0;
        float delta = tick - prevTime;

        float value = convertPlayerAnimValue(def, node.get(name).getAsFloat(), transformType, degrees, shouldNegate, rotate);
        List<Expression> expressions = Collections.singletonList(FloatExpression.of(value));
        List<List<Expression>> emptyList = Collections.singletonList(new ObjectArrayList<>(0));

        part.add(new Keyframe(delta, lastFrame == null ? (transformType == TransformType.SCALE ? ONE : ZERO) : lastFrame.endValue(), expressions, easing, emptyList));
    }

    private static float convertPlayerAnimValue(float def, float value, TransformType transformType, boolean degrees, boolean shouldNegate, int rotate) {
        if (transformType == null) value -= def;
        if (shouldNegate) value *= -1;
        if (transformType == TransformType.ROTATION) {
            if (degrees) value = MochaMath.d2r(value);
            value += MochaMath.PI * 2F * rotate;
        }
        if (transformType == TransformType.POSITION) value *= 16;

        return value;
    }

    public static EasingType easingTypeFromString(String string) {
        EasingType easingType = EasingType.fromString(string);
        if (easingType == EasingType.LINEAR) {
            return EasingType.fromString("ease" + string);
        }
        return easingType;
    }

    private static final Map<String, Vec3f> DEFAULT_VALUES = Map.of(
            "right_arm", new Vec3f(-5, 2, 0),
            "left_arm", new Vec3f(5, 2, 0),
            "left_leg", new Vec3f(1.9f, 12, 0.1f),
            "right_leg", new Vec3f(-1.9f, 12, 0.1f)
    );

    public static Vec3f getDefaultValues(String bone) {
        return DEFAULT_VALUES.getOrDefault(bone, Vec3f.ZERO);
    }
}
