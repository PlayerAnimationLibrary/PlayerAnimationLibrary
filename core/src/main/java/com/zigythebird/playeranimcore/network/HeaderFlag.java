package com.zigythebird.playeranimcore.network;

enum HeaderFlag {
    SHOULD_PLAY_AGAIN,
    HOLD_ON_LAST_FRAME,
    PLAYER_ANIMATOR,
    APPLY_BEND,
    EASE_BEFORE,
    HAS_BEGIN_TICK,
    HAS_END_TICK;

    final int mask = 1 << ordinal();

    boolean test(int flags) {
        return (flags & mask) != 0;
    }

    int set(int flags, boolean condition) {
        return condition ? flags | mask : flags;
    }
}
