package com.example.hud;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/** Visual-only HUD: keystrokes, FPS and coordinates, ping, armor durability, potion timers, food. */
public class HudClient implements ClientModInitializer {
    private static final int KEY_SIZE = 22;
    private static final int KEY_GAP = 3;
    private static final int COLOR_IDLE = 0x90000000;
    private static final int COLOR_PRESSED = 0xE0FFFFFF;
    private static final int TEXT_IDLE = 0xFFFFFFFF;
    private static final int TEXT_PRESSED = 0xFF000000;

    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hud_mod.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.hud_mod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HudMenuScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register(HudClient::render);
    }

    private static void text(DrawContext ctx, MinecraftClient client, String text, int x, int y, int color) {
        ctx.drawText(client.textRenderer, text, x, y, color, false);
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null || client.textRenderer == null) {
            return;
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        if (HudConfig.info) drawInfo(ctx, client);
        if (HudConfig.ping) drawPing(ctx, client);
        if (HudConfig.armor) drawArmor(ctx, client, screenH);
        if (HudConfig.potions) drawPotions(ctx, client, screenW);
        if (HudConfig.food) drawFood(ctx, client, screenW, screenH);
        if (HudConfig.keystrokes) drawKeystrokes(ctx, client, screenW, screenH);
    }

    private static void drawInfo(DrawContext ctx, MinecraftClient client) {
        BlockPos pos = client.player.getBlockPos();
        text(ctx, client, "FPS: " + client.getCurrentFps(), 4, 4, 0xFFFFFFFF);
        text(ctx, client, String.format("XYZ: %d %d %d", pos.getX(), pos.getY(), pos.getZ()), 4, 14, 0xFFFFFFFF);
    }

    private static void drawPing(DrawContext ctx, MinecraftClient client) {
        PlayerListEntry entry = client.getNetworkHandler() == null ? null
            : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        String label = entry == null ? "Ping: -" : "Ping: " + entry.getLatency() + " ms";
        text(ctx, client, label, 4, 24, 0xFFFFFFFF);
    }

    private static void drawArmor(DrawContext ctx, MinecraftClient client, int screenH) {
        int y = screenH - 70;
        for (ItemStack stack : client.player.getInventory().armor) {
            if (stack.isEmpty() || !stack.isDamageable()) {
                continue;
            }
            int left = stack.getMaxDamage() - stack.getDamage();
            text(ctx, client, left + " / " + stack.getMaxDamage(), 4, y, 0xFFFFFFFF);
            y -= 10;
        }
    }

    private static void drawPotions(DrawContext ctx, MinecraftClient client, int screenW) {
        int y = 4;
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            String name = effect.getEffectType().getName().getString();
            int seconds = effect.getDuration() / 20;
            String label = name + " " + (seconds / 60) + ":" + String.format("%02d", seconds % 60);
            int w = client.textRenderer.getWidth(label);
            text(ctx, client, label, screenW - w - 4, y, 0xFFFFFFFF);
            y += 10;
        }
    }

    private static void drawFood(DrawContext ctx, MinecraftClient client, int screenW, int screenH) {
        String label = String.format("Food: %d  Saturation: %.1f",
            client.player.getHungerManager().getFoodLevel(),
            client.player.getHungerManager().getSaturationLevel());
        int w = client.textRenderer.getWidth(label);
        text(ctx, client, label, (screenW - w) / 2, screenH - 50, 0xFFFFFFFF);
    }

    private static void drawKeystrokes(DrawContext ctx, MinecraftClient client, int screenW, int screenH) {
        int baseX = screenW - (KEY_SIZE * 3 + KEY_GAP * 2) - 10;
        int baseY = screenH - (KEY_SIZE * 3 + KEY_GAP * 2) - 40;

        drawKey(ctx, client, "W", client.options.forwardKey.isPressed(), baseX + KEY_SIZE + KEY_GAP, baseY, KEY_SIZE, KEY_SIZE);
        drawKey(ctx, client, "A", client.options.leftKey.isPressed(), baseX, baseY + KEY_SIZE + KEY_GAP, KEY_SIZE, KEY_SIZE);
        drawKey(ctx, client, "S", client.options.backKey.isPressed(), baseX + KEY_SIZE + KEY_GAP, baseY + KEY_SIZE + KEY_GAP, KEY_SIZE, KEY_SIZE);
        drawKey(ctx, client, "D", client.options.rightKey.isPressed(), baseX + (KEY_SIZE + KEY_GAP) * 2, baseY + KEY_SIZE + KEY_GAP, KEY_SIZE, KEY_SIZE);

        int row = baseY + (KEY_SIZE + KEY_GAP) * 2;
        int wide = KEY_SIZE * 3 / 2;
        drawKey(ctx, client, "LMB", client.options.attackKey.isPressed(), baseX, row, wide, KEY_SIZE);
        drawKey(ctx, client, "RMB", client.options.useKey.isPressed(), baseX + wide + KEY_GAP, row, wide, KEY_SIZE);
        drawKey(ctx, client, "_", client.options.jumpKey.isPressed(), baseX + (wide + KEY_GAP) * 2, row, wide, KEY_SIZE);
    }

    private static void drawKey(DrawContext ctx, MinecraftClient client, String label, boolean pressed,
                                int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, pressed ? COLOR_PRESSED : COLOR_IDLE);
        int tx = x + (w - client.textRenderer.getWidth(label)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        text(ctx, client, label, tx, ty, pressed ? TEXT_PRESSED : TEXT_IDLE);
    }
}
