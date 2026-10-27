package com.example.hud;

/** Which HUD pieces are shown. Kept in memory only, so they reset when the game restarts. */
public final class HudConfig {
    private HudConfig() {}

    public static boolean keystrokes = true;
    public static boolean info = true;
    public static boolean ping = true;
    public static boolean armor = true;
    public static boolean potions = true;
    public static boolean food = true;
}
