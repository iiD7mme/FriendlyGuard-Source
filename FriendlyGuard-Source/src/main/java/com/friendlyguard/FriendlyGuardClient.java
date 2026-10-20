package com.friendlyguard;

import com.friendlyguard.config.FriendlyGuardConfig;
import com.friendlyguard.gui.FriendlyGuardConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class FriendlyGuardClient implements ClientModInitializer {
    public static final String MOD_ID = "friendlyguard";
    private static KeyBinding openMenuKeyBinding;
    private static KeyBinding toggleProtectionKeyBinding;

    public static KeyBinding getOpenMenuKeyBinding() {
        return openMenuKeyBinding;
    }

    public static KeyBinding getToggleProtectionKeyBinding() {
        return toggleProtectionKeyBinding;
    }

    @Override
    public void onInitializeClient() {
        // Load configuration
        FriendlyGuardConfig.getInstance();

        // Register attack cancellation callback
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && player == MinecraftClient.getInstance().player) {
                FriendlyGuardConfig config = FriendlyGuardConfig.getInstance();
                if (config.isProtectionEnabled() && entity instanceof PlayerEntity targetPlayer) {
                    String name = targetPlayer.getGameProfile().getName();
                    if (config.isProtected(name)) {
                        if (config.isShowWarningMessage()) {
                            player.sendMessage(
                                Text.literal("You cannot hit " + name + "! (Protected)")
                                    .formatted(Formatting.RED),
                                true
                            );
                        }
                        return ActionResult.FAIL;
                    }
                }
            }
            return ActionResult.PASS;
        });

        // Register KeyBinding to open menu (Default: O)
        openMenuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.friendlyguard.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.friendlyguard"
        ));

        // Register KeyBinding to toggle protection ON/OFF (Default: P)
        toggleProtectionKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.friendlyguard.toggle_protection",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "category.friendlyguard"
        ));

        // Listen for key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKeyBinding.wasPressed()) {
                client.setScreen(FriendlyGuardConfigScreen.create(client.currentScreen));
            }

            while (toggleProtectionKeyBinding.wasPressed()) {
                FriendlyGuardConfig config = FriendlyGuardConfig.getInstance();
                boolean newState = !config.isProtectionEnabled();
                config.setProtectionEnabled(newState);

                if (client.player != null) {
                    String status = newState ? "Enabled" : "Disabled";
                    Formatting color = newState ? Formatting.GREEN : Formatting.RED;
                    client.player.sendMessage(
                        Text.literal("[FriendlyGuard] Protection: " + status).formatted(color),
                        true
                    );
                }
            }
        });

        // Register client commands: /friendlyguard and /fg
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("friendlyguard")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.send(() -> client.setScreen(FriendlyGuardConfigScreen.create(client.currentScreen)));
                    return 1;
                })
            );
            dispatcher.register(ClientCommandManager.literal("fg")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.send(() -> client.setScreen(FriendlyGuardConfigScreen.create(client.currentScreen)));
                    return 1;
                })
            );
        });
    }
}
