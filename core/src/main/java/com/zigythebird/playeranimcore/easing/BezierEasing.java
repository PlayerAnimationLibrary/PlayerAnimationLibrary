package com.zigythebird.playeranimcore.easing;

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
    public float apply(MochaEngine<?> env, float startValue, float endValue, float transitionLength, float lerpValue, @Nullable List<List<Expression>> easingArgs) {
        if (lerpValue >= 1) return endValue;
        if (Float.isNaN(lerpValue)) return startValue;

        if (easingArgs == null || easingArgs.isEmpty())
            return MochaMath.lerp(startValue, endValue, buildTransformer(null).apply(lerpValue));

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

        float transitionLength = animationPoint.transitionLength()/20f;

        float time_handle_before = Math.clamp(rightTime/transitionLength, 0, 1);
        float time_handle_after  = Math.clamp(leftTime/transitionLength, -1, 0);

        ModVector2d P0 = new ModVector2d(0, animationPoint.animationStartValue());
        ModVector2d P1 = new ModVector2d(time_handle_before, animationPoint.animationStartValue() + rightValue);
        ModVector2d P2 = new ModVector2d(time_handle_after + 1, animationPoint.animationEndValue() + leftValue);
        ModVector2d P3 = new ModVector2d(1, animationPoint.animationEndValue());

        // Determine t
        float t;
        if (lerpValue == P0.x) {
            // Handle corner cases explicitly to prevent rounding errors
            t = 0;
        } else if (lerpValue == P3.x) {
            t = 1;
        } else {
            // Calculate t
            float a = -P0.x + 3 * P1.x - 3 * P2.x + P3.x;
            float b = 3 * P0.x - 6 * P1.x + 3 * P2.x;
            float c = -3 * P0.x + 3 * P1.x;
            float d = P0.x - lerpValue;
            Float tTemp = SolveCubic(a, b, c, d);
            if (tTemp == null) return animationPoint.animationEndValue();
            t = tTemp;
        }

        // Calculate y from t
        return Cubed(1 - t) * P0.y
                + 3 * t * Squared(1 - t) * P1.y
                + 3 * Squared(t) * (1 - t) * P2.y
                + Cubed(t) * P3.y;
    }

    // Solves the equation ax³+bx²+cx+d = 0 for x ϵ ℝ
    // and returns the first result in [0, 1] or null.
    private static Float SolveCubic(float a, float b, float c, float d) {
        if (a == 0) return SolveQuadratic(b, c, d);
        if (d == 0) return 0f;

        b /= a;
        c /= a;
        d /= a;
        float q = (3 * c - Squared(b)) / 9;
        float r = (-27 * d + b * (9 * c - 2 * Squared(b))) / 54;
        float disc = Cubed(q) + Squared(r);
        float term1 = b / 3;

        if (disc > 0) {
            float s = (float) (r + Math.sqrt(disc));
            s = (s < 0) ? -CubicRoot(-s) : CubicRoot(s);
            float t = (float) (r - Math.sqrt(disc));
            t = (t < 0) ? -CubicRoot(-t) : CubicRoot(t);

            float result = -term1 + s + t;
            if (result >= 0 && result <= 1) return result;
        } else if (disc == 0) {
            float r13 = (r < 0) ? -CubicRoot(-r) : CubicRoot(r);

            float result = -term1 + 2 * r13;
            if (result >= 0 && result <= 1) return result;

            result = -(r13 + term1);
            if (result >= 0 && result <= 1) return result;
        } else {
            q = -q;
            float dum1 = q * q * q;
            dum1 = (float) Math.acos(r / Math.sqrt(dum1));
            float r13 = (float) (2 * Math.sqrt(q));

            float result = (float) (-term1 + r13 * Math.cos(dum1 / 3));
            if (result >= 0 && result <= 1) return result;

            result = (float) (-term1 + r13 * Math.cos((dum1 + 2 * Math.PI) / 3));
            if (result >= 0 && result <= 1) return result;

            result = (float) (-term1 + r13 * Math.cos((dum1 + 4 * Math.PI) / 3));
            if (result >= 0 && result <= 1) return result;
        }

        return null;
    }

    // Solves the equation ax² + bx + c = 0 for x ϵ ℝ
    // and returns the first result in [0, 1] or null.
    private static Float SolveQuadratic(float a, float b, float c) {
        float result = (float) ((-b + Math.sqrt(Squared(b) - 4 * a * c)) / (2 * a));
        if (result >= 0 && result <= 1) return result;

        result = (float) ((-b - Math.sqrt(Squared(b) - 4 * a * c)) / (2 * a));
        if (result >= 0 && result <= 1) return result;

        return null;
    }

    private static float Squared(float f) { return f * f; }

    private static float Cubed(float f) { return f * f * f; }

    private static float CubicRoot(float f) { return (float) Math.pow(f, 1.0 / 3.0); }
}
