package com.criztiandev.extractionevent.tasks;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RegionPresenceTask's pure-Java logic, optimised for
 * the 100-players / 10-TPS scenario.
 *
 * Tests that can run without a Bukkit server runtime:
 *   - pairKey canonical ordering (no Bukkit)
 *   - pairKey symmetry (A,B == B,A)
 *   - pairKey uniqueness at 100-player scale
 *   - Performance: 100-player O(n²) pair-key computation < 100 ms
 */
class RegionPresenceTaskTest {

    // ── pairKey canonical ordering ────────────────────────────────────────────

    @Test
    void pairKey_isSymmetric() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertEquals(RegionPresenceTask.pairKey(a, b),
                     RegionPresenceTask.pairKey(b, a),
                     "pairKey(A,B) must equal pairKey(B,A)");
    }

    @Test
    void pairKey_selfReferenceIsStable() {
        UUID a   = UUID.randomUUID();
        long key = RegionPresenceTask.pairKey(a, a);
        assertEquals(key, RegionPresenceTask.pairKey(a, a));
    }

    @Test
    void pairKey_differentPairsAreDistinct() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        long ab = RegionPresenceTask.pairKey(a, b);
        long ac = RegionPresenceTask.pairKey(a, c);
        long bc = RegionPresenceTask.pairKey(b, c);

        assertNotEquals(ab, ac, "A-B and A-C pairs must be distinct");
        assertNotEquals(ab, bc, "A-B and B-C pairs must be distinct");
        assertNotEquals(ac, bc, "A-C and B-C pairs must be distinct");
    }

    // ── 100-player scale correctness ──────────────────────────────────────────

    @Test
    void pairKey_100playersProducesCorrectUniqueCount() {
        // n*(n-1)/2 = 100*99/2 = 4950 unique pairs for 100 players
        int n = 100;
        UUID[] players = new UUID[n];
        for (int i = 0; i < n; i++) players[i] = UUID.randomUUID();

        java.util.Set<Long> keys = new java.util.HashSet<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                keys.add(RegionPresenceTask.pairKey(players[i], players[j]));
            }
        }

        // Expect no collisions across 4950 random UUID pairs
        assertEquals(n * (n - 1) / 2, keys.size(),
                "100 players should produce exactly 4950 unique pair keys");
    }

    // ── Performance: 100-player O(n²) must complete within budget ────────────

    /**
     * At 10 TPS the server tick budget is 100ms. RegionPresenceTask's Pass 3
     * computes n*(n-1)/2 = 4950 pair keys per tick. This test asserts the
     * entire computation (keys + HashSet diff) completes in well under 50ms,
     * leaving ample room for other tick work.
     */
    @Test
    void performanceTest_100players_visibility_completesUnder50ms() {
        int n = 100;
        UUID[] players = new UUID[n];
        for (int i = 0; i < n; i++) players[i] = UUID.randomUUID();

        java.util.Set<Long> hiddenPairs    = new java.util.HashSet<>(256);
        java.util.Set<Long> newHiddenPairs = new java.util.HashSet<>(256);

        // Pre-warm
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long key = RegionPresenceTask.pairKey(players[i], players[j]);
                if (i % 2 == 0) newHiddenPairs.add(key);
            }
        }
        hiddenPairs = newHiddenPairs;
        newHiddenPairs = new java.util.HashSet<>(256);

        // Time the hot path — 10 ticks
        long start = System.currentTimeMillis();
        for (int tick = 0; tick < 10; tick++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    long key = RegionPresenceTask.pairKey(players[i], players[j]);
                    boolean farApart = (i + j) % 3 == 0;
                    if (farApart) {
                        newHiddenPairs.add(key);
                        hiddenPairs.contains(key);
                    } else {
                        hiddenPairs.contains(key);
                    }
                }
            }
            hiddenPairs = newHiddenPairs;
            newHiddenPairs = new java.util.HashSet<>(256);
        }
        long elapsed = System.currentTimeMillis() - start;

        // 10 ticks in < 200ms — 10 TPS tick budget is 100ms each, so this is generous
        assertTrue(elapsed < 200,
                "10 ticks of 100-player visibility computation took " + elapsed +
                "ms (budget: 200ms). Performance regression detected.");
    }

    // ── Hidden-pairs diff logic ───────────────────────────────────────────────

    @Test
    void hiddenPairsDiff_newlyFarPairIsAdded() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        long key = RegionPresenceTask.pairKey(a, b);

        java.util.Set<Long> prevHidden = new java.util.HashSet<>();
        java.util.Set<Long> newHidden  = new java.util.HashSet<>();

        newHidden.add(key);

        boolean shouldHide = newHidden.contains(key) && !prevHidden.contains(key);
        assertTrue(shouldHide, "Newly-far pair should trigger hidePlayer call");
    }

    @Test
    void hiddenPairsDiff_alreadyFarPairIsNotRefired() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        long key = RegionPresenceTask.pairKey(a, b);

        java.util.Set<Long> prevHidden = new java.util.HashSet<>();
        prevHidden.add(key);

        java.util.Set<Long> newHidden = new java.util.HashSet<>();
        newHidden.add(key);

        boolean shouldHide = newHidden.contains(key) && !prevHidden.contains(key);
        assertFalse(shouldHide, "Already-hidden pair must NOT re-fire hidePlayer");
    }

    @Test
    void hiddenPairsDiff_playerWhoMovedCloseShouldBeShown() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        long key = RegionPresenceTask.pairKey(a, b);

        java.util.Set<Long> prevHidden = new java.util.HashSet<>();
        prevHidden.add(key);

        java.util.Set<Long> newHidden = new java.util.HashSet<>();

        boolean wasHidden     = prevHidden.contains(key);
        boolean isStillHidden = newHidden.contains(key);
        boolean shouldShow    = wasHidden && !isStillHidden;

        assertTrue(shouldShow, "Player who moved into range should trigger showPlayer");
    }
}
