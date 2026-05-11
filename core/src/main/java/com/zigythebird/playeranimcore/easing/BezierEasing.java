package com.zigythebird.playeranimcore.easing;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.redlance.mocha.parser.ast.Expression;
import org.redlance.mocha.runtime.MolangInterpreter;
import org.redlance.mocha.runtime.standard.MochaMath;

import java.util.ArrayList;
import java.util.List;

abstract class BezierEasing implements EasingTypeTransformer {
    @Override
    public Float2FloatFunction buildTransformer(@Nullable Float value) {
        return EasingType.easeIn(EasingType::linear);
    }

    abstract boolean isEasingBefore();

    @Override
    public float apply(MolangInterpreter<?> env, float startValue, float endValue, float transitionLength, float lerpValue, @Nullable List<List<Expression>> easingArgs) {
        if (lerpValue >= 1) return endValue;
        if (Float.isNaN(lerpValue)) return startValue;

        if (easingArgs == null || easingArgs.isEmpty())
            return MochaMath.lerp(startValue, endValue, buildTransformer(null).apply(lerpValue));

        float rightValue = isEasingBefore() ? 0 : env.eval(easingArgs.getFirst());
        float rightTime = isEasingBefore() ? 0.1f : env.eval(easingArgs.get(1));
        float leftValue = isEasingBefore() ? env.eval(easingArgs.getFirst()) : 0;
        float leftTime = isEasingBefore() ? env.eval(easingArgs.get(1)) : -0.1f;

        if (easingArgs.size() > 3) {
            rightValue = env.eval(easingArgs.get(2));
            rightTime = env.eval(easingArgs.get(3));
        }

        leftValue = (float) Math.toRadians(leftValue);
        rightValue = (float) Math.toRadians(rightValue);

        float gapTime = transitionLength / 20;

        float time_handle_before = Math.clamp(rightTime, 0, gapTime);
        float time_handle_after  = Math.clamp(leftTime, -gapTime, 0);

        CubicBezierCurve curve = new CubicBezierCurve(
                new Vector2f(0, startValue),
                new Vector2f(time_handle_before, startValue + rightValue),
                new Vector2f(time_handle_after + gapTime, endValue + leftValue),
                new Vector2f(gapTime, endValue));
        float time = gapTime * lerpValue;

        List<Vector2f> points = curve.getPoints(200);
        Vector2f closest  = new Vector2f();
        float closest_diff = Float.POSITIVE_INFINITY;
        for (Vector2f point : points) {
            float diff = Math.abs(point.x - time);
            if (diff < closest_diff) {
                closest_diff = diff;
                closest.set(point);
            }
        }
        Vector2f second_closest = new Vector2f();
        closest_diff = Float.POSITIVE_INFINITY;
        for (Vector2f point : points) {
            if (point == closest) continue;
            float diff = Math.abs(point.x - time);
            if (diff < closest_diff) {
                closest_diff = diff;
                second_closest.set(closest);
                second_closest.set(point);
            }
        }
        return MochaMath.lerp(closest.y, second_closest.y, Math.clamp(MochaMath.lerp(closest.x, second_closest.x, time), 0, 1));
    }
}

class BezierEasingBefore extends BezierEasing {
    @Override
    boolean isEasingBefore() {
        return true;
    }
}

class BezierEasingAfter extends BezierEasing {
    @Override
    boolean isEasingBefore() {
        return false;
    }
}

class CubicBezierCurve {
    private Vector2f v0;
    private Vector2f v1;
    private Vector2f v2;
    private Vector2f v3;

    public CubicBezierCurve(Vector2f v0, Vector2f v1, Vector2f v2, Vector2f v3) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    public Vector2f getPoint(float t) {
        return getPoint(t, new Vector2f());
    }

    public Vector2f getPoint(float t, Vector2f target) {
        if (target == null) {
            target = new Vector2f();
        }

        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        target.x = uuu * v0.x + 3 * uu * t * v1.x + 3 * u * tt * v2.x + ttt * v3.x;
        target.y = uuu * v0.y + 3 * uu * t * v1.y + 3 * u * tt * v2.y + ttt * v3.y;

        return target;
    }

    public List<Vector2f> getPoints(int divisions) {
        List<Vector2f> points = new ArrayList<>();

        for (int i = 0; i <= divisions; i++) {
            points.add(getPoint((float) i / divisions));
        }

        return points;
    }
}
