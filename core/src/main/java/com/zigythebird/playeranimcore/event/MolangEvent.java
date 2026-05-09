package com.zigythebird.playeranimcore.event;

import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.molang.QueryBinding;
import team.unnamed.mocha.runtime.MolangInterpreter;

/**
 * Register you own Molang queries and variables.
 */
public class MolangEvent {
    public static final Event<MolangEventInterface> MOLANG_EVENT = new Event<>(listeners -> (controller, engine, queryBinding) -> {
        for (MolangEventInterface listener : listeners) {
            listener.registerMolangQueries(controller, engine, queryBinding);
        }
    });

    @FunctionalInterface
    public interface MolangEventInterface {
        void registerMolangQueries(AnimationController controller, MolangInterpreter<AnimationController> engine, QueryBinding<AnimationController> queryBinding);
    }
}
