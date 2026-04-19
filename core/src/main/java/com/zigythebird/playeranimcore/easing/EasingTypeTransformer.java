package com.zigythebird.playeranimcore.easing;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.standard.MochaMath;
import team.unnamed.mocha.runtime.value.ObjectValue;

import java.util.List;

@FunctionalInterface
public interface EasingTypeTransformer extends ObjectValue.FloatFunction3 {
    Float2FloatFunction buildTransformer(@Nullable Float value);

    default float apply(MochaEngine<?> env, float startValue, float endValue, float transitionLength, float lerpValue, @Nullable List<List<Expression>> easingArgs) {
        if (lerpValue >= 1) return endValue;
        if (Float.isNaN(lerpValue)) return startValue;

        Float easingVariable = null;
        if (easingArgs != null && !easingArgs.isEmpty()) {
            easingVariable = env.eval(easingArgs.getFirst());
        }
        return apply(startValue, endValue, easingVariable, lerpValue);
    }

    @Override
    default float apply(float startValue, float endValue, float lerpValue) {
        return apply(startValue, endValue, null, lerpValue);
    }

    default float apply(float startValue, float endValue, @Nullable Float easingValue, float lerpValue) {
        return MochaMath.lerp(startValue, endValue, buildTransformer(easingValue).apply(lerpValue));
    }
}
