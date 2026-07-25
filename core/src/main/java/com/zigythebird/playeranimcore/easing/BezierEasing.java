package com.zigythebird.playeranimcore.easing;

import com.zigythebird.playeranimcore.animation.keyframe.AnimationPoint;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.standard.MochaMath;

import java.util.ArrayList;
import java.util.List;

public class BezierEasing implements EasingTypeTransformer {
    @Override
    public Float2FloatFunction buildTransformer(@Nullable Float value) {
        return EasingType.easeIn(EasingType::linear);
    }

    @Override
    public float apply(MochaEngine<?> env, AnimationPoint animationPoint, @Nullable Float easingValue, float lerpValue) {
        if (lerpValue >= 1) return animationPoint.animationEndValue();
        if (Float.isNaN(lerpValue) || lerpValue == 0) return animationPoint.animationStartValue();

        List<List<Expression>> easingArgs = animationPoint.easingArgs();

        if (easingArgs == null || easingArgs.isEmpty())
            return MochaMath.lerp(animationPoint.animationStartValue(), animationPoint.animationEndValue(), buildTransformer(null).apply(lerpValue));

        float rightValue;
        float rightTime;
        float leftValue = env.eval(easingArgs.getFirst());
        float leftTime = env.eval(easingArgs.get(1));

        if (easingArgs.size() > 3) {
            rightValue = env.eval(easingArgs.get(2));
            rightTime = env.eval(easingArgs.get(3));
        }
        else {
            rightValue = 0;
            rightTime = 0.1f;
        }

        float transitionLength = animationPoint.transitionLength() / 20f;

        float time_handle_before = rightTime/transitionLength;
        float time_handle_after  = leftTime/transitionLength;

        //Makes sure that when the time handles go past the keyframes that the clamping keeps the same curve
        if (time_handle_before > 1 || time_handle_before < 0) {
            float unclamped = time_handle_before;
            time_handle_before = Math.clamp(time_handle_before, 0, 1);
            rightValue /= 1 + Math.abs(time_handle_before - unclamped);
        }
        if (time_handle_after > 0 || time_handle_after < -1) {
            float unclamped = time_handle_after;
            time_handle_after = Math.clamp(time_handle_after, -1, 0);
            leftValue /= 1 + Math.abs(time_handle_after - unclamped);
        }

        Vector2f P0 = new Vector2f(0, animationPoint.animationStartValue());
        Vector2f P1 = new Vector2f(time_handle_before, animationPoint.animationStartValue() + rightValue);
        Vector2f P2 = new Vector2f(time_handle_after + 1, animationPoint.animationEndValue() + leftValue);
        Vector2f P3 = new Vector2f(1, animationPoint.animationEndValue());

        final List<Vector2f> points = new ArrayList<>();

        final int divisions = 200;
        for (int d = 0; d <= divisions; d++) {
            float t = (float) d /divisions;
            points.add(new Vector2f(
                CubicBezier(t, P0.x, P1.x, P2.x, P3.x),
                CubicBezier(t, P0.y, P1.y, P2.y, P3.y)
            ));
        }

        Vector2f closest = new Vector2f();
        float closest_diff = Float.POSITIVE_INFINITY;
        for (Vector2f point : points) {
            float diff = Math.abs(point.x - lerpValue);
            if (diff < closest_diff) {
                closest_diff = diff;
                closest = point;
            }
		}
        Vector2f second_closest = new Vector2f();
        closest_diff = Float.POSITIVE_INFINITY;
        for (Vector2f point : points) {
            if (point == closest) break;
            float diff = Math.abs(point.x - lerpValue);
            if (diff < closest_diff) {
                closest_diff = diff;
                second_closest = point;
            }
		}
        return MochaMath.lerp(closest.y, second_closest.y, Math.clamp(MochaMath.lerp(closest.x, second_closest.x, lerpValue), 0, 1));
    }

    float CubicBezierP0(float t, float p) {
        float k = 1 - t;
        return k * k * k * p;
    }

   float CubicBezierP1(float t, float p) {
	    final float k = 1 - t;
        return 3 * k * k * t * p;
    }

    float CubicBezierP2(float t, float p) {
        return 3 * ( 1 - t ) * t * t * p;
    }

    float CubicBezierP3(float t, float p) {
        return t * t * t * p;
    }

    /**
     * Computes a point on a Cubic Bezier curve.
     *
     * @param {number} t - The interpolation factor.
     * @param {number} p0 - The first control point.
     * @param {number} p1 - The second control point.
     * @param {number} p2 - The third control point.
     * @param {number} p3 - The fourth control point.
     * @return {number} The calculated point on a Cubic Bezier curve.
     */
    float CubicBezier(float t, float p0, float p1, float p2, float p3) {
        return CubicBezierP0( t, p0 ) + CubicBezierP1( t, p1 ) + CubicBezierP2( t, p2 ) +
                CubicBezierP3( t, p3 );
    }
}
