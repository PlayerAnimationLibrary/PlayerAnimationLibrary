package com.zigythebird.playeranim;

import org.redlance.common.services.AdvancedService;
import org.redlance.common.services.ServiceUtils;

public interface PlayerAnimLibService extends AdvancedService {
    PlayerAnimLibService INSTANCE = ServiceUtils.loadService(PlayerAnimLibService.class);

    boolean isModLoaded(String id);
}
