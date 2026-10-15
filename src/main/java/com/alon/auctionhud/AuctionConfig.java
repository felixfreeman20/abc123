package com.alon.auctionhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AuctionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("auctionhud.json");

    public boolean hudVisible = true;
    /** Empty = use your Minecraft username. */
    public String ign = "";
    public long minBid = 100_000_000L;
    public int durationSeconds = 60;
    /** ICON, TEXT or BOTH */
    public String displayMode = "BOTH";
    public int x = 6;
    public int y = 6;
    public List<String> items = new ArrayList<>();

    public static AuctionConfig load() {
        try {
            if (Files.exists(PATH)) {
                AuctionConfig c = GSON.fromJson(Files.readString(PATH), AuctionConfig.class);
                if (c != null) {
                    if (c.items == null) c.items = new ArrayList<>();
                    return c;
                }
            }
        } catch (Exception ignored) {
        }
        return new AuctionConfig();
    }

    public void save() {
        try {
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
