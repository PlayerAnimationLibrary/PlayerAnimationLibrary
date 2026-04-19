package com.zigythebird.playeranim.api;

import com.zigythebird.playeranim.animation.AvatarAnimManager;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Animation factory, the factory will be invoked whenever a client-player is constructed.
 * The returned animation will be automatically registered and added to playerAssociated data.
 * <p>
 * {@link PlayerAnimationAccess#REGISTER_ANIMATION_EVENT} is invoked <strong>after</strong> factories are done.
 */
public interface PlayerAnimationFactory {

    FactoryHolder ANIMATION_DATA_FACTORY = new FactoryHolder();

    @Nullable IAnimation invoke(@NotNull Avatar player);

    class FactoryHolder {
        private FactoryHolder() {}

        private static final List<Function<Avatar, DataHolder>> factories = new ArrayList<>();

        /**
         * Animation factory
         * @param id       animation id or <code>null</code> if you don't want to add to playerAssociated data
         * @param priority animation priority
         * @param factory  animation factory
         */
        public void registerFactory(@Nullable Identifier id, int priority, @NotNull PlayerAnimationFactory factory) {
            factories.add(player -> Optional.ofNullable(factory.invoke(player)).map(animation -> new DataHolder(id, priority, animation)).orElse(null));
        }

        @ApiStatus.Internal
        private record DataHolder(@Nullable Identifier id, int priority, @NotNull IAnimation animation) {}

        @ApiStatus.Internal
        public void prepareAnimations(Avatar player, AvatarAnimManager playerStack, Map<Identifier, IAnimation> animationMap) {
            for (Function<Avatar, DataHolder> factory: factories) {
                DataHolder dataHolder = factory.apply(player);
                if (dataHolder != null) {
                    playerStack.addAnimLayer(dataHolder.priority(), dataHolder.animation());
                    if (dataHolder.id() != null) {
                        animationMap.put(dataHolder.id(), dataHolder.animation());
                    }
                }
            }
        }
    }

}
