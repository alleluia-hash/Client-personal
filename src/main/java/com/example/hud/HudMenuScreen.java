package com.example.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** The HUD settings menu, opened with K. Only switches visual pieces on and off. */
public class HudMenuScreen extends Screen {
    public HudMenuScreen() {
        super(Text.literal("HUD Settings"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = 50;
        addToggle(x, y, "Keystrokes", () -> HudConfig.keystrokes, v -> HudConfig.keystrokes = v); y += 24;
        addToggle(x, y, "FPS and coordinates", () -> HudConfig.info, v -> HudConfig.info = v); y += 24;
        addToggle(x, y, "Ping", () -> HudConfig.ping, v -> HudConfig.ping = v); y += 24;
        addToggle(x, y, "Armor durability", () -> HudConfig.armor, v -> HudConfig.armor = v); y += 24;
        addToggle(x, y, "Potion timers", () -> HudConfig.potions, v -> HudConfig.potions = v); y += 24;
        addToggle(x, y, "Food and saturation", () -> HudConfig.food, v -> HudConfig.food = v); y += 34;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(x, y, 200, 20).build());
    }

    private interface Getter { boolean get(); }
    private interface Setter { void set(boolean v); }

    private void addToggle(int x, int y, String label, Getter get, Setter set) {
        addDrawableChild(ButtonWidget.builder(toggleText(label, get.get()), b -> {
            set.set(!get.get());
            b.setMessage(toggleText(label, get.get()));
        }).dimensions(x, y, 200, 20).build());
    }

    private static Text toggleText(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
