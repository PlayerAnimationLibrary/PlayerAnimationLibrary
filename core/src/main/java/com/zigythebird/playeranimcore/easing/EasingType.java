package com.zigythebird.playeranimcore.easing;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.zigythebird.playeranimcore.animation.keyframe.Keyframe;
import com.zigythebird.playeranimcore.math.MathHelper;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.standard.MochaMath;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Functional interface defining an easing function
 * <p>
 * {@code value} is the easing value provided from the keyframe's {@link Keyframe#easingArgs()}
 *
 * @see <a href="https://easings.net/">Easings.net</a>
 * @see <a href="https://cubic-bezier.com">Cubic-Bezier.com</a>
 */
public enum EasingType implements EasingTypeTransformer {
	LINEAR(0, "linear", value -> EasingType.easeIn(EasingType::linear)),
	CONSTANT(1, "constant", value -> (value1 -> 0)),
	STEP(37, "step", value -> EasingType.easeIn(EasingType.step(value))),

	EASE_IN_SINE(6, "easeinsine", value -> EasingType.easeIn(EasingType::sine)),
	EASE_OUT_SINE(7, "easeoutsine", value -> EasingType.easeOut(EasingType::sine)),
	EASE_IN_OUT_SINE(8, "easeinoutsine", value -> EasingType.easeInOut(EasingType::sine)),

	EASE_IN_QUAD(12, "easeinquad", value -> EasingType.easeIn(EasingType::quadratic)),
	EASE_OUT_QUAD(13, "easeoutquad", value -> EasingType.easeOut(EasingType::quadratic)),
	EASE_IN_OUT_QUAD(14, "easeinoutquad", value -> EasingType.easeInOut(EasingType::quadratic)),

	EASE_IN_CUBIC(9, "easeincubic", value -> EasingType.easeIn(EasingType::cubic)),
	EASE_OUT_CUBIC(10, "easeoutcubic", value -> EasingType.easeOut(EasingType::cubic)),
	EASE_IN_OUT_CUBIC(11, "easeinoutcubic", value -> EasingType.easeInOut(EasingType::cubic)),

	EASE_IN_QUART(15, "easeinquart", value -> EasingType.easeIn(EasingType.pow(4))),
	EASE_OUT_QUART(16, "easeoutquart", value -> EasingType.easeOut(EasingType.pow(4))),
	EASE_IN_OUT_QUART(17, "easeinoutquart", value -> EasingType.easeInOut(EasingType.pow(4))),

	EASE_IN_QUINT(18, "easeinquint", value -> EasingType.easeIn(EasingType.pow(5))),
	EASE_OUT_QUINT(19, "easeoutquint", value -> EasingType.easeOut(EasingType.pow(5))),
	EASE_IN_OUT_QUINT(20, "easeinoutquint", value -> EasingType.easeInOut(EasingType.pow(5))),

	EASE_IN_EXPO(21, "easeinexpo", value -> EasingType.easeIn(EasingType::exp)),
	EASE_OUT_EXPO(22, "easeoutexpo", value -> EasingType.easeOut(EasingType::exp)),
	EASE_IN_OUT_EXPO(23, "easeinoutexpo", value -> EasingType.easeInOut(EasingType::exp)),

	EASE_IN_CIRC(24, "easeincirc", value -> EasingType.easeIn(EasingType::circle)),
	EASE_OUT_CIRC(25, "easeoutcirc", value -> EasingType.easeOut(EasingType::circle)),
	EASE_IN_OUT_CIRC(26, "easeinoutcirc", value -> EasingType.easeInOut(EasingType::circle)),

	EASE_IN_BACK(27, "easeinback", value -> EasingType.easeIn(EasingType.back(value))),
	EASE_OUT_BACK(28, "easeoutback", value -> EasingType.easeOut(EasingType.back(value))),
	EASE_IN_OUT_BACK(29, "easeinoutback", value -> EasingType.easeInOut(EasingType.back(value))),

	EASE_IN_ELASTIC(30, "easeinelastic", value -> EasingType.easeIn(EasingType.elastic(value))),
	EASE_OUT_ELASTIC(31, "easeoutelastic", value -> EasingType.easeOut(EasingType.elastic(value))),
	EASE_IN_OUT_ELASTIC(32, "easeinoutelastic", value -> EasingType.easeInOut(EasingType.elastic(value))),

	EASE_IN_BOUNCE(33, "easeinbounce", value -> EasingType.easeIn(EasingType.bounce(value))),
	EASE_OUT_BOUNCE(34, "easeoutbounce", value -> EasingType.easeOut(EasingType.bounce(value))),
	EASE_IN_OUT_BOUNCE(35, "easeinoutbounce", value -> EasingType.easeInOut(EasingType.bounce(value))),

	CATMULLROM(36, "catmullrom", new CatmullRomEasing()),
	// 37 - STEP

	BEZIER(38, "bezier", new BezierEasing());

	public final byte id;
	public final String name;
	private final EasingTypeTransformer transformer;

