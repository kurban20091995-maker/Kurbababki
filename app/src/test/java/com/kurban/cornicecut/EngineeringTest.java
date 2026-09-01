package com.kurban.cornicecut;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class EngineeringTest {
    @Test public void straightZeroOverhangKeepsLength() {
        List<CorniceGeometryEngine.SegmentSpec> s = Arrays.asList(
                new CorniceGeometryEngine.SegmentSpec("A", 1000, 0, false));
        CorniceGeometryEngine.Result r = CorniceGeometryEngine.calculate(s, 0, true, 0, 0);
        assertEquals(1000.0, r.segments.get(0).referenceLength, 1e-7);
    }

    @Test public void right90WithLeftOffsetExtendsBothSegments() {
        List<CorniceGeometryEngine.SegmentSpec> s = Arrays.asList(
                new CorniceGeometryEngine.SegmentSpec("A", 1000, 90, true),
                new CorniceGeometryEngine.SegmentSpec("B", 1000, 0, false));
        CorniceGeometryEngine.Result r = CorniceGeometryEngine.calculate(s, 60, true, 0, 0);
        assertEquals(1060.0, r.segments.get(0).referenceLength, 1e-6);
        assertEquals(1060.0, r.segments.get(1).referenceLength, 1e-6);
        assertEquals(1060.0, r.offsetPolyline.get(1).x, 1e-6);
        assertEquals(60.0, r.offsetPolyline.get(1).y, 1e-6);
    }

    @Test public void left90WithLeftOffsetShortensBothSegments() {
        List<CorniceGeometryEngine.SegmentSpec> s = Arrays.asList(
                new CorniceGeometryEngine.SegmentSpec("A", 1000, 90, false),
                new CorniceGeometryEngine.SegmentSpec("B", 1000, 0, false));
        CorniceGeometryEngine.Result r = CorniceGeometryEngine.calculate(s, 60, true, 0, 0);
        assertEquals(940.0, r.segments.get(0).referenceLength, 1e-6);
        assertEquals(940.0, r.segments.get(1).referenceLength, 1e-6);
    }

    @Test public void obtuse135UsesLineIntersectionNotFixedAddition() {
        List<CorniceGeometryEngine.SegmentSpec> s = Arrays.asList(
                new CorniceGeometryEngine.SegmentSpec("A", 1000, 135, true),
                new CorniceGeometryEngine.SegmentSpec("B", 1000, 0, false));
        CorniceGeometryEngine.Result r = CorniceGeometryEngine.calculate(s, 60, true, 0, 0);
        double expected = 1000.0 + 60.0 * Math.tan(Math.toRadians(22.5));
        assertEquals(expected, r.segments.get(0).referenceLength, 1e-5);
        assertEquals(expected, r.segments.get(1).referenceLength, 1e-5);
    }

    @Test public void compound38At90MatchesControl() {
        CompoundCutCalculator.Result r = CompoundCutCalculator.calculate(38, 90);
        assertEquals(31.62, r.miterDeg, 0.02);
        assertEquals(33.86, r.bevelDeg, 0.02);
    }

    @Test public void compound45At90MatchesControl() {
        CompoundCutCalculator.Result r = CompoundCutCalculator.calculate(45, 90);
        assertEquals(35.26, r.miterDeg, 0.02);
        assertEquals(30.00, r.bevelDeg, 0.02);
    }

    @Test public void triangleAnglesComplementEachOther() {
        double v = CompoundCutCalculator.angleFromVertical(60, 80);
        double h = CompoundCutCalculator.angleFromHorizontal(60, 80);
        assertEquals(90.0, v + h, 1e-9);
    }

    @Test public void kerfConsumptionCanRequireExtraBar() {
        List<StockCutOptimizer.CutPiece> p = Arrays.asList(
                new StockCutOptimizer.CutPiece("A", 1000, 1003),
                new StockCutOptimizer.CutPiece("B", 1000, 1003),
                new StockCutOptimizer.CutPiece("C", 1000, 1003));
        List<StockCutOptimizer.StockPiece> s = Arrays.asList(
                new StockCutOptimizer.StockPiece("H1", 3000, false),
                new StockCutOptimizer.StockPiece("H2", 3000, false));
        StockCutOptimizer.Solution r = StockCutOptimizer.optimize(p, s, 2000);
        assertEquals(2, r.bars.size());
    }

    @Test public void optimizerFindsTwoPerfectBars() {
        List<StockCutOptimizer.CutPiece> p = Arrays.asList(
                new StockCutOptimizer.CutPiece("A", 1500, 1500),
                new StockCutOptimizer.CutPiece("B", 1500, 1500),
                new StockCutOptimizer.CutPiece("C", 500, 500),
                new StockCutOptimizer.CutPiece("D", 500, 500));
        List<StockCutOptimizer.StockPiece> s = Arrays.asList(
                new StockCutOptimizer.StockPiece("H1", 2000, false),
                new StockCutOptimizer.StockPiece("H2", 2000, false),
                new StockCutOptimizer.StockPiece("H3", 2000, false));
        StockCutOptimizer.Solution r = StockCutOptimizer.optimize(p, s, 2000);
        assertEquals(2, r.bars.size());
        assertEquals(0.0, r.totalWaste, 1e-7);
    }
}
