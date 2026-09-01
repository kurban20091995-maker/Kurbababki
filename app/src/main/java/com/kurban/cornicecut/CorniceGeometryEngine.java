package com.kurban.cornicecut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure geometry: cabinet/front reference polyline -> parallel cornice reference line. */
public final class CorniceGeometryEngine {
    private static final double EPS = 1e-9;

    private CorniceGeometryEngine() {}

    public static final class Point {
        public final double x;
        public final double y;
        public Point(double x, double y) { this.x = x; this.y = y; }
        public Point add(Point o) { return new Point(x + o.x, y + o.y); }
        public Point sub(Point o) { return new Point(x - o.x, y - o.y); }
        public Point mul(double k) { return new Point(x * k, y * k); }
        public double length() { return Math.hypot(x, y); }
    }

    /**
     * cornerAngleAfterDeg is the included corner angle (90 = square corner, 135 = obtuse corner).
     * turnRight applies after this segment. The last segment should use 0 for cornerAngleAfterDeg.
     */
    public static final class SegmentSpec {
        public final String id;
        public final double length;
        public final double cornerAngleAfterDeg;
        public final boolean turnRight;

        public SegmentSpec(String id, double length, double cornerAngleAfterDeg, boolean turnRight) {
            if (!(length > 0)) throw new IllegalArgumentException("Длина участка должна быть больше 0");
            if (cornerAngleAfterDeg != 0 && !(cornerAngleAfterDeg > 0 && cornerAngleAfterDeg < 180)) {
                throw new IllegalArgumentException("Угол должен быть больше 0° и меньше 180°");
            }
            this.id = id;
            this.length = length;
            this.cornerAngleAfterDeg = cornerAngleAfterDeg;
            this.turnRight = turnRight;
        }
    }

    public static final class SegmentResult {
        public final String id;
        public final double baseLength;
        public final double referenceLength;
        public final Point start;
        public final Point end;
        public final double leftCornerAngleDeg;
        public final double rightCornerAngleDeg;
        public final Boolean leftTurnRight;
        public final Boolean rightTurnRight;

        SegmentResult(String id, double baseLength, double referenceLength, Point start, Point end,
                      double leftCornerAngleDeg, double rightCornerAngleDeg,
                      Boolean leftTurnRight, Boolean rightTurnRight) {
            this.id = id;
            this.baseLength = baseLength;
            this.referenceLength = referenceLength;
            this.start = start;
            this.end = end;
            this.leftCornerAngleDeg = leftCornerAngleDeg;
            this.rightCornerAngleDeg = rightCornerAngleDeg;
            this.leftTurnRight = leftTurnRight;
            this.rightTurnRight = rightTurnRight;
        }
    }

    public static final class Result {
        public final List<Point> basePolyline;
        public final List<Point> offsetPolyline;
        public final List<SegmentResult> segments;
        public final double effectiveOverhang;
        public final boolean offsetLeft;

        Result(List<Point> basePolyline, List<Point> offsetPolyline, List<SegmentResult> segments,
               double effectiveOverhang, boolean offsetLeft) {
            this.basePolyline = Collections.unmodifiableList(basePolyline);
            this.offsetPolyline = Collections.unmodifiableList(offsetPolyline);
            this.segments = Collections.unmodifiableList(segments);
            this.effectiveOverhang = effectiveOverhang;
            this.offsetLeft = offsetLeft;
        }
    }

    private static final class Line {
        final Point p;
        final Point d;
        Line(Point p, Point d) { this.p = p; this.d = d; }
    }

    public static Result calculate(List<SegmentSpec> specs, double effectiveOverhang,
                                   boolean offsetLeft, double startExtension, double endExtension) {
        if (specs == null || specs.isEmpty()) throw new IllegalArgumentException("Добавьте участки кухни");
        if (effectiveOverhang < 0 || startExtension < 0 || endExtension < 0) {
            throw new IllegalArgumentException("Вылет и удлинения не могут быть отрицательными");
        }

        List<Point> base = new ArrayList<>();
        List<Point> dirs = new ArrayList<>();
        List<Line> offsetLines = new ArrayList<>();
        base.add(new Point(0, 0));

        double headingDeg = 0;
        for (int i = 0; i < specs.size(); i++) {
            SegmentSpec s = specs.get(i);
            double a = Math.toRadians(headingDeg);
            Point d = new Point(Math.cos(a), Math.sin(a));
            dirs.add(d);
            Point next = base.get(base.size() - 1).add(d.mul(s.length));
            base.add(next);

            if (i < specs.size() - 1) {
                if (s.cornerAngleAfterDeg == 0) {
                    throw new IllegalArgumentException("У участка " + s.id + " не указан угол поворота до следующего участка");
                }
                double headingChange = 180.0 - s.cornerAngleAfterDeg;
                headingDeg += s.turnRight ? -headingChange : headingChange;
            }
        }

        for (int i = 0; i < specs.size(); i++) {
            Point d = dirs.get(i);
            Point n = offsetLeft ? new Point(-d.y, d.x) : new Point(d.y, -d.x);
            Point shifted = base.get(i).add(n.mul(effectiveOverhang));
            offsetLines.add(new Line(shifted, d));
        }

        List<Point> offset = new ArrayList<>();
        Point first = offsetLines.get(0).p.sub(dirs.get(0).mul(startExtension));
        offset.add(first);

        for (int i = 1; i < specs.size(); i++) {
            Point hit = intersect(offsetLines.get(i - 1), offsetLines.get(i));
            if (hit == null) {
                // Nearly straight segments: use the end of the preceding shifted segment.
                Point d = dirs.get(i - 1);
                Point n = offsetLeft ? new Point(-d.y, d.x) : new Point(d.y, -d.x);
                hit = base.get(i).add(n.mul(effectiveOverhang));
            }
            offset.add(hit);
        }

        int lastIndex = specs.size() - 1;
        Point dLast = dirs.get(lastIndex);
        Point nLast = offsetLeft ? new Point(-dLast.y, dLast.x) : new Point(dLast.y, -dLast.x);
        Point last = base.get(base.size() - 1).add(nLast.mul(effectiveOverhang)).add(dLast.mul(endExtension));
        offset.add(last);

        List<SegmentResult> results = new ArrayList<>();
        for (int i = 0; i < specs.size(); i++) {
            SegmentSpec s = specs.get(i);
            double len = offset.get(i + 1).sub(offset.get(i)).length();
            double leftAngle = i > 0 ? specs.get(i - 1).cornerAngleAfterDeg : 0;
            double rightAngle = i < specs.size() - 1 ? s.cornerAngleAfterDeg : 0;
            Boolean leftRight = i > 0 ? specs.get(i - 1).turnRight : null;
            Boolean rightRight = i < specs.size() - 1 ? s.turnRight : null;
            results.add(new SegmentResult(s.id, s.length, len, offset.get(i), offset.get(i + 1),
                    leftAngle, rightAngle, leftRight, rightRight));
        }
        return new Result(base, offset, results, effectiveOverhang, offsetLeft);
    }

    private static Point intersect(Line a, Line b) {
        double cross = cross(a.d, b.d);
        if (Math.abs(cross) < EPS) return null;
        Point qmp = b.p.sub(a.p);
        double t = cross(qmp, b.d) / cross;
        return a.p.add(a.d.mul(t));
    }

    private static double cross(Point a, Point b) {
        return a.x * b.y - a.y * b.x;
    }
}
