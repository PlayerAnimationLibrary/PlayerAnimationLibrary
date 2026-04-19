package com.zigythebird.playeranim;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.keyframe.event.builtin.AutoPlayingSoundKeyframeHandler;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranim.molang.MolangQueries;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.keyframe.event.CustomKeyFrameEvents;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.event.MolangEvent;
import net.minecraft.resources.Identifier;

public abstract class PlayerAnimLibMod extends PlayerAnimLib {
    public static final Identifier ANIMATION_LAYER_ID = PlayerAnimLibMod.id("factory");

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    protected void init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(ANIMATION_LAYER_ID, 1000,
                player -> new PlayerAnimationController(player, (_, _, _) -> PlayState.STOP)
        );
        MolangEvent.MOLANG_EVENT.register((_, _, queryBinding) ->
                MolangQueries.setDefaultQueryValues(queryBinding)
        );
        CustomKeyFrameEvents.SOUND_KEYFRAME_EVENT.register(new AutoPlayingSoundKeyframeHandler());
    }
}
