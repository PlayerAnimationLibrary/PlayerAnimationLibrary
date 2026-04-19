package com.zigythebird.playeranim.neoforge.event;

import com.zigythebird.playeranim.accessors.IAnimatedAvatar;
import com.zigythebird.playeranim.animation.AvatarAnimManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Avatar;
import net.neoforged.bus.api.Event;

/**
 * If you don't want to create your own mixin, you can use this event to add animation to players<br>
 * <b>The event will fire for every player</b> and if the player reloads, it will fire again.<br>
 * <hr>
 * NOTE: When the event fires, {@link IAnimatedAvatar#playerAnimLib$getAnimManager()} will be null you'll have to use the given stack.
 */
public class PlayerAnimationRegisterEvent extends Event {
    private final Avatar avatar;
    private final AvatarAnimManager manager;

    /**
     * Player object is in construction, it will be invoked when you can register animation
     * It will be invoked for every player only ONCE
     */
    public PlayerAnimationRegisterEvent(Avatar avatar, AvatarAnimManager manager) {
        this.avatar = avatar;
        this.manager = manager;
    }

    public Avatar getAvatar() {
        return this.avatar;
    }

    public AvatarAnimManager getAnimManager() {
        return this.manager;
    }
}
