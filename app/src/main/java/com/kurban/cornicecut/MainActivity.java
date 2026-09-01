package com.kurban.cornicecut;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText stockLengthInput;
    private EditText kerfInput;
    private EditText availableBarsInput;
    private EditText partsInput;
    private TextView resultView;
    private Button calculateButton;

    private final int bg = Color.rgb(245, 246, 248);
    private final int card = Color.WHITE;
    private final int text = Color.rgb(25, 29, 36);
    private final int muted = Color.rgb(96, 104, 118);
    private final int accent = Color.rgb(32, 88, 120);
    private final int good = Color.rgb(30, 110, 72);
    private final int warn = Color.rgb(168, 92, 24);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("РАСКРОЙ КАРНИЗА");
        title.setTextColor(text);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Минимум отходов • учёт пропила • схема по каждой палке");
        sub.setTextColor(muted);
        sub.setTextSize(14);
        sub.setPadding(0, dp(4), 0, dp(18));
        root.addView(sub);

        LinearLayout cardBox = new LinearLayout(this);
        cardBox.setOrientation(LinearLayout.VERTICAL);
        cardBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        cardBox.setBackgroundColor(card);
        root.addView(cardBox, lpMatchWrap(dp(14)));

        stockLengthInput = field(cardBox, "Длина одной палки, мм", "3000", true);
        kerfInput = field(cardBox, "Толщина пропила диска, мм", "3", true);
        availableBarsInput = field(cardBox, "Сколько палок есть (0 = без ограничения)", "0", true);

        TextView partsLabel = label("Детали, мм");
        partsLabel.setPadding(0, dp(12), 0, dp(6));
        cardBox.addView(partsLabel);

        partsInput = new EditText(this);
        partsInput.setHint("Например: 1480, 920, 760, 620\nМожно: 520x3");
        partsInput.setTextSize(17);
        partsInput.setTextColor(text);
        partsInput.setHintTextColor(Color.rgb(145, 150, 160));
        partsInput.setGravity(Gravity.TOP | Gravity.START);
        partsInput.setMinLines(4);
        partsInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        partsInput.setBackgroundColor(Color.rgb(249, 250, 252));
        cardBox.addView(partsInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));

        TextView help = new TextView(this);
        help.setText("Форматы: 1200, 850, 600   или   1200 850 600   или   450x4");
        help.setTextSize(12);
        help.setTextColor(muted);
        help.setPadding(0, dp(7), 0, dp(12));
        cardBox.addView(help);

        calculateButton = new Button(this);
        calculateButton.setText("РАССЧИТАТЬ РАСКРОЙ");
        calculateButton.setTextSize(16);
        calculateButton.setTextColor(Color.WHITE);
        calculateButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        calculateButton.setBackgroundColor(accent);
        calculateButton.setAllCaps(false);
        calculateButton.setPadding(dp(10), dp(13), dp(10), dp(13));
        cardBox.addView(calculateButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        calculateButton.setOnClickListener(v -> startCalculation());

        TextView note = new TextView(this);
        note.setText("Пропил считается на каждую отпиливаемую деталь — это безопасный вариант для реальной нарезки.");
        note.setTextSize(12);
        note.setTextColor(muted);
        note.setPadding(0, dp(10), 0, 0);
        cardBox.addView(note);

        TextView resultTitle = new TextView(this);
        resultTitle.setText("РЕЗУЛЬТАТ");
        resultTitle.setTextSize(18);
        resultTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultTitle.setTextColor(text);
        resultTitle.setPadding(0, dp(22), 0, dp(8));
        root.addView(resultTitle);

        resultView = new TextView(this);
        resultView.setText("Введите размеры и нажмите «Рассчитать раскрой».");
        resultView.setTextSize(16);
        resultView.setTextColor(text);
        resultView.setLineSpacing(0, 1.12f);
        resultView.setPadding(dp(16), dp(16), dp(16), dp(16));
        resultView.setBackgroundColor(card);
        root.addView(resultView, lpMatchWrap(0));

        setContentView(scroll);
    }

    private LinearLayout.LayoutParams lpMatchWrap(int bottomMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = bottomMargin;
        return lp;
    }

    private TextView label(String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(14);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setTextColor(text);
        return v;
    }

    private EditText field(LinearLayout parent, String labelText, String value, boolean numeric) {
        TextView l = label(labelText);
        l.setPadding(0, dp(8), 0, dp(5));
        parent.addView(l);

        EditText e = new EditText(this);
        e.setText(value);
        e.setTextSize(18);
        e.setTextColor(text);
        e.setSelectAllOnFocus(true);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundColor(Color.rgb(249, 250, 252));
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        parent.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return e;
    }

    private void startCalculation() {
        final int stock;
        final int kerf;
        final int available;
        final List<Integer> parts;

        try {
            stock = positiveInt(stockLengthInput.getText().toString(), "Длина палки");
            kerf = nonNegativeInt(kerfInput.getText().toString(), "Пропил");
            available = nonNegativeInt(availableBarsInput.getText().toString(), "Количество палок");
            parts = parseParts(partsInput.getText().toString());
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        if (parts.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одну деталь", Toast.LENGTH_LONG).show();
            return;
        }

        for (int p : parts) {
            if (p + kerf > stock) {
                Toast.makeText(this,
                        "Деталь " + p + " мм с учётом пропила не помещается в палку " + stock + " мм",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        calculateButton.setEnabled(false);
        calculateButton.setText("СЧИТАЮ…");
        resultView.setText("Подбираю самый выгодный раскрой…");

        new Thread(() -> {
            Solution solution = optimize(parts, stock, kerf, 3500);
            String output = formatSolution(solution, stock, kerf, available);
            runOnUiThread(() -> {
                resultView.setText(output);
                calculateButton.setEnabled(true);
                calculateButton.setText("РАССЧИТАТЬ РАСКРОЙ");
            });
        }).start();
    }

    private int positiveInt(String s, String name) {
        int v = parseIntSafe(s, name);
        if (v <= 0) throw new IllegalArgumentException(name + " должна быть больше 0");
        return v;
    }

    private int nonNegativeInt(String s, String name) {
        int v = parseIntSafe(s, name);
        if (v < 0) throw new IllegalArgumentException(name + " не может быть отрицательным");
        return v;
    }

    private int parseIntSafe(String s, String name) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Проверь поле «" + name + "»");
        }
    }

    private List<Integer> parseParts(String raw) {
        List<Integer> out = new ArrayList<>();
        String normalized = raw.toLowerCase(Locale.ROOT)
                .replace('×', 'x')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace(';', ' ')
                .replace(',', ' ');

        String[] tokens = normalized.trim().split("\\s+");
        Pattern p = Pattern.compile("^(\\d+)(?:[x*](\\d+))?$");

        for (String token : tokens) {
            if (token.trim().isEmpty()) continue;
            Matcher m = p.matcher(token.trim());
            if (!m.matches()) {
                throw new IllegalArgumentException("Не понял размер: " + token + ". Пример: 850 или 520x3");
            }
            int len = Integer.parseInt(m.group(1));
            int count = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
            if (len <= 0 || count <= 0) {
                throw new IllegalArgumentException("Размер и количество должны быть больше 0");
            }
            if (count > 200) {
                throw new IllegalArgumentException("Слишком большое количество одной детали: " + count);
            }
            for (int i = 0; i < count; i++) out.add(len);
        }
        if (out.size() > 250) {
            throw new IllegalArgumentException("За один расчёт можно добавить до 250 деталей");
        }
        return out;
    }

    private Solution optimize(List<Integer> lengths, int stock, int kerf, long maxMillis) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < lengths.size(); i++) {
            items.add(new Item(lengths.get(i), lengths.get(i) + kerf, i));
        }
        Collections.sort(items, (a, b) -> Integer.compare(b.effective, a.effective));

        Solution ffd = firstFitDecreasing(items, stock);
        int total = 0;
        for (Item it : items) total += it.effective;
        int lower = (total + stock - 1) / stock;

        long deadline = System.currentTimeMillis() + maxMillis;
        Solution best = ffd;
        boolean proven = false;

        if (items.size() <= 55) {
            for (int bins = lower; bins < ffd.bars.size(); bins++) {
                SearchState state = new SearchState(items, stock, bins, deadline);
                boolean ok = state.search(0);
                if (state.timedOut) {
                    best.exact = false;
                    return best;
                }
                if (ok) {
                    best = state.toSolution();
                    proven = true;
                    break;
                }
            }
            if (ffd.bars.size() == lower) proven = true;
            if (!proven && best == ffd) proven = true;
        }

        best.exact = proven;
        return best;
    }

    private Solution firstFitDecreasing(List<Item> items, int stock) {
        List<List<Item>> bars = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>();
        for (Item item : items) {
            boolean placed = false;
            for (int b = 0; b < bars.size(); b++) {
                if (remaining.get(b) >= item.effective) {
                    bars.get(b).add(item);
                    remaining.set(b, remaining.get(b) - item.effective);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Item> one = new ArrayList<>();
                one.add(item);
                bars.add(one);
                remaining.add(stock - item.effective);
            }
        }
        Solution s = new Solution();
        s.bars = bars;
        s.exact = false;
        return s;
    }

    private String formatSolution(Solution s, int stock, int kerf, int available) {
        StringBuilder sb = new StringBuilder();
        int usefulTotal = 0;
        int kerfTotal = 0;
        int wasteTotal = 0;

        sb.append("Нужно палок: ").append(s.bars.size()).append(" шт.\n");
        sb.append(s.exact ? "Раскрой: оптимальный\n" : "Раскрой: лучший найденный вариант\n");

        if (available > 0) {
            if (s.bars.size() <= available) {
                sb.append("Материала хватает. Останется целых палок: ")
                        .append(available - s.bars.size()).append(" шт.\n");
            } else {
                sb.append("⚠ Не хватает палок: ещё ")
                        .append(s.bars.size() - available).append(" шт.\n");
            }
        }

        sb.append("\n");

        for (int i = 0; i < s.bars.size(); i++) {
            List<Item> bar = s.bars.get(i);
            Collections.sort(bar, (a, b) -> Integer.compare(b.length, a.length));
            int usedEffective = 0;
            int useful = 0;

            sb.append("ПАЛКА №").append(i + 1).append(" — ").append(stock).append(" мм\n");
            sb.append("Резать: ");
            for (int j = 0; j < bar.size(); j++) {
                if (j > 0) sb.append(" + ");
                sb.append(bar.get(j).length);
                usedEffective += bar.get(j).effective;
                useful += bar.get(j).length;
            }
            int cuts = bar.size();
            int cutLoss = cuts * kerf;
            int leftover = stock - usedEffective;
            sb.append(" мм\n");
            sb.append("Пропил: ").append(cuts).append(" × ").append(kerf)
                    .append(" = ").append(cutLoss).append(" мм\n");
            sb.append("Остаток: ").append(leftover).append(" мм\n\n");

            usefulTotal += useful;
            kerfTotal += cutLoss;
            wasteTotal += leftover;
        }

        int stockTotal = s.bars.size() * stock;
        double efficiency = stockTotal == 0 ? 0 : (100.0 * usefulTotal / stockTotal);

        sb.append("ИТОГО\n");
        sb.append("Полезные детали: ").append(usefulTotal).append(" мм\n");
        sb.append("Уйдёт в пропил: ").append(kerfTotal).append(" мм\n");
        sb.append("Остатки: ").append(wasteTotal).append(" мм\n");
        sb.append(String.format(Locale.getDefault(), "Использование материала: %.1f%%", efficiency));

        return sb.toString();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    static class Item {
        final int length;
        final int effective;
        final int id;
        Item(int length, int effective, int id) {
            this.length = length;
            this.effective = effective;
            this.id = id;
        }
    }

    static class Solution {
        List<List<Item>> bars;
        boolean exact;
    }

    static class SearchState {
        final List<Item> items;
        final int stock;
        final int binCount;
        final int[] remaining;
        final int[] assignment;
        final long deadline;
        int[] foundAssignment;
        boolean timedOut = false;

        SearchState(List<Item> items, int stock, int binCount, long deadline) {
            this.items = items;
            this.stock = stock;
            this.binCount = binCount;
            this.deadline = deadline;
            this.remaining = new int[binCount];
            Arrays.fill(this.remaining, stock);
            this.assignment = new int[items.size()];
            Arrays.fill(this.assignment, -1);
        }

        boolean search(int index) {
            if (System.currentTimeMillis() > deadline) {
                timedOut = true;
                return false;
            }
            if (index >= items.size()) {
                foundAssignment = Arrays.copyOf(assignment, assignment.length);
                return true;
            }

            Item item = items.get(index);
            HashSet<Integer> triedRemaining = new HashSet<>();

            for (int b = 0; b < binCount; b++) {
                int before = remaining[b];
                if (before < item.effective) continue;
                if (!triedRemaining.add(before)) continue;

                remaining[b] -= item.effective;
                assignment[index] = b;
                if (search(index + 1)) return true;
                if (timedOut) return false;
                assignment[index] = -1;
                remaining[b] = before;

                if (before == stock) break;
            }
            return false;
        }

        Solution toSolution() {
            List<List<Item>> bars = new ArrayList<>();
            for (int i = 0; i < binCount; i++) bars.add(new ArrayList<>());
            for (int i = 0; i < items.size(); i++) {
                bars.get(foundAssignment[i]).add(items.get(i));
            }
            Solution s = new Solution();
            s.bars = bars;
            s.exact = true;
            return s;
        }
    }
}
