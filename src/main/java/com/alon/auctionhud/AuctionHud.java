package com.alon.auctionhud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class AuctionHud {
    private static final int PURPLE = 0xFFD13CFF;
    private static final int PURPLE_DIM = 0xFF4A1A5C;
    private static final int BG = 0xB0140A1C;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int GOLD = 0xFFFFD34D;
    private static final int PAD = 6;
    private static final int RING_R = 22;

    private AuctionHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        AuctionConfig cfg = AuctionHudMod.config;
        AuctionState st = AuctionHudMod.state;
        Minecraft mc = Minecraft.getInstance();
        if (!cfg.hudVisible || mc.options.hideGui || mc.player == null) return;

        Font font = mc.font;
        String ign = cfg.ign.isEmpty() ? mc.getUser().getName() : cfg.ign;
        String mode = cfg.displayMode;
        List<ItemStack> items = AuctionHudMod.itemStacks;

        String ignLine = "IGN: " + ign;
        String minLine = "Min: " + Money.format(cfg.minBid);
        String topLine = "Top bid: " + (st.topBidder == null ? "-" : st.topBidder + " - " + Money.format(st.topBid));
        String endLine = null;
        if (st.finished) {
            endLine = st.winner == null ? "Ended: no bids"
                    : "Winner: " + st.winner + " - " + Money.format(st.winnerBid);
        }

        // ---- sizing ----
        int lineH = 11;
        int w = Math.max(150, Math.max(font.width(ignLine) + 2 * PAD,
                Math.max(font.width(topLine), endLine == null ? 0 : font.width(endLine)) + 2 * PAD));
        int rowH = 0;
        int iconsPerRow = Math.max(1, (w - 2 * PAD) / 18);
        if (!items.isEmpty()) {
            switch (mode) {
                case "ICON" -> rowH = ((items.size() + iconsPerRow - 1) / iconsPerRow) * 18;
                case "TEXT" -> rowH = items.size() * lineH;
                default -> rowH = items.size() * 18;
            }
            for (ItemStack s : items) {
                int tw = font.width(s.getHoverName().getString()) + (mode.equals("BOTH") ? 22 : 0) + 2 * PAD;
                if (!mode.equals("ICON")) w = Math.max(w, tw);
            }
        }

        int h = PAD + lineH            // ign
                + 2 * RING_R + 8       // ring block
                + lineH * 2            // min + top
                + (endLine != null ? lineH : 0)
                + (rowH > 0 ? rowH + 6 : 0)
                + PAD;

        int x = cfg.x, y = cfg.y;

        // ---- panel ----
        g.fill(x, y, x + w, y + h, BG);
        g.fill(x, y, x + w, y + 1, PURPLE);
        g.fill(x, y + h - 1, x + w, y + h, PURPLE);
        g.fill(x, y, x + 1, y + h, PURPLE);
        g.fill(x + w - 1, y, x + w, y + h, PURPLE);

        int cy = y + PAD;
        g.text(font, ignLine, x + PAD, cy, PURPLE, false);
        cy += lineH + 2;

        // ---- timer ring ----
        int ringCx = x + w - PAD - RING_R - 4;
        int ringCy = cy + RING_R + 2;
        g.text(font, "Timer", x + PAD, ringCy - 4, PURPLE, false);
        double frac = 0;
        long remMs = st.remainingMillis();
        if (st.running && st.durationSeconds > 0) frac = Math.min(1.0, remMs / (st.durationSeconds * 1000.0));
        drawRing(g, ringCx, ringCy, frac);
        long secs = st.running ? (remMs + 999) / 1000 : (st.finished ? 0 : cfg.durationSeconds);
        String time = String.format("%02d:%02d", secs / 60, secs % 60);
        g.text(font, time, ringCx - font.width(time) / 2, ringCy - 4, WHITE, true);
        cy += 2 * RING_R + 6;

        g.text(font, minLine, x + PAD, cy, PURPLE, false);
        cy += lineH;
        g.text(font, topLine, x + PAD, cy, PURPLE, false);
        cy += lineH;
        if (endLine != null) {
            g.text(font, endLine, x + PAD, cy, st.winner == null ? GOLD : GREEN, false);
            cy += lineH;
        }

        // ---- items ----
        if (!items.isEmpty()) {
            cy += 4;
            switch (mode) {
                case "ICON" -> {
                    for (int i = 0; i < items.size(); i++) {
                        int ix = x + PAD + (i % iconsPerRow) * 18;
                        int iy = cy + (i / iconsPerRow) * 18;
                        g.item(items.get(i), ix, iy);
                    }
                }
                case "TEXT" -> {
                    for (int i = 0; i < items.size(); i++) {
                        g.text(font, items.get(i).getHoverName().getString(), x + PAD, cy + i * lineH, WHITE, false);
                    }
                }
                default -> {
                    for (int i = 0; i < items.size(); i++) {
                        int iy = cy + i * 18;
                        g.item(items.get(i), x + PAD, iy);
                        g.text(font, items.get(i).getHoverName().getString(), x + PAD + 22, iy + 4, WHITE, false);
                    }
                }
            }
        }
    }

    /** Clockwise ring starting at 12 o'clock; `frac` of it is lit. */
    private static void drawRing(GuiGraphicsExtractor g, int cx, int cy, double frac) {
        int steps = 120;
        int lit = (int) Math.round(steps * frac);
        for (int i = 0; i < steps; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / steps;
            double cos = Math.cos(a), sin = Math.sin(a);
            int color = i < lit ? PURPLE : PURPLE_DIM;
            for (int r = RING_R - 4; r <= RING_R; r += 2) {
                int px = (int) Math.round(cx + cos * r);
                int py = (int) Math.round(cy + sin * r);
                g.fill(px - 1, py - 1, px + 1, py + 1, color);
            }
        }
    }
}
