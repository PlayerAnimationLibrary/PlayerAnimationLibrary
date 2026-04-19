package com.zigythebird.playeranim.animation;

import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranimcore.PlayerAnimLib;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.loading.UniversalAnimLoader;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Cache class for holding loaded {@link Animation Animations}
 */
public class PlayerAnimResources implements ResourceManagerReloadListener {
	public static final Identifier KEY = PlayerAnimLibMod.id("animation");
	private static final Map<Identifier, Animation> ANIMATIONS = new Object2ObjectOpenHashMap<>();

	/**
	 * Get an animation from the registry, using Identifier(mod_id, animation_name) as the key.
	 * @return animation, <code>null</code> if no animation
	 */
	public static @Nullable Animation getAnimation(Identifier id) {
		return ANIMATIONS.get(id);
	}

	/**
	 * Get Optional animation from registry
	 * @param identifier identifier
	 * @return Optional animation
	 */
	@NotNull
	public static Optional<Animation> getAnimationOptional(@NotNull Identifier identifier) {
		return Optional.ofNullable(getAnimation(identifier));
	}

	/**
	 * @return an unmodifiable map of all the animations
	 */
	public static Map<Identifier, Animation> getAnimations() {
		return Collections.unmodifiableMap(ANIMATIONS);
	}

	/**
	 * Returns the animations of a specific mod/namespace
	 * @param modid namespace (assets/modid)
	 * @return map of path and animations
	 */
	@NotNull
	public static Map<String, Animation> getModAnimations(@NotNull String modid) {
		HashMap<String, Animation> map = new HashMap<>();
		for (Map.Entry<Identifier, Animation> entry: ANIMATIONS.entrySet()) {
			if (entry.getKey().getNamespace().equals(modid)) {
				map.put(entry.getKey().getPath(), entry.getValue());
			}
		}
		return map;
	}

	/**
	 * @param id ID of the desired animation.
	 * @return Returns true if that animation is available.
	 */
	public static boolean hasAnimation(Identifier id) {
		return ANIMATIONS.containsKey(id);
	}

	/**
	 * Load animations using ResourceManager
	 * Internal use only!
	 */
	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		ANIMATIONS.clear();

		for (var resource : manager.listResources("player_animations", resourceLocation -> resourceLocation.getPath().endsWith(".json")).entrySet()) {
			String namespace = resource.getKey().getNamespace();
			try (InputStream is = resource.getValue().open()) {
				for (var entry : UniversalAnimLoader.loadAnimations(is).entrySet()) {
					ANIMATIONS.put(Identifier.fromNamespaceAndPath(namespace, entry.getKey()), entry.getValue());
				}
			} catch (Exception e) {
				PlayerAnimLib.LOGGER.error("Player Animation Library failed to load animation {} because:", resource.getKey(), e);
			}
		}
	}

	@Override
	public @NotNull CompletableFuture<Void> reload(SharedState sharedState, Executor backgroundExecutor, PreparationBarrier barrier, Executor gameExecutor) {
        ResourceManager manager = sharedState.resourceManager();
        return CompletableFuture.runAsync(() -> onResourceManagerReload(manager), backgroundExecutor).thenCompose(barrier::wait);
	}
}
