package com.kurban.cornicecut;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.List;

public final class PlanView extends View {
    private CorniceGeometryEngine.Result result;
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint offsetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PlanView(Context context) {
        super(context);
        basePaint.setColor(Color.rgb(55, 60, 68));
        basePaint.setStrokeWidth(dp(3));
        basePaint.setStyle(Paint.Style.STROKE);
        offsetPaint.setColor(Color.rgb(31, 111, 160));
        offsetPaint.setStrokeWidth(dp(4));
        offsetPaint.setStyle(Paint.Style.STROKE);
        pointPaint.setColor(Color.rgb(31, 111, 160));
        textPaint.setColor(Color.rgb(35, 40, 48));
        textPaint.setTextSize(dp(12));
    }

    public void setResult(CorniceGeometryEngine.Result result) {
        this.result = result;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        if (result == null || result.basePolyline.isEmpty()) return;

        Bounds b = new Bounds();
        includeAll(b, result.basePolyline);
        includeAll(b, result.offsetPolyline);
        double w = Math.max(1, b.maxX - b.minX);
        double h = Math.max(1, b.maxY - b.minY);
        float pad = dp(24);
        float scale = (float) Math.min((getWidth() - pad * 2) / w, (getHeight() - pad * 2) / h);
        if (!Float.isFinite(scale) || scale <= 0) scale = 1;

        drawPolyline(canvas, result.basePolyline, b, scale, pad, basePaint);
        drawPolyline(canvas, result.offsetPolyline, b, scale, pad, offsetPaint);

        for (int i = 0; i < result.offsetPolyline.size(); i++) {
            CorniceGeometryEngine.Point p = result.offsetPolyline.get(i);
            float x = mapX(p.x, b, scale, pad);
            float y = mapY(p.y, b, scale, pad);
            canvas.drawCircle(x, y, dp(4), pointPaint);
        }

        for (CorniceGeometryEngine.SegmentResult s : result.segments) {
            float x = (mapX(s.start.x, b, scale, pad) + mapX(s.end.x, b, scale, pad)) / 2f;
            float y = (mapY(s.start.y, b, scale, pad) + mapY(s.end.y, b, scale, pad)) / 2f;
            canvas.drawText(s.id + "  " + String.format(java.util.Locale.US, "%.1f", s.referenceLength), x + dp(4), y - dp(4), textPaint);
        }
    }

    private void drawPolyline(Canvas c, List<CorniceGeometryEngine.Point> pts, Bounds b, float scale, float pad, Paint paint) {
        if (pts.size() < 2) return;
        Path path = new Path();
        path.moveTo(mapX(pts.get(0).x, b, scale, pad), mapY(pts.get(0).y, b, scale, pad));
        for (int i = 1; i < pts.size(); i++) {
            path.lineTo(mapX(pts.get(i).x, b, scale, pad), mapY(pts.get(i).y, b, scale, pad));
        }
        c.drawPath(path, paint);
    }

    private void includeAll(Bounds b, List<CorniceGeometryEngine.Point> pts) {
        for (CorniceGeometryEngine.Point p : pts) b.add(p.x, p.y);
    }

    private float mapX(double x, Bounds b, float scale, float pad) { return pad + (float)((x - b.minX) * scale); }
    private float mapY(double y, Bounds b, float scale, float pad) { return getHeight() - pad - (float)((y - b.minY) * scale); }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    private static final class Bounds {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        void add(double x, double y) {
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
    }
}
