package com.zigythebird.playeranim.neoforge;

import com.zigythebird.playeranim.PlayerAnimLibMod;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.commands.PlayerAnimCommands;
import com.zigythebird.playeranim.neoforge.event.PlayerAnimationRegisterEvent;
import com.zigythebird.playeranimcore.event.MolangEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

@Mod(value = PlayerAnimLibModNeo.MOD_ID)
public final class PlayerAnimLibModNeo extends PlayerAnimLibMod {
    public PlayerAnimLibModNeo(IEventBus bus) {
        bus.addListener(this::onAddClientReloadListeners);
        if (!FMLLoader.getCurrent().isProduction() || ModList.get().getModFileById(PlayerAnimLibMod.MOD_ID).versionString().contains("dev"))
            NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        bus.addListener(this::onClientInit);
    }

    public void onAddClientReloadListeners(@NotNull AddClientReloadListenersEvent event) {
        event.addListener(PlayerAnimResources.KEY, new PlayerAnimResources());
    }

    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        PlayerAnimCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    public void onClientInit(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            super.init();

            PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, manager) ->
                    NeoForge.EVENT_BUS.post(new PlayerAnimationRegisterEvent(player, manager))
            );
            MolangEvent.MOLANG_EVENT.register((controller, builder, q) ->
                    NeoForge.EVENT_BUS.post(new com.zigythebird.playeranim.neoforge.event.MolangEvent(controller, builder, q))
            );
        });
    }
}