	private static final Map<String, EasingType> BY_NAME = new ConcurrentHashMap<>(64);
	private static final Map<Byte, EasingType> BY_ID = new ConcurrentHashMap<>(64);

	EasingType(int id, String name, EasingTypeTransformer transformer) {
		this.id = (byte) id;
		this.name = name;
		this.transformer = transformer;
	}

	static {
		for (EasingType type : values()) {
			BY_NAME.putIfAbsent(type.name.toLowerCase(Locale.ROOT), type);
			BY_ID.putIfAbsent(type.id, type);
		}
	}

	@Override
	public Float2FloatFunction buildTransformer(@Nullable Float value) {
		return this.transformer.buildTransformer(value);
	}

	public float apply(MochaEngine<?> env, float startValue, float endValue, float transitionLength, float lerpValue, @Nullable List<List<Expression>> easingArgs) {
		return this.transformer.apply(env, startValue, endValue, transitionLength, lerpValue, easingArgs);
	}

	public static float lerpWithOverride(MochaEngine<?> env, float startValue, float endValue, float transitionLength, float lerpValue, @Nullable List<List<Expression>> easingArgs, EasingType easingType, @Nullable EasingType override) {
		EasingType easing = override != null ? override : easingType;
		return easing.apply(env, startValue, endValue, transitionLength, lerpValue, easingArgs);
	}

	@Override
	public float apply(float startValue, float endValue, float lerpValue) {
		return this.transformer.apply(startValue, endValue, lerpValue);
	}

	@Override
	public float apply(float startValue, float endValue, @Nullable Float easingValue, float lerpValue) {
		return this.transformer.apply(startValue, endValue, lerpValue);
	}

	/**
	 * Retrieve an {@code EasingType} instance based on a {@link JsonElement}. Returns one of the default {@code EasingTypes} if the name matches, or any other registered {@code EasingType} with a matching name
	 *
	 * @param json The {@code easing} {@link JsonElement} to attempt to parse.
	 * @return A usable {@code EasingType} instance
	 */
	public static EasingType fromJson(JsonElement json) {
		if (!(json instanceof JsonPrimitive primitive) || !primitive.isString())
			return LINEAR;

		return fromString(primitive.getAsString().toLowerCase(Locale.ROOT));
	}

	/**
	 * Get an existing {@code EasingType} from a given string, matching the string to its name
	 *
	 * @param name The name of the easing function
	 * @return The relevant {@code EasingType}, or {@link EasingType#LINEAR} if none match
	 */
	public static EasingType fromString(String name) {
		return BY_NAME.getOrDefault(name.toLowerCase(Locale.ROOT), EasingType.LINEAR);
	}

	// ---> Easing Transition Type Functions <--- //

	/**
	 * Returns an easing function running forward in time
	 */
	public static Float2FloatFunction easeIn(Float2FloatFunction function) {
		return function;
	}

	/**
	 * Returns an easing function running backwards in time
	 */
	public static Float2FloatFunction easeOut(Float2FloatFunction function) {
		return time -> 1 - function.apply(1 - time);
	}

	/**
	 * Returns an easing function that runs equally both forwards and backwards in time based on the halfway point, generating a symmetrical curve
	 */
	public static Float2FloatFunction easeInOut(Float2FloatFunction function) {
		return time -> {
			if (time < 0.5d) {
				return function.apply(time * 2f) / 2f;
			}

			return 1 - function.apply((1 - time) * 2f) / 2f;
		};
	}

	// ---> Mathematical Functions <--- //

	/**
	 * A linear function, equivalent to a null-operation
	 * <p>
	 * {@code f(n) = n}
	 */
	public static float linear(float n) {
		return n;
	}

	/**
	 * A quadratic function, equivalent to the square (<i>n</i>^2) of elapsed time
	 * <p>
	 * {@code f(n) = n^2}
	 * <p>
	 * <a href="http://easings.net/#easeInQuad">Easings.net#easeInQuad</a>
	 */
	public static float quadratic(float n) {
		return n * n;
	}

	/**
	 * A cubic function, equivalent to cube (<i>n</i>^3) of elapsed time
	 * <p>
	 * {@code f(n) = n^3}
	 * <p>
	 * <a href="http://easings.net/#easeInCubic">Easings.net#easeInCubic</a>
	 */
	public static float cubic(float n) {
		return n * n * n;
	}

	/**
	 * A sinusoidal function, equivalent to a sine curve output
	 * <p>
	 * {@code f(n) = 1 - cos(n * π / 2)}
	 * <p>
	 * <a href="http://easings.net/#easeInSine">Easings.net#easeInSine</a>
	 */
	public static float sine(float n) {
		return 1 - MathHelper.cos(n * MochaMath.PI / 2f);
	}

	/**
	 * A circular function, equivalent to a normally symmetrical curve
	 * <p>
	 * {@code f(n) = 1 - sqrt(1 - n^2)}
	 * <p>
	 * <a href="http://easings.net/#easeInCirc">Easings.net#easeInCirc</a>
	 */
	public static float circle(float n) {
		return 1 - MochaMath.sqrt(1 - n * n);
	}

