package com.example.util;

import net.minecraft.entity.EntityType;

public final class ThreatProfiles {
    private ThreatProfiles() {}

    public static double extraReach(EntityType<?> type) {
        if (type == EntityType.WARDEN) return 0.80;
        if (type == EntityType.RAVAGER) return 0.45;
        return 0.30;
    }

    public static float kbH(EntityType<?> type) {
        if (type == EntityType.IRON_GOLEM) return 0.85f;
        if (type == EntityType.RAVAGER) return 0.70f;
        if (type == EntityType.WARDEN) return 0.65f;
        return 0.45f;
    }

    public static double kbV(EntityType<?> type) {
        if (type == EntityType.IRON_GOLEM) return 0.30;
        if (type == EntityType.RAVAGER) return 0.12;
        return 0.0;
    }

    public static float contactDamage(EntityType<?> type) {
        if (type == EntityType.IRON_GOLEM) return 7f;
        if (type == EntityType.RAVAGER) return 6f;
        if (type == EntityType.WARDEN) return 8f;
        return 3f;
    }
}
