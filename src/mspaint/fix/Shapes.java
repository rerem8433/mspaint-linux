package mspaint.fix;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import mspaint.gui.GUIHandler;

/** Géométrie et rendu de toutes les formes de la barre « Shapes ». */
public final class Shapes {
    private Shapes() {}

    public static final String[] BOX_SHAPES = {
        "Oval", "Rectangle", "Round Rec", "Triangle", "Right Triangle", "Diamond",
        "Pentagon", "Hexagon", "Right Arrow", "Left Arrow", "Up Arrow", "Down Arrow",
        "4 Point Star", "5 Point Star", "6 Point Star", "Round Rec Callout",
        "Oval Callout", "Cloud Callout", "Heart", "Lightening"
    };

    public static boolean isBoxShape(String tool) {
        if (tool == null) return false;
        for (String s : BOX_SHAPES) if (s.equals(tool)) return true;
        return false;
    }

    /** Forme dans le rectangle englobant (x,y,w,h) — coordonnées image. */
    public static Shape build(String tool, double x, double y, double w, double h) {
        if (w < 1) w = 1;
        if (h < 1) h = 1;
        if ("Oval".equals(tool)) return new Ellipse2D.Double(x, y, w, h);
        if ("Rectangle".equals(tool)) return new Rectangle2D.Double(x, y, w, h);
        if ("Round Rec".equals(tool)) {
            double a = Math.min(w, h) / 3.0;
            return new RoundRectangle2D.Double(x, y, w, h, a, a);
        }
        Shape unit = unitShape(tool);
        AffineTransform t = new AffineTransform();
        t.translate(x, y);
        t.scale(w, h);
        return t.createTransformedShape(unit);
    }

    private static Shape unitShape(String tool) {
        switch (tool) {
            case "Triangle": return poly(0.5, 0, 1, 1, 0, 1);
            case "Right Triangle": return poly(0, 0, 1, 1, 0, 1);
            case "Diamond": return poly(0.5, 0, 1, 0.5, 0.5, 1, 0, 0.5);
            case "Pentagon": return regular(5, -90, 1.0);
            case "Hexagon": return regular(6, 0, 1.0);
            case "Right Arrow": return poly(0, .25, .6, .25, .6, 0, 1, .5, .6, 1, .6, .75, 0, .75);
            case "Left Arrow": return poly(1, .25, .4, .25, .4, 0, 0, .5, .4, 1, .4, .75, 1, .75);
            case "Up Arrow": return poly(.25, 1, .25, .4, 0, .4, .5, 0, 1, .4, .75, .4, .75, 1);
            case "Down Arrow": return poly(.25, 0, .25, .6, 0, .6, .5, 1, 1, .6, .75, .6, .75, 0);
            case "4 Point Star": return star(4, 0.38);
            case "5 Point Star": return star(5, 0.40);
            case "6 Point Star": return star(6, 0.55);
            case "Round Rec Callout": {
                Area a = new Area(new RoundRectangle2D.Double(0, 0, 1, 0.75, 0.25, 0.25));
                a.add(new Area(poly(0.2, 0.7, 0.38, 0.7, 0.12, 1)));
                return a;
            }
            case "Oval Callout": {
                Area a = new Area(new Ellipse2D.Double(0, 0, 1, 0.8));
                a.add(new Area(poly(0.22, 0.62, 0.42, 0.74, 0.12, 1)));
                return a;
            }
            case "Cloud Callout": {
                Area a = new Area();
                double[][] c = {{.08, .18, .38, .38}, {.28, .02, .42, .4}, {.55, .05, .4, .38},
                                {.62, .28, .38, .38}, {.4, .38, .42, .36}, {.1, .36, .42, .36},
                                {.25, .18, .55, .4}};
                for (double[] e : c) a.add(new Area(new Ellipse2D.Double(e[0], e[1], e[2], e[3])));
                a.add(new Area(new Ellipse2D.Double(0.14, 0.78, 0.1, 0.08)));
                a.add(new Area(new Ellipse2D.Double(0.06, 0.92, 0.06, 0.06)));
                return a;
            }
            case "Heart": {
                Path2D p = new Path2D.Double();
                p.moveTo(0.5, 0.25);
                p.curveTo(0.5, 0.0, 0.0, 0.0, 0.0, 0.3);
                p.curveTo(0.0, 0.6, 0.5, 0.8, 0.5, 1.0);
                p.curveTo(0.5, 0.8, 1.0, 0.6, 1.0, 0.3);
                p.curveTo(1.0, 0.0, 0.5, 0.0, 0.5, 0.25);
                p.closePath();
                return p;
            }
            case "Lightening":
                return poly(.38, 0, .62, .3, .52, .35, .8, .6, .7, .65, 1, 1, .42, .72,
                            .54, .67, .22, .46, .34, .41, 0, .14);
            default: return new Rectangle2D.Double(0, 0, 1, 1);
        }
    }

