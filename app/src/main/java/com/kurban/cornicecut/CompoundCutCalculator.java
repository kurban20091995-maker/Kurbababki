package com.kurban.cornicecut;

/** Compound-miter math for crown/cornice cut flat on the saw table. */
public final class CompoundCutCalculator {
    private CompoundCutCalculator() {}

    public static final class Result {
        public final double miterDeg;
        public final double bevelDeg;
        Result(double miterDeg, double bevelDeg) {
            this.miterDeg = miterDeg;
            this.bevelDeg = bevelDeg;
        }
    }

    public static Result calculate(double springAngleDeg, double cornerAngleDeg) {
        if (!(springAngleDeg > 0 && springAngleDeg < 90)) {
            throw new IllegalArgumentException("Угол наклона карниза должен быть между 0° и 90°");
        }
        if (!(cornerAngleDeg > 0 && cornerAngleDeg < 180)) {
            throw new IllegalArgumentException("Угол между участками должен быть между 0° и 180°");
        }

        double s = Math.toRadians(springAngleDeg);
        double c = Math.toRadians(cornerAngleDeg);
        double miter = Math.atan(Math.sin(s) / Math.tan(c / 2.0));
        double x = Math.cos(s) * Math.cos(c / 2.0);
        x = Math.max(-1.0, Math.min(1.0, x));
        double bevel = Math.asin(x);
        return new Result(Math.toDegrees(miter), Math.toDegrees(bevel));
    }

    public static double angleFromVertical(double horizontal, double vertical) {
        if (!(horizontal >= 0) || !(vertical > 0)) throw new IllegalArgumentException("Проверь размеры треугольника");
        return Math.toDegrees(Math.atan(horizontal / vertical));
    }

    public static double angleFromHorizontal(double horizontal, double vertical) {
        if (!(horizontal > 0) || !(vertical >= 0)) throw new IllegalArgumentException("Проверь размеры треугольника");
        return Math.toDegrees(Math.atan(vertical / horizontal));
    }

    /**
     * Human-readable directions under one explicit convention. The user still must verify on scrap:
     * decorative face up, profile lying flat, pieces listed in route order.
     */
    public static String directionHint(boolean turnRight, boolean firstPieceRightEnd) {
        boolean tableRight = turnRight == firstPieceRightEnd;
        String table = tableRight ? "ВПРАВО" : "ВЛЕВО";
        String bevel = tableRight ? "ВЛЕВО" : "ВПРАВО";
        return "стол " + table + ", диск " + bevel;
    }
}
