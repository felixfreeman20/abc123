package com.alon.auctionhud;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AuctionHudMod implements ClientModInitializer {
    public static final String MOD_ID = "auctionhud";
    public static AuctionConfig config;
    public static final AuctionState state = new AuctionState();
    public static List<ItemStack> itemStacks = new ArrayList<>();

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        config = AuctionConfig.load();
        refreshItems();

        // Toggle keybind (rebind in Controls -> Auction HUD)
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.auctionhud.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggleKey.consumeClick()) {
                config.hudVisible = !config.hudVisible;
                config.save();
            }
            state.tick();
        });

        // Incoming payments arrive as system messages
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            state.onChat(message.getString(), config.minBid);
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), AuctionHud::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));
    }

    public static void refreshItems() {
        List<ItemStack> out = new ArrayList<>();
        for (String s : config.items) {
            Identifier id = Identifier.tryParse(s);
            if (id == null) continue;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != Items.AIR) out.add(new ItemStack(item));
        }
        itemStacks = out;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> lit(String n) {
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal(n);
    }

    private static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> arg(
            String n, com.mojang.brigadier.arguments.ArgumentType<T> t) {
        return RequiredArgumentBuilder.<FabricClientCommandSource, T>argument(n, t);
    }

    private static void msg(FabricClientCommandSource src, String s) {
        src.sendFeedback(Component.literal("[Auction] " + s));
    }

    private static void registerCommands(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> d) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = lit("auction");

        // /auction toggle
        root.then(lit("toggle").executes(c -> {
            config.hudVisible = !config.hudVisible;
            config.save();
            msg(c.getSource(), "HUD " + (config.hudVisible ? "on" : "off"));
            return 1;
        }));

        // /auction start [seconds]
        root.then(lit("start")
                .executes(c -> {
                    state.start(config.durationSeconds);
                    msg(c.getSource(), "Started (" + config.durationSeconds + "s)");
                    return 1;
                })
                .then(arg("seconds", IntegerArgumentType.integer(1, 86400)).executes(c -> {
                    int s = IntegerArgumentType.getInteger(c, "seconds");
                    state.start(s);
                    msg(c.getSource(), "Started (" + s + "s)");
                    return 1;
                })));

        root.then(lit("stop").executes(c -> {
            state.stop();
            msg(c.getSource(), "Stopped");
            return 1;
        }));

        root.then(lit("reset").executes(c -> {
            state.reset();
            msg(c.getSource(), "Cleared");
            return 1;
        }));

        // /auction timer <seconds>  (default length for /auction start)
        root.then(lit("timer").then(arg("seconds", IntegerArgumentType.integer(1, 86400)).executes(c -> {
            config.durationSeconds = IntegerArgumentType.getInteger(c, "seconds");
            config.save();
            msg(c.getSource(), "Timer length = " + config.durationSeconds + "s");
            return 1;
        })));

        // /auction min <amount>   e.g. 100m, 20k, 1.5b
        root.then(lit("min").then(arg("amount", StringArgumentType.word()).executes(c -> {
            long v = Money.parseInput(StringArgumentType.getString(c, "amount"));
            if (v < 0) {
                msg(c.getSource(), "Invalid amount. Examples: 500k, 100m, 1.5b");
                return 0;
            }
            config.minBid = v;
            config.save();
            msg(c.getSource(), "Min bid = " + Money.format(v));
            return 1;
        })));

        // /auction ign <name>
        root.then(lit("ign").then(arg("name", StringArgumentType.word()).executes(c -> {
            config.ign = StringArgumentType.getString(c, "name");
            config.save();
            msg(c.getSource(), "IGN = " + config.ign);
            return 1;
        })));

        // /auction display <icon|text|both>
        root.then(lit("display").then(arg("mode", StringArgumentType.word()).executes(c -> {
            String m = StringArgumentType.getString(c, "mode").toUpperCase();
            if (!m.equals("ICON") && !m.equals("TEXT") && !m.equals("BOTH")) {
                msg(c.getSource(), "Use: icon, text or both");
                return 0;
            }
            config.displayMode = m;
            config.save();
            msg(c.getSource(), "Display = " + m.toLowerCase());
            return 1;
        })));

        // /auction pos <x> <y>
        root.then(lit("pos").then(arg("x", IntegerArgumentType.integer(0))
                .then(arg("y", IntegerArgumentType.integer(0)).executes(c -> {
                    config.x = IntegerArgumentType.getInteger(c, "x");
                    config.y = IntegerArgumentType.getInteger(c, "y");
                    config.save();
                    return 1;
                }))));

        // /auction item add [id] | remove <id> | clear
        LiteralArgumentBuilder<FabricClientCommandSource> item = lit("item");
        item.then(lit("add")
                .executes(c -> { // held item
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.player.getMainHandItem().isEmpty()) {
                        msg(c.getSource(), "Hold an item or give an id: /auction item add diamond");
                        return 0;
                    }
                    Identifier id = BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem());
                    return addItem(c.getSource(), id.toString());
                })
                .then(arg("id", StringArgumentType.word()).executes(c ->
                        addItem(c.getSource(), StringArgumentType.getString(c, "id")))));
        item.then(lit("remove").then(arg("id", StringArgumentType.word()).executes(c -> {
            Identifier id = Identifier.tryParse(StringArgumentType.getString(c, "id"));
            boolean removed = id != null && config.items.remove(id.toString());
            config.save();
            refreshItems();
            msg(c.getSource(), removed ? "Removed" : "Not in list");
            return removed ? 1 : 0;
        })));
        item.then(lit("clear").executes(c -> {
            config.items.clear();
            config.save();
            refreshItems();
            msg(c.getSource(), "Items cleared");
            return 1;
        }));
        root.then(item);

        d.register(root);
    }

    private static int addItem(FabricClientCommandSource src, String raw) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null || BuiltInRegistries.ITEM.getValue(id) == Items.AIR) {
            msg(src, "Unknown item: " + raw);
            return 0;
        }
        if (!config.items.contains(id.toString())) config.items.add(id.toString());
        config.save();
        refreshItems();
        msg(src, "Added " + id);
        return 1;
    }
}
