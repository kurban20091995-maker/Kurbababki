package ru.raschet.ku;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(11, 24, 39);
    private final int CARD = Color.rgb(18, 39, 60);
    private final int CARD2 = Color.rgb(24, 50, 75);
    private final int TEXT = Color.rgb(244, 248, 252);
    private final int MUTED = Color.rgb(164, 184, 202);
    private final int ACCENT = Color.rgb(32, 198, 230);
    private final int LINE = Color.rgb(45, 74, 99);

    private EditText cornerInput, springInput, heightInput, offsetInput, slopedLengthInput, slopedOffsetInput;
    private TextView miterResult, bevelResult, workingResult, formulaInfo, hpResult, lpResult;
    private TextView insideLeft, insideRight, outsideLeft, outsideRight;
    private double lastMiter = 35.26438968;
    private double lastBevel = 30.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(22), dp(16), dp(32));
        scroll.addView(root);

        TextView title = text("Расчет к.у.", 30, TEXT, true);
        root.addView(title);
        TextView subtitle = text("Комбинированный рез кухонного карниза", 14, MUTED, false);
        subtitle.setPadding(0, dp(3), 0, dp(14));
        root.addView(subtitle);

        // 1. MAIN ANGLES
        LinearLayout angles = card();
        root.addView(angles, cardParams());
        angles.addView(sectionTitle("1. ОСНОВНЫЕ УГЛЫ"));
        angles.addView(label("Угол стены / кухни C, °"));
        cornerInput = input("90");
        angles.addView(cornerInput, fieldParams());
        angles.addView(label("Угол установки карниза S, °"));
        springInput = input("45");
        angles.addView(springInput, fieldParams());

        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setWeightSum(3f);
        presetRow.addView(preset("38°", 38), weighted());
        presetRow.addView(preset("45°", 45), weighted());
        presetRow.addView(preset("52°", 52), weighted());
        angles.addView(presetRow);

        Button calc = primary("РАССЧИТАТЬ РЕЗ");
        calc.setOnClickListener(v -> calculate());
        root.addView(calc, buttonParams());

        // 2. RESULT
        LinearLayout result = card();
        root.addView(result, cardParams());
        result.addView(sectionTitle("2. НАСТРОЙКИ / РЕЗУЛЬТАТ РАСЧЁТА РЕЗА"));
        result.addView(labelTop("Поворот стола торцовки"));
        miterResult = bigValue("35,26°");
        result.addView(miterResult);
        result.addView(labelTop("Наклон диска"));
        bevelResult = bigValue("30,00°");
        result.addView(bevelResult);
        result.addView(labelTop("Если карниз зафиксирован в рабочем положении"));
        workingResult = text("Поворот 45,00° · наклон 0°", 16, TEXT, true);
        result.addView(workingResult);
        formulaInfo = text("", 13, MUTED, false);
        formulaInfo.setPadding(0, dp(12), 0, 0);
        formulaInfo.setLineSpacing(0, 1.15f);
        result.addView(formulaInfo);

        // 3. CUT GUIDANCE
        LinearLayout cuts = card();
        root.addView(cuts, cardParams());
        cuts.addView(sectionTitle("3. ЛЕВЫЙ / ПРАВЫЙ РЕЗ"));
        TextView position = text("Положение для подсказок: карниз лежит плашмя, широкая тыльная сторона на столе, ВЕРХ карниза — к упору торцовки.", 13, TEXT, true);
        position.setLineSpacing(0, 1.15f);
        position.setPadding(0, dp(6), 0, dp(10));
        cuts.addView(position);
        insideLeft = cutBlock("ВНУТРЕННИЙ УГОЛ · ЛЕВАЯ ДЕТАЛЬ"); cuts.addView(insideLeft, blockParams());
        insideRight = cutBlock("ВНУТРЕННИЙ УГОЛ · ПРАВАЯ ДЕТАЛЬ"); cuts.addView(insideRight, blockParams());
        outsideLeft = cutBlock("НАРУЖНЫЙ УГОЛ · ЛЕВАЯ ДЕТАЛЬ"); cuts.addView(outsideLeft, blockParams());
        outsideRight = cutBlock("НАРУЖНЫЙ УГОЛ · ПРАВАЯ ДЕТАЛЬ"); cuts.addView(outsideRight, blockParams());

        // 4. SPRING ANGLE FROM DIMENSIONS
        LinearLayout measure = card();
        root.addView(measure, cardParams());
        measure.addView(sectionTitle("4. ВЫЧИСЛИТЬ УГОЛ УСТАНОВКИ ПО РАЗМЕРАМ"));
        TextView a = text("СПОСОБ A · H + P", 14, ACCENT, true);
        a.setPadding(0, dp(10), 0, dp(6)); measure.addView(a);
        measure.addView(help("H — вертикальная высота карниза, P — горизонтальный вылет от вертикальной стены. Формула: S = arctan(P / H)."));
        heightInput = input("100"); offsetInput = input("100");
        measure.addView(label("Высота H, мм")); measure.addView(heightInput, fieldParams());
        measure.addView(label("Вылет P, мм")); measure.addView(offsetInput, fieldParams());
        Button hp = secondary("ВЫЧИСЛИТЬ ПО H + P"); hp.setOnClickListener(v -> springFromHP()); measure.addView(hp, buttonInnerParams());
        hpResult = text("Угол установки: 45,00°", 18, TEXT, true); hpResult.setGravity(Gravity.CENTER); measure.addView(hpResult);

        TextView b = text("СПОСОБ B · L + P", 14, ACCENT, true);
        b.setPadding(0, dp(22), 0, dp(6)); measure.addView(b);
        measure.addView(help("L — длина карниза по наклонной между точками прилегания, P — горизонтальный вылет. Формула: S = arcsin(P / L)."));
        slopedLengthInput = input("141.42"); slopedOffsetInput = input("100");
        measure.addView(label("Длина по наклонной L, мм")); measure.addView(slopedLengthInput, fieldParams());
        measure.addView(label("Вылет P, мм")); measure.addView(slopedOffsetInput, fieldParams());
        Button lp = secondary("ВЫЧИСЛИТЬ ПО L + P"); lp.setOnClickListener(v -> springFromLP()); measure.addView(lp, buttonInnerParams());
        lpResult = text("Угол установки: 45,00°", 18, TEXT, true); lpResult.setGravity(Gravity.CENTER); measure.addView(lpResult);

        // INSTRUCTION
        LinearLayout guide = card();
        root.addView(guide, cardParams());
        guide.addView(sectionTitle("ИНСТРУКЦИЯ"));
        TextView guideText = text(
                "1. Измерь реальный угол стены C — он не всегда ровно 90°.\n\n" +
                "2. Введи угол установки карниза S вручную или вычисли его в пункте 4.\n\n" +
                "3. Нажми «РАССЧИТАТЬ РЕЗ». В пункте 2 появятся поворот стола и наклон диска.\n\n" +
                "4. В пункте 3 выбери нужную строку: внутренний/наружный угол и левая/правая деталь. Там указано направление стола, диска и какую часть сохранить.\n\n" +
                "5. Для комбинированного реза карниз лежит плашмя: широкая тыльная сторона на столе, верх карниза — к упору.\n\n" +
                "6. Способ H + P: H — вертикальная высота, P — вылет от стены. Пример 100/100 мм → S = 45°.\n\n" +
                "7. Способ L + P: L — длина по наклонной, P — вылет. Пример L = 141,42 мм и P = 100 мм → S ≈ 45°.\n\n" +
                "8. Перед чистовым резом обязательно проверь настройку на обрезке. Направление наклона может обозначаться иначе на разных моделях торцовок.",
                14, TEXT, false);
        guideText.setLineSpacing(0, 1.2f);
        guide.addView(guideText);

        TextView warning = text("Работай только с надёжно зафиксированной деталью и соблюдай инструкцию производителя торцовочной пилы.", 12, MUTED, false);
        warning.setPadding(dp(4), 0, dp(4), dp(4)); warning.setLineSpacing(0, 1.15f); root.addView(warning);

        setContentView(scroll);
        calculate();
    }

    private void calculate() {
        try {
            double c = read(cornerInput);
            double s = read(springInput);
            if (c <= 0 || c >= 180) throw new IllegalArgumentException("Угол стены должен быть больше 0° и меньше 180°");
            if (s < 0 || s >= 90) throw new IllegalArgumentException("Угол установки должен быть от 0° до 89,99°");
            double cr = Math.toRadians(c), sr = Math.toRadians(s);
            double m = Math.toDegrees(Math.atan(Math.sin(sr) / Math.tan(cr / 2.0)));
            double x = Math.cos(sr) * Math.cos(cr / 2.0);
            x = Math.max(-1.0, Math.min(1.0, x));
            double b = Math.toDegrees(Math.asin(x));
            double working = (180.0 - c) / 2.0;
            lastMiter = m; lastBevel = b;
            miterResult.setText(fmt(m) + "°");
            bevelResult.setText(fmt(b) + "°");
            workingResult.setText("Поворот " + fmt(working) + "° · наклон 0°");
            formulaInfo.setText("Для C = " + fmt(c) + "° и S = " + fmt(s) + "° при резе плашмя выставь одновременно: поворот " + fmt(m) + "° и наклон " + fmt(b) + "°.");
            updateCuts();
        } catch (Exception e) { toast(e.getMessage()); }
    }

    private void springFromHP() {
        try {
            double h = read(heightInput), p = read(offsetInput);
            if (h <= 0) throw new IllegalArgumentException("Высота H должна быть больше 0 мм");
            if (p < 0) throw new IllegalArgumentException("Вылет P не может быть отрицательным");
            double s = Math.toDegrees(Math.atan(p / h));
            if (s >= 90) throw new IllegalArgumentException("Получился недопустимый угол");
            springInput.setText(fmt(s)); hpResult.setText("Угол установки: " + fmt(s) + "°"); lpResult.setText("Угол установки: " + fmt(s) + "°"); calculate();
        } catch (Exception e) { toast(e.getMessage()); }
    }

    private void springFromLP() {
        try {
            double l = read(slopedLengthInput), p = read(slopedOffsetInput);
            if (l <= 0) throw new IllegalArgumentException("Длина L должна быть больше 0 мм");
            if (p < 0) throw new IllegalArgumentException("Вылет P не может быть отрицательным");
            if (p >= l) throw new IllegalArgumentException("Вылет P должен быть меньше длины L");
            double s = Math.toDegrees(Math.asin(p / l));
            springInput.setText(fmt(s)); lpResult.setText("Угол установки: " + fmt(s) + "°"); hpResult.setText("Угол установки: " + fmt(s) + "°"); calculate();
        } catch (Exception e) { toast(e.getMessage()); }
    }

    private void updateCuts() {
        insideLeft.setText(cutText("ВНУТРЕННИЙ УГОЛ · ЛЕВАЯ ДЕТАЛЬ", "ВПРАВО", "ВЛЕВО", "ЛЕВУЮ"));
        insideRight.setText(cutText("ВНУТРЕННИЙ УГОЛ · ПРАВАЯ ДЕТАЛЬ", "ВЛЕВО", "ВПРАВО", "ПРАВУЮ"));
        outsideLeft.setText(cutText("НАРУЖНЫЙ УГОЛ · ЛЕВАЯ ДЕТАЛЬ", "ВЛЕВО", "ВПРАВО", "ЛЕВУЮ"));
        outsideRight.setText(cutText("НАРУЖНЫЙ УГОЛ · ПРАВАЯ ДЕТАЛЬ", "ВПРАВО", "ВЛЕВО", "ПРАВУЮ"));
    }

    private String cutText(String title, String table, String blade, String keep) {
        return title + "\nСтол: " + table + " " + fmt(lastMiter) + "°\nДиск: " + blade + " " + fmt(lastBevel) + "°\nСохранить: " + keep + " часть";
    }

    private Button preset(String label, double value) {
        Button b = secondary(label);
        b.setOnClickListener(v -> { springInput.setText(fmt(value)); calculate(); });
        return b;
    }

    private double read(EditText e) {
        String s = e.getText().toString().trim().replace(',', '.');
        if (s.isEmpty()) throw new IllegalArgumentException("Заполни все поля");
        double v = Double.parseDouble(s);
        if (!Double.isFinite(v)) throw new IllegalArgumentException("Введите корректное число");
        return v;
    }

    private String fmt(double v) {
        DecimalFormatSymbols sy = new DecimalFormatSymbols(new Locale("ru", "RU")); sy.setDecimalSeparator(',');
        return new DecimalFormat("0.00", sy).format(v);
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16), dp(16), dp(16), dp(16)); l.setBackgroundColor(CARD); return l;
    }
    private TextView sectionTitle(String s) { TextView t = text(s, 13, ACCENT, true); t.setLetterSpacing(.06f); return t; }
    private TextView label(String s) { TextView t = text(s, 13, MUTED, false); t.setPadding(0, dp(12), 0, dp(5)); return t; }
    private TextView labelTop(String s) { TextView t = label(s); t.setPadding(0, dp(15), 0, dp(3)); return t; }
    private TextView help(String s) { TextView t = text(s, 13, MUTED, false); t.setLineSpacing(0, 1.15f); t.setPadding(0, 0, 0, dp(2)); return t; }
    private TextView bigValue(String s) { return text(s, 34, TEXT, true); }
    private TextView cutBlock(String s) { TextView t = text(s, 14, TEXT, true); t.setBackgroundColor(CARD2); t.setPadding(dp(13), dp(12), dp(13), dp(12)); t.setLineSpacing(dp(2), 1.08f); return t; }
    private EditText input(String value) { EditText e = new EditText(this); e.setText(value); e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setTextSize(18); e.setSingleLine(true); e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED); e.setPadding(dp(12), 0, dp(12), 0); e.setBackgroundColor(CARD2); return e; }
    private Button primary(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(Color.rgb(4,25,34)); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackgroundColor(ACCENT); b.setAllCaps(false); return b; }
    private Button secondary(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(TEXT); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackgroundColor(CARD2); b.setAllCaps(false); return b; }
    private TextView text(String s, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t; }
    private LinearLayout.LayoutParams cardParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(13)); return p; }
    private LinearLayout.LayoutParams fieldParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52)); p.setMargins(0, 0, 0, dp(2)); return p; }
    private LinearLayout.LayoutParams buttonParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(56)); p.setMargins(0, 0, 0, dp(13)); return p; }
    private LinearLayout.LayoutParams buttonInnerParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52)); p.setMargins(0, dp(10), 0, dp(8)); return p; }
    private LinearLayout.LayoutParams blockParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(7), 0, 0); return p; }
    private LinearLayout.LayoutParams weighted() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1f); p.setMargins(dp(2), dp(4), dp(2), 0); return p; }
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String s) { Toast.makeText(this, s == null ? "Ошибка" : s, Toast.LENGTH_LONG).show(); }
}
