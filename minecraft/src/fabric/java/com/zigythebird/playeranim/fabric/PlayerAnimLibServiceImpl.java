package com.zigythebird.playeranim.fabric;

import com.zigythebird.playeranim.PlayerAnimLibService;
import net.fabricmc.loader.api.FabricLoader;

public final class PlayerAnimLibServiceImpl implements PlayerAnimLibService {
    @Override
    public boolean isServiceActive() {
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return true;
        } catch (Exception th) {
            return false;
        }
    }

    @Override
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
