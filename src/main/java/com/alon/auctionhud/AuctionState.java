package com.alon.auctionhud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionState {
    /** Anchored so public chat lines like "Name » X paid you $1B" can't spoof a bid. */
    private static final Pattern PAID = Pattern.compile(
            "^\\s*(\\w{2,16}) paid you\\s*\\$?\\s*(\\d[\\d,]*(?:\\.\\d+)?)\\s*([kKmMbBtT])?\\b");

    public boolean running;
    public long endMillis;
    public int durationSeconds;
    public String topBidder;
    public long topBid;
    public boolean finished;
    public String winner;
    public long winnerBid;

    public void start(int seconds) {
        durationSeconds = seconds;
        endMillis = System.currentTimeMillis() + seconds * 1000L;
        running = true;
        finished = false;
        topBidder = null;
        topBid = 0;
        winner = null;
        winnerBid = 0;
    }

    public void stop() {
        running = false;
    }

    public void reset() {
        running = false;
        finished = false;
        topBidder = null;
        topBid = 0;
        winner = null;
        winnerBid = 0;
    }

    public long remainingMillis() {
        return running ? Math.max(0, endMillis - System.currentTimeMillis()) : 0;
    }

    public void tick() {
        if (running && System.currentTimeMillis() >= endMillis) {
            running = false;
            finished = true;
            winner = topBidder;
            winnerBid = topBid;
        }
    }

    /** Called for every system chat line. */
    public void onChat(String line, long minBid) {
        if (!running) return;
        Matcher m = PAID.matcher(line);
        if (!m.find()) return;
        long amount = Money.parse(m.group(2), m.group(3));
        if (amount < 0 || amount < minBid) return;
        if (amount > topBid) {
            topBid = amount;
            topBidder = m.group(1);
        }
    }
}
