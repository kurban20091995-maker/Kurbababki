package com.kurban.cornicecut;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(242, 244, 247);
    private final int card = Color.WHITE;
    private final int text = Color.rgb(27, 32, 39);
    private final int muted = Color.rgb(94, 104, 116);
    private final int accent = Color.rgb(30, 95, 132);
    private final int danger = Color.rgb(155, 55, 48);

    private LinearLayout content;
    private ProfileStore store;
    private List<ProfileStore.Profile> profiles;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ProfileStore(this);
        profiles = store.loadProfiles();
        buildShell();
        showNewCut();
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(16), dp(14), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("РАСКРОЙ КАРНИЗА v2");
        title.setTextSize(26);
        title.setTextColor(text);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Вылет • геометрия кухни • углы пилы • минимум отходов");
        sub.setTextSize(13);
        sub.setTextColor(muted);
        sub.setPadding(0, dp(3), 0, dp(12));
        root.addView(sub);

        LinearLayout nav1 = row();
        nav1.addView(navButton("НОВЫЙ РАСКРОЙ", v -> showNewCut()), weightLp());
        nav1.addView(navButton("МОИ КАРНИЗЫ", v -> showProfiles()), weightLp());
        root.addView(nav1, matchWrap(dp(6)));

        LinearLayout nav2 = row();
        nav2.addView(navButton("ОСТАТКИ", v -> showLeftovers()), weightLp());
        nav2.addView(navButton("РАСЧЁТ УГЛОВ", v -> showAngles()), weightLp());
        root.addView(nav2, matchWrap(dp(14)));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, matchWrap(0));
        setContentView(scroll);
    }

    private void showNewCut() {
        clearContent();
        TextView h = heading("НОВЫЙ РАСКРОЙ");
        content.addView(h);

        LinearLayout box = cardBox();
        content.addView(box, matchWrap(dp(14)));

        final Spinner profileSpinner = spinner(box, "1. Выберите профиль", profileNames());
        final EditText stockSpec = field(box, "2. Новые хлысты (длина × количество)", profiles.get(0).stockSpec, false);
        final EditText leftoverSpec = field(box, "Остатки этого профиля", store.loadLeftovers(profiles.get(0).id), false);
        final EditText effectiveOverhang = field(box, "Фактический вылет от выбранной линии, мм", fmt1(profiles.get(0).effectiveOverhang), true);
        final Spinner offsetSide = spinner(box, "Карниз находится относительно маршрута кухни", new String[]{"СЛЕВА от линии", "СПРАВА от линии"});
        final Spinner referenceLine = spinner(box, "Контрольная линия замера", new String[]{"по корпусу", "по фасаду", "по задней установочной кромке", "по передней кромке", "вручную заданная линия"});
        final EditText kerf = field(box, "Толщина пропила, мм", "3", true);
        final EditText trim = field(box, "Запас на чистовой рез НА ДЕТАЛЬ, мм", "0", true);
        final EditText startExt = field(box, "Удлинение свободного левого конца, мм", "0", true);
        final EditText endExt = field(box, "Удлинение свободного правого конца, мм", "0", true);

        TextView planLabel = label("3–4. Участки кухни и углы");
        planLabel.setPadding(0, dp(12), 0, dp(5));
        box.addView(planLabel);
        final EditText planInput = multiline(box,
                "1800 R90\n1200 L135\n900",
                "Каждая строка: длина и угол ДО следующего участка. R/П = направо, L/Л = налево.\nПример: 1800 R90 затем 1200. Для угла стены 135° пишите R135/L135.");

        TextView syntax = small("Угол после R/L — это реальный угол МЕЖДУ соседними участками (90°, 87.5°, 135° и т.д.), а не угол поворота направления.");
        box.addView(syntax);

        PlanView planView = new PlanView(this);
        planView.setMinimumHeight(dp(260));
        box.addView(planView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(280)));

        final TextView result = resultBox("Введите схему кухни и нажмите «Рассчитать».");

        Button calc = actionButton("5–6. РАССЧИТАТЬ ГЕОМЕТРИЮ И РАСКРОЙ");
        box.addView(calc, matchHeight(dp(56), dp(10)));
        box.addView(small("Синяя линия на схеме — расчётная линия карниза; тёмная — исходная линия кухни."));
        content.addView(heading("РЕЗУЛЬТАТ"));
        content.addView(result, matchWrap(0));

        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ProfileStore.Profile p = profiles.get(position);
                stockSpec.setText(p.stockSpec);
                leftoverSpec.setText(store.loadLeftovers(p.id));
                effectiveOverhang.setText(fmt1(p.effectiveOverhang));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        calc.setOnClickListener(v -> {
            try {
                int pi = profileSpinner.getSelectedItemPosition();
                ProfileStore.Profile profile = profiles.get(Math.max(0, pi));
                double overhang = number(effectiveOverhang, "Фактический вылет");
                double kerfMm = number(kerf, "Пропил");
                double trimMm = number(trim, "Запас на чистовой рез");
                double startMm = number(startExt, "Удлинение левого конца");
                double endMm = number(endExt, "Удлинение правого конца");
                if (overhang < 0 || kerfMm < 0 || trimMm < 0 || startMm < 0 || endMm < 0) throw new IllegalArgumentException("Размеры не могут быть отрицательными");

                List<CorniceGeometryEngine.SegmentSpec> specs = parsePlan(planInput.getText().toString());
                boolean offsetLeft = offsetSide.getSelectedItemPosition() == 0;
                CorniceGeometryEngine.Result geo = CorniceGeometryEngine.calculate(specs, overhang, offsetLeft, startMm, endMm);
                planView.setResult(geo);

                List<StockCutOptimizer.StockPiece> stocks = new ArrayList<>();
                stocks.addAll(parseStocks(stockSpec.getText().toString(), false, "Х"));
                stocks.addAll(parseStocks(leftoverSpec.getText().toString(), true, "О"));
                if (stocks.isEmpty()) throw new IllegalArgumentException("Добавьте хотя бы один хлыст или остаток");

                List<StockCutOptimizer.CutPiece> cuts = new ArrayList<>();
                for (CorniceGeometryEngine.SegmentResult s : geo.segments) {
                    cuts.add(new StockCutOptimizer.CutPiece(s.id, s.referenceLength, s.referenceLength + trimMm + kerfMm));
                }
                StockCutOptimizer.Solution solution = StockCutOptimizer.optimize(cuts, stocks, 4500);
                result.setText(formatJob(profile, geo, solution, kerfMm, trimMm,
                        referenceLine.getSelectedItem().toString()));
            } catch (Exception ex) {
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatJob(ProfileStore.Profile profile, CorniceGeometryEngine.Result geo,
                             StockCutOptimizer.Solution solution, double kerf, double trim, String referenceLine) {
        StringBuilder sb = new StringBuilder();
        sb.append("ПРОФИЛЬ: ").append(profile.name).append("\n");
        sb.append("Размер детали указан: ").append(referenceLine).append(" → смещённая линия карниза\n");
        sb.append("Фактический вылет: ").append(fmt1(geo.effectiveOverhang)).append(" мм\n");
        sb.append("Угол наклона профиля: ").append(fmt2(profile.springAngle)).append("°\n\n");

        sb.append("РАССЧИТАННЫЕ ДЕТАЛИ\n");
        for (CorniceGeometryEngine.SegmentResult s : geo.segments) {
            sb.append(s.id).append(": кухня ").append(fmt1(s.baseLength))
              .append(" → карниз ").append(fmt1(s.referenceLength))
              .append(" → расход ").append(fmt1(s.referenceLength + trim + kerf)).append(" мм\n");
            if (s.leftCornerAngleDeg > 0) appendCut(sb, "  левый конец", profile.springAngle, s.leftCornerAngleDeg, s.leftTurnRight, false);
            else sb.append("  левый конец: прямой\n");
            if (s.rightCornerAngleDeg > 0) appendCut(sb, "  правый конец", profile.springAngle, s.rightCornerAngleDeg, s.rightTurnRight, true);
            else sb.append("  правый конец: прямой\n");
        }

        sb.append("\nОПТИМАЛЬНЫЙ РАСКРОЙ\n");
        double usedMaterial = 0;
        int n = 1;
        for (StockCutOptimizer.BarPlan b : solution.bars) {
            usedMaterial += b.stock.length;
            sb.append("\n").append(b.stock.leftover ? "ОСТАТОК " : "ХЛЫСТ ")
              .append("№").append(n++).append(" — ").append(fmt1(b.stock.length)).append(" мм\n");
            sb.append("Резать: ");
            for (int i = 0; i < b.pieces.size(); i++) {
                if (i > 0) sb.append(" + ");
                sb.append(b.pieces.get(i).id).append(" ").append(fmt1(b.pieces.get(i).referenceLength));
            }
            sb.append("\nОстаток после резов: ").append(fmt1(b.remaining)).append(" мм\n");
        }

        int pieces = geo.segments.size();
        double kerfLoss = pieces * kerf;
        double trimLoss = pieces * trim;
        double usage = usedMaterial > 0 ? (solution.totalUseful / usedMaterial * 100.0) : 0;
        sb.append("\nИТОГО\n")
          .append("Хлыстов/остатков использовано: ").append(solution.bars.size()).append("\n")
          .append(solution.exact ? "Оптимум для этого набора доказан\n" : "Лучший найденный вариант (большой набор)\n")
          .append("Материала взято: ").append(fmt1(usedMaterial)).append(" мм\n")
          .append("Полезная длина деталей: ").append(fmt1(solution.totalUseful)).append(" мм\n")
          .append("Пропил (безопасный учёт 1 рез/деталь): ").append(fmt1(kerfLoss)).append(" мм\n")
          .append("Запас на чистовой рез: ").append(fmt1(trimLoss)).append(" мм\n")
          .append("Суммарные остатки: ").append(fmt1(solution.totalWaste)).append(" мм\n")
          .append("Использование материала по полезной длине: ").append(fmt1(usage)).append("%\n\n")
          .append("⚠ Перед чистовым угловым резом сделайте проверку на обрезке: фактический угол стены и ориентация профиля на конкретной пиле могут отличаться.");
        return sb.toString();
    }

    private void appendCut(StringBuilder sb, String name, double spring, double corner, Boolean turnRight, boolean rightEnd) {
        try {
            CompoundCutCalculator.Result c = CompoundCutCalculator.calculate(spring, corner);
            boolean tr = turnRight != null && turnRight;
            sb.append(name).append(": угол ").append(fmt2(corner)).append("°, MITER ")
              .append(fmt2(c.miterDeg)).append("°, BEVEL ").append(fmt2(c.bevelDeg)).append("°; ")
              .append(CompoundCutCalculator.directionHint(tr, rightEnd)).append("*\n");
        } catch (Exception ex) {
            sb.append(name).append(": ").append(ex.getMessage()).append("\n");
        }
    }

    private void showProfiles() {
        clearContent();
        content.addView(heading("МОИ КАРНИЗЫ"));
        LinearLayout box = cardBox();
        content.addView(box, matchWrap(0));

        final Spinner choose = spinner(box, "Сохранённый профиль", profileNames());
        final EditText name = field(box, "Название", profiles.get(0).name, false);
        final EditText height = field(box, "Высота профиля, мм", fmt1(profiles.get(0).height), true);
        final EditText profileOverhang = field(box, "Габаритный вылет профиля, мм", fmt1(profiles.get(0).profileOverhang), true);
        final EditText platform = field(box, "Установочная площадка, мм", fmt1(profiles.get(0).platform), true);
        final EditText effective = field(box, "ФАКТИЧЕСКИЙ вылет от корпуса/фасада, мм", fmt1(profiles.get(0).effectiveOverhang), true);
        final EditText spring = field(box, "Угол установки / spring angle, °", fmt2(profiles.get(0).springAngle), true);
        final EditText stocks = field(box, "Стандартные хлысты", profiles.get(0).stockSpec, false);
        final EditText color = field(box, "Цвет", profiles.get(0).color, false);
        final EditText coating = field(box, "Покрытие", profiles.get(0).coating, false);
        final EditText batch = field(box, "Партия", profiles.get(0).batch, false);

        choose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ProfileStore.Profile p = profiles.get(position);
                name.setText(p.name); height.setText(fmt1(p.height)); profileOverhang.setText(fmt1(p.profileOverhang));
                platform.setText(fmt1(p.platform)); effective.setText(fmt1(p.effectiveOverhang)); spring.setText(fmt2(p.springAngle));
                stocks.setText(p.stockSpec); color.setText(p.color); coating.setText(p.coating); batch.setText(p.batch);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        TextView note = small("Важно: габаритный вылет профиля и фактическое нависание над выбранной линией — разные поля. В геометрию идёт только фактический вылет.");
        note.setPadding(0, dp(10), 0, dp(8));
        box.addView(note);

        Button update = actionButton("ОБНОВИТЬ ВЫБРАННЫЙ ПРОФИЛЬ");
        box.addView(update, matchHeight(dp(52), dp(8)));
        Button add = secondaryButton("СОХРАНИТЬ КАК НОВЫЙ");
        box.addView(add, matchHeight(dp(50), dp(8)));
        Button del = secondaryButton("УДАЛИТЬ ПРОФИЛЬ");
        del.setTextColor(danger);
        box.addView(del, matchHeight(dp(48), 0));

        update.setOnClickListener(v -> {
            try {
                int i = Math.max(0, choose.getSelectedItemPosition());
                fillProfile(profiles.get(i), name, height, profileOverhang, platform, effective, spring, stocks, color, coating, batch);
                store.saveProfiles(profiles);
                Toast.makeText(this, "Профиль обновлён", Toast.LENGTH_SHORT).show();
                showProfiles();
            } catch (Exception ex) { Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show(); }
        });
        add.setOnClickListener(v -> {
            try {
                ProfileStore.Profile p = new ProfileStore.Profile();
                p.id = "p" + System.currentTimeMillis();
                fillProfile(p, name, height, profileOverhang, platform, effective, spring, stocks, color, coating, batch);
                profiles.add(p); store.saveProfiles(profiles);
                Toast.makeText(this, "Новый профиль сохранён", Toast.LENGTH_SHORT).show();
                showProfiles();
            } catch (Exception ex) { Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show(); }
        });
        del.setOnClickListener(v -> {
            if (profiles.size() <= 1) { Toast.makeText(this, "Должен остаться хотя бы один профиль", Toast.LENGTH_LONG).show(); return; }
            int i = Math.max(0, choose.getSelectedItemPosition());
            profiles.remove(i); store.saveProfiles(profiles); showProfiles();
        });
    }

    private void fillProfile(ProfileStore.Profile p, EditText name, EditText height, EditText profileOverhang,
                             EditText platform, EditText effective, EditText spring, EditText stocks,
                             EditText color, EditText coating, EditText batch) {
        String n = name.getText().toString().trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Введите название профиля");
        p.name = n;
        p.height = number(height, "Высота");
        p.profileOverhang = number(profileOverhang, "Габаритный вылет");
        p.platform = number(platform, "Установочная площадка");
        p.effectiveOverhang = number(effective, "Фактический вылет");
        p.springAngle = number(spring, "Угол установки");
        if (!(p.springAngle > 0 && p.springAngle < 90)) throw new IllegalArgumentException("Угол установки должен быть между 0° и 90°");
        p.stockSpec = stocks.getText().toString().trim();
        if (p.stockSpec.isEmpty()) throw new IllegalArgumentException("Укажите стандартные хлысты");
        p.color = color.getText().toString().trim();
        p.coating = coating.getText().toString().trim();
        p.batch = batch.getText().toString().trim();
    }

    private void showLeftovers() {
        clearContent();
        content.addView(heading("МОИ ОСТАТКИ"));
        LinearLayout box = cardBox(); content.addView(box, matchWrap(0));
        final Spinner choose = spinner(box, "Профиль", profileNames());
        final EditText leftovers = field(box, "Длины остатков, мм", store.loadLeftovers(profiles.get(0).id), false);
        box.addView(small("Можно: 1480, 920, 670 или 1480x2. Остатки не смешиваются между разными профилями."));
        choose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { leftovers.setText(store.loadLeftovers(profiles.get(position).id)); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        Button save = actionButton("СОХРАНИТЬ ОСТАТКИ"); box.addView(save, matchHeight(dp(52), dp(8)));
        save.setOnClickListener(v -> {
            int i = Math.max(0, choose.getSelectedItemPosition());
            store.saveLeftovers(profiles.get(i).id, leftovers.getText().toString());
            Toast.makeText(this, "Остатки сохранены", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAngles() {
        clearContent();
        content.addView(heading("РАСЧЁТ УГЛОВ"));
        LinearLayout box = cardBox(); content.addView(box, matchWrap(0));
        final EditText spring = field(box, "Угол наклона карниза, °", "38", true);
        final EditText corner = field(box, "Угол между участками кухни, °", "90", true);
        final TextView out = resultBox("Введите значения.");
        Button calc = actionButton("РАССЧИТАТЬ MITER / BEVEL"); box.addView(calc, matchHeight(dp(52), dp(10)));
        calc.setOnClickListener(v -> {
            try {
                double s = number(spring, "Угол наклона"); double c = number(corner, "Угол кухни");
                CompoundCutCalculator.Result r = CompoundCutCalculator.calculate(s, c);
                out.setText("MITER / поворот стола: " + fmt2(r.miterDeg) + "°\nBEVEL / наклон диска: " + fmt2(r.bevelDeg) + "°\n\nДля 38° и угла 90° контроль ≈ 31.62° / 33.86°.\nДля 45° и 90° контроль ≈ 35.26° / 30.00°.");
            } catch (Exception ex) { Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show(); }
        });

        TextView th = label("Определить наклон по двум размерам"); th.setPadding(0, dp(18), 0, dp(5)); box.addView(th);
        final EditText horizontal = field(box, "Горизонтальное отклонение, мм", "60", true);
        final EditText vertical = field(box, "Вертикальный размер установочного треугольника, мм", "80", true);
        Button tri = secondaryButton("РАССЧИТАТЬ ОБА УГЛА"); box.addView(tri, matchHeight(dp(50), dp(8)));
        tri.setOnClickListener(v -> {
            try {
                double h = number(horizontal, "Горизонталь"); double vv = number(vertical, "Вертикаль");
                double fromV = CompoundCutCalculator.angleFromVertical(h, vv);
                double fromH = CompoundCutCalculator.angleFromHorizontal(h, vv);
                out.setText("От вертикали: " + fmt2(fromV) + "°\nОт горизонтали: " + fmt2(fromH) + "°\nСумма: " + fmt2(fromV + fromH) + "°\n\nИспользуйте эти формулы только если введённые размеры — именно катеты РЕАЛЬНОГО установочного треугольника, а не общие габариты декоративного профиля.");
            } catch (Exception ex) { Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show(); }
        });
        box.addView(out, matchWrap(0));
    }

    private List<CorniceGeometryEngine.SegmentSpec> parsePlan(String raw) {
        String[] lines = raw.trim().split("\\r?\\n");
        List<CorniceGeometryEngine.SegmentSpec> out = new ArrayList<>();
        Pattern p = Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:([RrLlПпЛл])\\s*(\\d+(?:[.,]\\d+)?))?\\s*$");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            Matcher m = p.matcher(lines[i]);
            if (!m.matches()) throw new IllegalArgumentException("Не понял строку схемы: " + lines[i] + ". Пример: 1800 R90");
            double length = parseNumber(m.group(1));
            String turn = m.group(2);
            double corner = m.group(3) == null ? 0 : parseNumber(m.group(3));
            boolean turnRight = turn != null && (turn.equalsIgnoreCase("R") || turn.equalsIgnoreCase("П"));
            out.add(new CorniceGeometryEngine.SegmentSpec(segmentId(out.size()), length, corner, turnRight));
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Добавьте хотя бы один участок");
        for (int i = 0; i < out.size() - 1; i++) {
            if (out.get(i).cornerAngleAfterDeg == 0) throw new IllegalArgumentException("После участка " + out.get(i).id + " укажите R90/L90 или другой реальный угол");
        }
        return out;
    }

    private List<StockCutOptimizer.StockPiece> parseStocks(String raw, boolean leftover, String prefix) {
        List<StockCutOptimizer.StockPiece> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        String normalized = raw.replace('×', 'x');
        Pattern p = Pattern.compile("(\\d+(?:[.,]\\d+)?)(?:\\s*[xX*]\\s*(\\d+))?");
        Matcher m = p.matcher(normalized);
        int seq = 1;
        while (m.find()) {
            double len = parseNumber(m.group(1));
            int count = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
            if (!(len > 0) || count < 1 || count > 100) throw new IllegalArgumentException("Проверь список хлыстов");
            for (int i = 0; i < count; i++) out.add(new StockCutOptimizer.StockPiece(prefix + (seq++), len, leftover));
        }
        return out;
    }

    private String segmentId(int i) {
        if (i < 26) return String.valueOf((char)('A' + i));
        return "P" + (i + 1);
    }

    private void clearContent() { content.removeAllViews(); }
    private LinearLayout row() { LinearLayout x = new LinearLayout(this); x.setOrientation(LinearLayout.HORIZONTAL); return x; }
    private LinearLayout cardBox() { LinearLayout x = new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(dp(14), dp(14), dp(14), dp(14)); x.setBackgroundColor(card); return x; }

    private TextView heading(String s) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(18); v.setTextColor(text); v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); v.setPadding(0, dp(8), 0, dp(8)); return v;
    }
    private TextView label(String s) { TextView v = new TextView(this); v.setText(s); v.setTextSize(14); v.setTextColor(text); v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private TextView small(String s) { TextView v = new TextView(this); v.setText(s); v.setTextSize(12); v.setTextColor(muted); v.setLineSpacing(0, 1.12f); return v; }
    private TextView resultBox(String s) { TextView v = new TextView(this); v.setText(s); v.setTextSize(14); v.setTextColor(text); v.setLineSpacing(0, 1.13f); v.setPadding(dp(14), dp(14), dp(14), dp(14)); v.setBackgroundColor(card); v.setTextIsSelectable(true); return v; }

    private Button navButton(String s, View.OnClickListener l) { Button b = new Button(this); b.setText(s); b.setTextSize(11); b.setAllCaps(false); b.setOnClickListener(l); return b; }
    private Button actionButton(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); b.setBackgroundColor(accent); return b; }
    private Button secondaryButton(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(text); b.setTextSize(13); b.setAllCaps(false); return b; }

    private EditText field(LinearLayout parent, String label, String value, boolean numeric) {
        TextView l = label(label); l.setPadding(0, dp(8), 0, dp(4)); parent.addView(l);
        EditText e = new EditText(this); e.setText(value == null ? "" : value); e.setTextSize(16); e.setTextColor(text); e.setSelectAllOnFocus(true); e.setSingleLine(true); e.setPadding(dp(10), dp(8), dp(10), dp(8)); e.setBackgroundColor(Color.rgb(248, 249, 251));
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        parent.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))); return e;
    }

    private EditText multiline(LinearLayout parent, String value, String hint) {
        EditText e = new EditText(this); e.setText(value); e.setHint(hint); e.setTextSize(16); e.setTextColor(text); e.setHintTextColor(Color.rgb(130, 138, 148)); e.setGravity(Gravity.TOP | Gravity.START); e.setMinLines(5); e.setPadding(dp(10), dp(10), dp(10), dp(10)); e.setBackgroundColor(Color.rgb(248, 249, 251));
        parent.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150))); return e;
    }

    private Spinner spinner(LinearLayout parent, String labelText, String[] values) {
        TextView l = label(labelText); l.setPadding(0, dp(8), 0, dp(4)); parent.addView(l);
        Spinner s = new Spinner(this); ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values); a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a); parent.addView(s, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))); return s;
    }

    private String[] profileNames() { String[] a = new String[profiles.size()]; for (int i = 0; i < profiles.size(); i++) a[i] = profiles.get(i).name; return a; }
    private double number(EditText e, String name) { try { return parseNumber(e.getText().toString()); } catch (Exception ex) { throw new IllegalArgumentException("Проверь поле «" + name + "»"); } }
    private double parseNumber(String s) { return Double.parseDouble(s.trim().replace(',', '.')); }
    private String fmt1(double v) { return String.format(Locale.US, "%.1f", v); }
    private String fmt2(double v) { return String.format(Locale.US, "%.2f", v); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private LinearLayout.LayoutParams weightLp() { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f); lp.setMargins(dp(2), 0, dp(2), 0); return lp; }
    private LinearLayout.LayoutParams matchWrap(int bottom) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.bottomMargin = bottom; return lp; }
    private LinearLayout.LayoutParams matchHeight(int height, int bottom) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height); lp.bottomMargin = bottom; return lp; }
}
