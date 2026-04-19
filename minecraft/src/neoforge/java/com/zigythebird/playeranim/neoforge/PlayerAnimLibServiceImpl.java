package com.zigythebird.playeranim.neoforge;

import com.zigythebird.playeranim.PlayerAnimLibService;
import net.neoforged.fml.loading.FMLLoader;

public final class PlayerAnimLibServiceImpl implements PlayerAnimLibService {
    @Override
    public boolean isServiceActive() {
        try {
            Class.forName("net.neoforged.fml.loading.FMLLoader");
            return true;
        } catch (Exception th) {
            return false;
        }
    }

    @Override
    public boolean isModLoaded(String id) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(id) != null;
    }
}
