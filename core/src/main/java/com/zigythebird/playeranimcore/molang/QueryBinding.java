package com.zigythebird.playeranimcore.molang;

import com.zigythebird.playeranimcore.PlayerAnimLib;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.ExecutionContext;
import team.unnamed.mocha.runtime.value.Function;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.ObjectProperty;
import team.unnamed.mocha.runtime.value.Value;

import java.util.Objects;

public class QueryBinding<T> extends MutableObjectBinding implements ExecutionContext<T> {
    private final T entity;

    public QueryBinding(T entity) {
        this.entity = entity;
    }

    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public @Nullable ObjectProperty getProperty(@NotNull String name) {
        ObjectProperty property = super.getProperty(name);
        if (property == null) return null;

        if (property.value() instanceof Function function && !property.constant()) {
            try {
                return ObjectProperty.property(Objects.requireNonNull(function.evaluate(this)), false);
            } catch (Throwable th) {
                PlayerAnimLib.LOGGER.warn("Failed to evaluate function property '{}'", name, th);
            }
        }

        return property;
    }

    @Override
    public T entity() {
        return this.entity;
    }

    @Override
    public @Nullable Value eval(@NotNull Expression expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Object flag() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flag(@Nullable Object flag) {
        throw new UnsupportedOperationException();
    }
}
