package com.zigythebird.playeranim;

import com.google.j2objc.annotations.J2ObjCIncompatible;
import org.redlance.common.services.AdvancedService;
import org.redlance.common.services.ServiceUtils;

@J2ObjCIncompatible
public interface PlayerAnimLibService extends AdvancedService {
    PlayerAnimLibService INSTANCE = ServiceUtils.loadService(PlayerAnimLibService.class);

    boolean isModLoaded(String id);
}