    private static Shape poly(double... xy) {
        GeneralPath p = new GeneralPath();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }

    private static Shape regular(int n, double startDeg, double r) {
        GeneralPath p = new GeneralPath();
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(startDeg + 360.0 * i / n);
            double px = 0.5 + 0.5 * r * Math.cos(a), py = 0.5 + 0.5 * r * Math.sin(a);
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    private static Shape star(int n, double inner) {
        GeneralPath p = new GeneralPath();
        for (int i = 0; i < 2 * n; i++) {
            double a = Math.toRadians(-90 + 180.0 * i / n);
            double r = (i % 2 == 0) ? 0.5 : 0.5 * inner;
            double px = 0.5 + r * Math.cos(a), py = 0.5 + r * Math.sin(a);
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    public static Shape line(double x1, double y1, double x2, double y2) {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    // ---------------- rendu ----------------

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(c.getAlpha(), a));
    }

    private static Stroke brush(float size, float density) {
        try {
            return new com.pump.awt.BrushStroke(size, density);
        } catch (Throwable t) {
            return new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
    }

    /**
     * Dessine la forme selon les options Outline/Fill de la barre d'outils.
     * Contour = Couleur 1, remplissage = Couleur 2 (comme MS Paint).
     */
    public static void paint(Graphics2D g, Shape s, boolean closed, GUIHandler gui) {
        String outline = gui.homeToolBar.shapeTools.activeOutline;
        String fill = gui.homeToolBar.shapeTools.activeFill;
        float size = Math.max(1, gui.activeSize);
        Color c1 = gui.activeColor1 != null ? gui.activeColor1 : Color.BLACK;
        Color c2 = gui.activeColor2 != null ? gui.activeColor2 : Color.WHITE;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setComposite(AlphaComposite.SrcOver);

        boolean doFill = closed && fill != null && !"No Fill".equals(fill);
        boolean doOutline = !closed || !"No Outline".equals(outline) || !doFill;
        if (doFill) {
            int a = "Oil".equals(fill) ? 200 : "Watercolor".equals(fill) ? 110 : 255;
            g.setColor(alpha(c2, a));
            g.fill(s);
            if ("Brush".equals(fill) || "Oil".equals(fill)) {
                g.setColor(alpha(c2, 90));
                g.setStroke(brush(Math.max(4, size * 2), 0.6f));
                g.fill(s);
            }
        }
        if (doOutline) {
            String st = (outline == null || "No Outline".equals(outline)) ? "Solid" : outline;
            BasicStroke solid = new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            switch (st) {
                case "Brush":
                    g.setColor(c1);
                    g.setStroke(brush(size + 1, 0.87f));
                    g.draw(s);
                    break;
                case "Oil":
                    g.setColor(alpha(c1, 200));
                    g.setStroke(solid);
                    g.draw(s);
                    g.setColor(alpha(c1, 120));
                    g.setStroke(brush(size + 2, 0.78f));
                    g.draw(s);
                    break;
                case "Watercolor":
                    g.setColor(alpha(c1, 110));
                    g.setStroke(solid);
                    g.draw(s);
                    g.setColor(alpha(c1, 60));
                    g.setStroke(brush(size + 2, 0.78f));
                    g.draw(s);
                    break;
                default:
                    g.setColor(c1);
                    g.setStroke(solid);
                    g.draw(s);
            }
        }
    }
}
