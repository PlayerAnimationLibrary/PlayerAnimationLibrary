package com.zigythebird.playeranim.api;

import com.zigythebird.playeranim.accessors.IAnimatedAvatar;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.event.Event;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerAnimationAccess {
    /**
     * Get the animation manager for a player entity on the client.
     *
     * @param avatar The ClientPlayer object
     * @return The players' animation manager
     */
    public static AvatarAnimManager getPlayerAnimManager(Avatar avatar) throws IllegalArgumentException {
        if (avatar instanceof IAnimatedAvatar animatedAvatar) {
            return animatedAvatar.playerAnimLib$getAnimManager();
        } else throw new IllegalArgumentException(avatar + " is not a player or library mixins failed");
    }

    /**
     * Get the player animator (usually a {@link AnimationController}) associated with an id.
     * @param avatar player entity
     * @throws IllegalArgumentException if the given argument is not a player, or api mixins have failed (normally never)
     * @implNote data is stored in the player object (using mixins), using it is more efficient than any objectMap as objectMap solution does not know when to delete the data.
     */
    public static @Nullable IAnimation getPlayerAnimationLayer(@NotNull Avatar avatar, @NotNull Identifier id) {
        if (avatar instanceof IAnimatedAvatar animatedPlayer) {
            return animatedPlayer.playerAnimLib$getAnimation(id);
        } else throw new IllegalArgumentException(avatar + " is not a player or library mixins failed");
    }

    /**
     * If you don't want to create your own mixin, you can use this event to add animation to players<br>
     * <b>The event will fire for every player</b> and if the player reloads, it will fire again.<br>
     * <hr>
     * NOTE: When the event fires, {@link IAnimatedAvatar#playerAnimLib$getAnimManager()} will be null you'll have to use the given stack.
     */
    public static final Event<AnimationRegister> REGISTER_ANIMATION_EVENT = new Event<>(listeners -> (player, animationStack) -> {
        for (AnimationRegister listener : listeners) {
            listener.registerAnimation(player, animationStack);
        }
    });

    @FunctionalInterface
    public interface AnimationRegister {
        /**
         * Player object is in construction, it will be invoked when you can register animation
         * It will be invoked for every player only ONCE
         */
        void registerAnimation(@NotNull Avatar avatar, @NotNull AvatarAnimManager manager);
    }
}
