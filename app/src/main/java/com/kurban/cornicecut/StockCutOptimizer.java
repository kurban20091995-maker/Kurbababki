package com.kurban.cornicecut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Mixed-length finite-stock cutting optimizer with an exact branch-and-bound pass for small jobs. */
public final class StockCutOptimizer {
    private StockCutOptimizer() {}

    public static final class CutPiece {
        public final String id;
        public final double referenceLength;
        public final double consumption;
        public CutPiece(String id, double referenceLength, double consumption) {
            this.id = id;
            this.referenceLength = referenceLength;
            this.consumption = consumption;
        }
    }

    public static final class StockPiece {
        public final String id;
        public final double length;
        public final boolean leftover;
        public StockPiece(String id, double length, boolean leftover) {
            this.id = id;
            this.length = length;
            this.leftover = leftover;
        }
    }

    public static final class BarPlan {
        public final StockPiece stock;
        public final List<CutPiece> pieces;
        public final double remaining;
        BarPlan(StockPiece stock, List<CutPiece> pieces, double remaining) {
            this.stock = stock;
            this.pieces = pieces;
            this.remaining = remaining;
        }
    }

    public static final class Solution {
        public final List<BarPlan> bars;
        public final boolean exact;
        public final double totalUseful;
        public final double totalConsumed;
        public final double totalWaste;
        Solution(List<BarPlan> bars, boolean exact, double totalUseful, double totalConsumed, double totalWaste) {
            this.bars = bars;
            this.exact = exact;
            this.totalUseful = totalUseful;
            this.totalConsumed = totalConsumed;
            this.totalWaste = totalWaste;
        }
    }

    public static Solution optimize(List<CutPiece> rawPieces, List<StockPiece> rawStocks, long maxMillis) {
        if (rawPieces == null || rawPieces.isEmpty()) throw new IllegalArgumentException("Нет деталей для раскроя");
        if (rawStocks == null || rawStocks.isEmpty()) throw new IllegalArgumentException("Нет хлыстов для раскроя");

        List<CutPiece> pieces = new ArrayList<>(rawPieces);
        pieces.sort((a, b) -> Double.compare(b.consumption, a.consumption));
        List<StockPiece> stocks = new ArrayList<>(rawStocks);
        stocks.sort((a, b) -> {
            if (a.leftover != b.leftover) return a.leftover ? -1 : 1;
            return Double.compare(b.length, a.length);
        });

        double maxStock = 0;
        for (StockPiece s : stocks) maxStock = Math.max(maxStock, s.length);
        for (CutPiece p : pieces) {
            if (p.consumption > maxStock + 1e-7) {
                throw new IllegalArgumentException("Деталь " + p.id + " (" + round1(p.referenceLength) + " мм) не помещается ни в один хлыст");
            }
        }

        Search search = new Search(pieces, stocks, Math.max(250, maxMillis));
        search.seedGreedy(false);
        search.seedGreedy(true);
        if (search.best == null) throw new IllegalArgumentException("Имеющихся хлыстов недостаточно");

        boolean exactEligible = pieces.size() <= 18 && stocks.size() <= 20;
        if (exactEligible) search.runExact();
        return search.toSolution(exactEligible && !search.timedOut);
    }

    private static final class Search {
        final List<CutPiece> pieces;
        final List<StockPiece> stocks;
        final long deadline;
        final double[] rem;
        final boolean[] used;
        final List<List<CutPiece>> assign;
        PlanSnapshot best;
        boolean timedOut;
        double[] suffix;
        double maxStock;

        Search(List<CutPiece> pieces, List<StockPiece> stocks, long maxMillis) {
            this.pieces = pieces;
            this.stocks = stocks;
            this.deadline = System.currentTimeMillis() + maxMillis;
            this.rem = new double[stocks.size()];
            this.used = new boolean[stocks.size()];
            this.assign = new ArrayList<>();
            for (int i = 0; i < stocks.size(); i++) {
                rem[i] = stocks.get(i).length;
                assign.add(new ArrayList<>());
                maxStock = Math.max(maxStock, stocks.get(i).length);
            }
            suffix = new double[pieces.size() + 1];
            for (int i = pieces.size() - 1; i >= 0; i--) suffix[i] = suffix[i + 1] + pieces.get(i).consumption;
        }

        void reset() {
            for (int i = 0; i < stocks.size(); i++) {
                rem[i] = stocks.get(i).length;
                used[i] = false;
                assign.get(i).clear();
            }
        }

        void seedGreedy(boolean preferShortestNewBar) {
            reset();
            for (CutPiece p : pieces) {
                int chosen = -1;
                double bestAfter = Double.POSITIVE_INFINITY;
                for (int i = 0; i < stocks.size(); i++) {
                    if (used[i] && rem[i] + 1e-7 >= p.consumption) {
                        double after = rem[i] - p.consumption;
                        if (after < bestAfter) { bestAfter = after; chosen = i; }
                    }
                }
                if (chosen < 0) {
                    double score = preferShortestNewBar ? Double.POSITIVE_INFINITY : -Double.POSITIVE_INFINITY;
                    for (int i = 0; i < stocks.size(); i++) {
                        if (used[i] || stocks.get(i).length + 1e-7 < p.consumption) continue;
                        double candidate = stocks.get(i).length;
                        // Reusable leftovers get a small preference without allowing an impossible fit.
                        if (stocks.get(i).leftover) candidate += preferShortestNewBar ? -0.001 : 0.001;
                        if ((preferShortestNewBar && candidate < score) || (!preferShortestNewBar && candidate > score)) {
                            score = candidate;
                            chosen = i;
                        }
                    }
                }
                if (chosen < 0) { reset(); return; }
                used[chosen] = true;
                rem[chosen] -= p.consumption;
                assign.get(chosen).add(p);
            }
            considerBest();
            reset();
        }

