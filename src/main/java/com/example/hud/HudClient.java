package com.example.hud;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
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

    private static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null || client.textRenderer == null) {
            return;
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        if (HudConfig.info) drawInfo(client, matrices);
        if (HudConfig.ping) drawPing(client, matrices);
        if (HudConfig.armor) drawArmor(client, matrices, screenH);
        if (HudConfig.potions) drawPotions(client, matrices, screenW);
        if (HudConfig.food) drawFood(client, matrices, screenW, screenH);
        if (HudConfig.keystrokes) drawKeystrokes(client, matrices, screenW, screenH);
    }

    private static void drawInfo(MinecraftClient client, MatrixStack matrices) {
        BlockPos pos = client.player.getBlockPos();
        client.textRenderer.draw(matrices, "FPS: " + client.getCurrentFps(), 4, 4, 0xFFFFFFFF);
        client.textRenderer.draw(matrices,
            String.format("XYZ: %d %d %d", pos.getX(), pos.getY(), pos.getZ()), 4, 14, 0xFFFFFFFF);
    }

    private static void drawPing(MinecraftClient client, MatrixStack matrices) {
        PlayerListEntry entry = client.getNetworkHandler() == null ? null
            : client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        String text = entry == null ? "Ping: -" : "Ping: " + entry.getLatency() + " ms";
        client.textRenderer.draw(matrices, text, 4, 24, 0xFFFFFFFF);
    }

    private static void drawArmor(MinecraftClient client, MatrixStack matrices, int screenH) {
        int y = screenH - 70;
        for (ItemStack stack : client.player.getInventory().armor) {
            if (stack.isEmpty() || !stack.isDamageable()) {
                continue;
            }
            int left = stack.getMaxDamage() - stack.getDamage();
            String text = left + " / " + stack.getMaxDamage();
            client.textRenderer.draw(matrices, text, 4, y, 0xFFFFFFFF);
            y -= 10;
        }
    }

    private static void drawPotions(MinecraftClient client, MatrixStack matrices, int screenW) {
        int y = 4;
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            String name = effect.getEffectType().getName().getString();
            int seconds = effect.getDuration() / 20;
            String text = name + " " + (seconds / 60) + ":" + String.format("%02d", seconds % 60);
            int w = client.textRenderer.getWidth(text);
            client.textRenderer.draw(matrices, text, screenW - w - 4, y, 0xFFFFFFFF);
            y += 10;
        }
    }

    private static void drawFood(MinecraftClient client, MatrixStack matrices, int screenW, int screenH) {
        String text = String.format("Food: %d  Saturation: %.1f",
            client.player.getHungerManager().getFoodLevel(),
            client.player.getHungerManager().getSaturationLevel());
        int w = client.textRenderer.getWidth(text);
        client.textRenderer.draw(matrices, text, (screenW - w) / 2, screenH - 50, 0xFFFFFFFF);
    }

    private static void drawKeystrokes(MinecraftClient client, MatrixStack matrices, int screenW, int screenH) {
        int baseX = screenW - (KEY_SIZE * 3 + KEY_GAP * 2) - 10;
        int baseY = screenH - (KEY_SIZE * 3 + KEY_GAP * 2) - 40;

        drawKey(client, matrices, "W", client.options.forwardKey.isPressed(), baseX + KEY_SIZE + KEY_GAP, baseY);
        drawKey(client, matrices, "A", client.options.leftKey.isPressed(), baseX, baseY + KEY_SIZE + KEY_GAP);
        drawKey(client, matrices, "S", client.options.backKey.isPressed(), baseX + KEY_SIZE + KEY_GAP, baseY + KEY_SIZE + KEY_GAP);
        drawKey(client, matrices, "D", client.options.rightKey.isPressed(), baseX + (KEY_SIZE + KEY_GAP) * 2, baseY + KEY_SIZE + KEY_GAP);

        int row = baseY + (KEY_SIZE + KEY_GAP) * 2;
        int wide = KEY_SIZE * 3 / 2;
        drawKey(client, matrices, "LMB", client.options.attackKey.isPressed(), baseX, row, wide, KEY_SIZE);
        drawKey(client, matrices, "RMB", client.options.useKey.isPressed(), baseX + wide + KEY_GAP, row, wide, KEY_SIZE);
        drawKey(client, matrices, "_", client.options.jumpKey.isPressed(), baseX + (wide + KEY_GAP) * 2, row, wide, KEY_SIZE);
    }

    private static void drawKey(MinecraftClient client, MatrixStack matrices, String label, boolean pressed, int x, int y) {
        drawKey(client, matrices, label, pressed, x, y, KEY_SIZE, KEY_SIZE);
    }

    private static void drawKey(MinecraftClient client, MatrixStack matrices, String label, boolean pressed,
                                int x, int y, int w, int h) {
        DrawableHelper.fill(matrices, x, y, x + w, y + h, pressed ? COLOR_PRESSED : COLOR_IDLE);
        int tx = x + (w - client.textRenderer.getWidth(label)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        client.textRenderer.draw(matrices, label, tx, ty, pressed ? TEXT_PRESSED : TEXT_IDLE);
    }
}
