package com.zigythebird.playeranim.fabric;

import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.commands.PlayerAnimCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;

public final class PlayerAnimLibModFabric extends PlayerAnimLibMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(PlayerAnimResources.KEY, new PlayerAnimResources());
        if (FabricLoader.getInstance().isDevelopmentEnvironment() || FabricLoader.getInstance().getModContainer(PlayerAnimLibMod.MOD_ID).get().getMetadata().getVersion().getFriendlyString().contains("dev"))
            ClientCommandRegistrationCallback.EVENT.register(PlayerAnimCommands::register);

        super.init();
    }
}