        void runExact() {
            reset();
            dfs(0, 0);
        }

        void dfs(int index, int usedCount) {
            if (System.currentTimeMillis() > deadline) { timedOut = true; return; }
            if (best != null && usedCount > best.usedBars) return;
            if (index == pieces.size()) { considerBest(); return; }

            double freeInUsed = 0;
            for (int i = 0; i < stocks.size(); i++) if (used[i]) freeInUsed += rem[i];
            double need = Math.max(0, suffix[index] - freeInUsed);
            int minExtra = (int) Math.ceil(need / Math.max(1.0, maxStock) - 1e-12);
            if (best != null && usedCount + minExtra > best.usedBars) return;

            CutPiece p = pieces.get(index);
            Set<Long> seenRemaining = new HashSet<>();

            // First fill already opened bars (best fit order is naturally pruned by duplicate remaining states).
            for (int i = 0; i < stocks.size(); i++) {
                if (!used[i] || rem[i] + 1e-7 < p.consumption) continue;
                long key = Math.round(rem[i] * 1000.0);
                if (!seenRemaining.add(key)) continue;
                rem[i] -= p.consumption;
                assign.get(i).add(p);
                dfs(index + 1, usedCount);
                assign.get(i).remove(assign.get(i).size() - 1);
                rem[i] += p.consumption;
                if (timedOut) return;
            }

            // Then open a new stock piece. Skip equivalent stock instances to avoid factorial permutations.
            Set<String> seenStockTypes = new HashSet<>();
            for (int i = 0; i < stocks.size(); i++) {
                if (used[i] || stocks.get(i).length + 1e-7 < p.consumption) continue;
                StockPiece s = stocks.get(i);
                String type = Math.round(s.length * 1000.0) + ":" + s.leftover;
                if (!seenStockTypes.add(type)) continue;
                if (best != null && usedCount + 1 > best.usedBars) continue;
                used[i] = true;
                rem[i] -= p.consumption;
                assign.get(i).add(p);
                dfs(index + 1, usedCount + 1);
                assign.get(i).remove(assign.get(i).size() - 1);
                rem[i] += p.consumption;
                used[i] = false;
                if (timedOut) return;
            }
        }

        void considerBest() {
            int usedBars = 0;
            int newBars = 0;
            double usedLength = 0;
            double maxRemainder = 0;
            for (int i = 0; i < stocks.size(); i++) {
                if (!used[i]) continue;
                usedBars++;
                if (!stocks.get(i).leftover) newBars++;
                usedLength += stocks.get(i).length;
                maxRemainder = Math.max(maxRemainder, rem[i]);
            }
            if (usedBars == 0) return;
            if (best != null && !better(usedBars, newBars, usedLength, maxRemainder, best)) return;

            List<List<CutPiece>> copy = new ArrayList<>();
            double[] remCopy = rem.clone();
            boolean[] usedCopy = used.clone();
            for (List<CutPiece> list : assign) copy.add(new ArrayList<>(list));
            best = new PlanSnapshot(usedBars, newBars, usedLength, maxRemainder, copy, remCopy, usedCopy);
        }

        boolean better(int usedBars, int newBars, double usedLength, double maxRemainder, PlanSnapshot b) {
            if (usedBars != b.usedBars) return usedBars < b.usedBars;
            if (newBars != b.newBars) return newBars < b.newBars;
            if (Math.abs(usedLength - b.usedLength) > 1e-7) return usedLength < b.usedLength;
            return maxRemainder > b.maxRemainder + 1e-7;
        }

        Solution toSolution(boolean exact) {
            List<BarPlan> bars = new ArrayList<>();
            double useful = 0;
            double consumed = 0;
            double waste = 0;
            for (CutPiece p : pieces) { useful += p.referenceLength; consumed += p.consumption; }
            for (int i = 0; i < stocks.size(); i++) {
                if (!best.used[i]) continue;
                List<CutPiece> ps = new ArrayList<>(best.assign.get(i));
                ps.sort(Comparator.comparingDouble((CutPiece x) -> x.referenceLength).reversed());
                bars.add(new BarPlan(stocks.get(i), ps, best.rem[i]));
                waste += best.rem[i];
            }
            bars.sort((a, b) -> {
                if (a.stock.leftover != b.stock.leftover) return a.stock.leftover ? -1 : 1;
                return Double.compare(b.stock.length, a.stock.length);
            });
            return new Solution(bars, exact, useful, consumed, waste);
        }
    }

    private static final class PlanSnapshot {
        final int usedBars;
        final int newBars;
        final double usedLength;
        final double maxRemainder;
        final List<List<CutPiece>> assign;
        final double[] rem;
        final boolean[] used;
        PlanSnapshot(int usedBars, int newBars, double usedLength, double maxRemainder,
                     List<List<CutPiece>> assign, double[] rem, boolean[] used) {
            this.usedBars = usedBars;
            this.newBars = newBars;
            this.usedLength = usedLength;
            this.maxRemainder = maxRemainder;
            this.assign = assign;
            this.rem = rem;
            this.used = used;
        }
    }

    private static String round1(double v) {
        return String.format(java.util.Locale.US, "%.1f", v);
    }
}
