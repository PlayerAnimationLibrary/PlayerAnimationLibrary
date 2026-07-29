package com.zigythebird.playeranimcore.molang;

import com.google.gson.JsonElement;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.event.MolangEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.parser.MolangParser;
import team.unnamed.mocha.parser.ParseException;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.parser.ast.FloatExpression;
import team.unnamed.mocha.runtime.IsConstantExpression;
import team.unnamed.mocha.runtime.value.NumberValue;
import team.unnamed.mocha.runtime.value.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class MolangLoader {
    private static final Consumer<ParseException> HANDLER = e -> PlayerAnimLib.LOGGER.warn("Failed to parse!", e);

    /**
     * Common compiler, use only for compiling constants!
     */
    public static final MochaEngine<?> MOCHA_ENGINE = MolangLoader.createNewEngine();

    @Contract("_, null, _ -> new; _, !null, _ -> new")
    public static List<Expression> parseJson(boolean isForRotation, @Nullable JsonElement element, @NotNull Expression defaultValue) {
        return parseJson(isForRotation, element, Collections.singletonList(defaultValue));
    }

    @Contract("_, null, _ -> param3; _, !null, _ -> !null")
    public static List<Expression> parseJson(boolean isForRotation, @Nullable JsonElement element, @NotNull List<Expression> defaultValue) {
        if (element == null) return defaultValue;

        List<Expression> expressions;
        try (MolangParser parser = MolangParser.parser(element.getAsString())) {
            List<Expression> expressions1 = parser.parseAll();
            if (expressions1.size() == 1 && isForRotation && IsConstantExpression.test(expressions1.getFirst())) {
                expressions = new ArrayList<>();
                expressions.add(FloatExpression.of(Math.toRadians(MOCHA_ENGINE.eval(expressions1))));
            } else {
                expressions = expressions1;
            }
        } catch (IOException e) {
            PlayerAnimLib.LOGGER.error("Failed to compile molang '{}'!", element, e);
            return defaultValue;
        }
        return expressions;
    }

    public static MochaEngine<AnimationController> createNewEngine(AnimationController controller) {
        MochaEngine<AnimationController> engine = createBaseEngine(controller);

        QueryBinding<AnimationController> queryBinding = new QueryBinding<>(controller);
        setDoubleQuery(queryBinding, "anim_time", AnimationController::getAnimationTime);
        setDoubleQuery(queryBinding, "controller_speed", AnimationController::getAnimationSpeed);

        MolangEvent.MOLANG_EVENT.invoker().registerMolangQueries(controller, engine, queryBinding);
        queryBinding.block(); // make immutable

        engine.scope().set("query", queryBinding);
        engine.scope().set("q", queryBinding);
        return engine;
    }

    public static MochaEngine<?> createNewEngine() {
        return createNewEngine(null);
    }

    public static <T> MochaEngine<T> createBaseEngine(T entity) {
        MochaEngine<T> engine = MochaEngine.createStandard(entity);
        engine.handleParseExceptions(MolangLoader.HANDLER);
        engine.warnOnReflectiveFunctionUsage(true);

        engine.scope().set("math", new MochaMathExtensions(engine.scope().getProperty("math")));
        return engine;
    }

    public static <T> boolean setDoubleQuery(QueryBinding<T> binding, String name, ToDoubleFunction<T> value) {
        return setControllerQuery(binding, name, controller -> NumberValue.of(value.applyAsDouble(controller)), NumberValue.of(0.0f));
    }

    public static <T> boolean setBoolQuery(QueryBinding<T> binding, String name, Function<T, Boolean> value) {
        return setControllerQuery(binding, name, controller -> Value.of((boolean) value.apply(controller)), Value.of(false));
    }

    /**
     * some shit code
     */
    public static <T> boolean setControllerQuery(QueryBinding<T> binding, String name, Function<T, Value> value, Value defaultValue) {
        return binding.set(name, (team.unnamed.mocha.runtime.value.Function<T>)
                (ctx, args) -> {
                    try {
                        return value.apply(ctx.entity());
                    } catch (Exception e) {
                        PlayerAnimLib.LOGGER.error(e.toString());
                        return defaultValue;
                    }
                }
        );
    }

    public static boolean isConstant(List<Expression> expressions) {
        return expressions.stream().anyMatch(IsConstantExpression::test);
    }
}
