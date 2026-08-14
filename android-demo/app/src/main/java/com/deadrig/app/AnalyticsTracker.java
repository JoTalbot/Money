package com.deadrig.app;

import android.content.SharedPreferences;

/**
 * Локальная privacy-first аналитика. События считаются только на устройстве и никуда
 * не отправляются. Класс служит стабильным адаптером для будущего Firebase/другого SDK.
 */
public final class AnalyticsTracker {
    private static final String ENABLED = "analytics_local_enabled";
    private final SharedPreferences prefs;

    public AnalyticsTracker(SharedPreferences prefs) { this.prefs = prefs; }

    public boolean isEnabled() { return prefs.getBoolean(ENABLED, true); }
    public void setEnabled(boolean enabled) { prefs.edit().putBoolean(ENABLED, enabled).apply(); }

    public void track(String event) {
        if (!isEnabled() || event == null || event.length() == 0) return;
        String safe = event.replaceAll("[^a-zA-Z0-9_]", "_");
        long count = prefs.getLong("analytics_event_" + safe, 0) + 1;
        prefs.edit()
                .putLong("analytics_event_" + safe, count)
                .putLong("analytics_total", prefs.getLong("analytics_total", 0) + 1)
                .putString("analytics_last", safe)
                .apply();
    }

    public long totalEvents() { return prefs.getLong("analytics_total", 0); }
    public String lastEvent() { return prefs.getString("analytics_last", "—"); }
    public void clear() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) if (key.startsWith("analytics_")) editor.remove(key);
        editor.apply();
    }
}