	/**
	 * An exponential function, equivalent to an exponential curve
	 * <p>
	 * {@code f(n) = 2^(10 * (n - 1))}
	 * <p>
	 * <a href="http://easings.net/#easeInExpo">Easings.net#easeInExpo</a>
	 */
	public static float exp(float n) {
		return MochaMath.pow(2, 10 * (n - 1));
	}

	// ---> Easing Curve Functions <--- //

	/**
	 * An elastic function, equivalent to an oscillating curve
	 * <p>
	 * <i>n</i> defines the elasticity of the output
	 * <p>
	 * {@code f(t) = 1 - (cos(t * π) / 2))^3 * cos(t * n * π)}
	 * <p>
	 * <a href="http://easings.net/#easeInElastic">Easings.net#easeInElastic</a>
	 */
	public static Float2FloatFunction elastic(Float n) {
		float n2 = n == null ? 1 : n;

		return t -> 1 - MochaMath.pow(MathHelper.cos(t * MochaMath.PI / 2f), 3) * MathHelper.cos(t * n2 * MochaMath.PI);
	}

	/**
	 * A bouncing function, equivalent to a bouncing ball curve
	 * <p>
	 * <i>n</i> defines the bounciness of the output
	 * <p>
	 * Thanks to <b>Waterded#6455</b> for making the bounce adjustable, and <b>GiantLuigi4#6616</b> for additional cleanup
	 * <p>
	 * <a href="http://easings.net/#easeInBounce">Easings.net#easeInBounce</a>
	 */
	public static Float2FloatFunction bounce(Float n) {
		final float n2 = n == null ? 0.5f : n;

		Float2FloatFunction one = x -> 121f / 16f * x * x;
		Float2FloatFunction two = x -> 121f / 4f * n2 * MochaMath.pow(x - 6f / 11f, 2) + 1 - n2;
		Float2FloatFunction three = x -> 121 * n2 * n2 * MochaMath.pow(x - 9f / 11f, 2) + 1 - n2 * n2;
		Float2FloatFunction four = x -> 484 * n2 * n2 * n2 * MochaMath.pow(x - 10.5f / 11f, 2) + 1 - n2 * n2 * n2;

		return t -> Math.min(Math.min(one.apply(t), two.apply(t)), Math.min(three.apply(t), four.apply(t)));
	}

	/**
	 * A negative elastic function, equivalent to inverting briefly before increasing
	 * <p>
	 * <code>f(t) = t^2 * ((n * 1.70158 + 1) * t - n * 1.70158)</code>
	 * <p>
	 * <a href="https://easings.net/#easeInBack">Easings.net#easeInBack</a>
	 */
	public static Float2FloatFunction back(Float n) {
		final float n2 = n == null ? 1.70158f : n * 1.70158f;

		return t -> t * t * ((n2 + 1) * t - n2);
	}

	/**
	 * An exponential function, equivalent to an exponential curve to the {@code n} root
	 * <p>
	 * <code>f(t) = t^n</code>
	 *
	 * @param n The exponent
	 */
	public static Float2FloatFunction pow(float n) {
		return t -> MochaMath.pow(t, n);
	}

	// The MIT license notice below applies to the function step
	/**
	 * The MIT License (MIT)
	 *<br><br>
	 * Copyright (c) 2015 Boris Chumichev
	 *<br><br>
	 * Permission is hereby granted, free of charge, to any person obtaining a copy
	 * of this software and associated documentation files (the "Software"), to deal
	 * in the Software without restriction, including without limitation the rights
	 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	 * copies of the Software, and to permit persons to whom the Software is
	 * furnished to do so, subject to the following conditions:
	 *<br><br>
	 * The above copyright notice and this permission notice shall be included in
	 * all copies or substantial portions of the Software.
	 *<br><br>
	 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	 * SOFTWARE.
	 * <br><br>
	 * Returns a stepped value based on the nearest step to the input value.<br>
	 * The size (grade) of the steps depends on the provided value of {@code n}
	 **/
	public static Float2FloatFunction step(Float n) {
		float n2 = n == null ? 2 : n;

		if (n2 < 2)
			throw new IllegalArgumentException("Steps must be >= 2, got: " + n2);

		final int steps = (int)n2;

		return t -> {
			float result = 0;

			if (t < 0)
				return result;

			float stepLength = (1 / (float)steps);

			if (t > (result = (steps - 1) * stepLength))
				return result;

			int testIndex;
			int leftBorderIndex = 0;
			int rightBorderIndex = steps - 1;

			while (rightBorderIndex - leftBorderIndex != 1) {
				testIndex = leftBorderIndex + (rightBorderIndex - leftBorderIndex) / 2;

				if (t >= testIndex * stepLength) {
					leftBorderIndex = testIndex;
				}
				else {
					rightBorderIndex = testIndex;
				}
			}

			return leftBorderIndex * stepLength;
		};
	}

	public static EasingType fromId(byte id) {
		return BY_ID.getOrDefault(id, EasingType.LINEAR);
	}
}
