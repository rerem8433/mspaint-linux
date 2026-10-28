package mspaint.fix;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Graphics2D qui convertit toutes les couleurs clair -> sombre à la volée.
 * Les zones exclues (canevas, cases de la palette) reçoivent un Graphics normal.
 */
final class DarkGraphics extends Graphics2D {
    private final Graphics2D g;
    private final DarkMode.Ctx ctx;
    private Color color;
    private Paint paint;
    private Color background;
    private final mspaint.gui.toolbar.home.ColorPlateTool.ColorButton swatch;

    DarkGraphics(Graphics2D g, DarkMode.Ctx ctx) { this(g, ctx, null); }

    DarkGraphics(Graphics2D g, DarkMode.Ctx ctx, mspaint.gui.toolbar.home.ColorPlateTool.ColorButton swatch) {
        this.g = g;
        this.ctx = ctx;
        this.swatch = swatch;
        this.color = g.getColor();
        this.paint = g.getPaint();
        this.background = g.getBackground();
        // la couleur courante héritée doit aussi être convertie
        g.setColor(DarkMode.map(color));
        if (!(paint instanceof Color)) g.setPaint(mapPaint(paint));
        g.setBackground(DarkMode.map(background));
    }

    private static Paint mapPaint(Paint p) {
        if (p instanceof Color) return DarkMode.map((Color) p);
        if (p instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) p;
            return new GradientPaint(gp.getPoint1(), DarkMode.map(gp.getColor1()),
                                     gp.getPoint2(), DarkMode.map(gp.getColor2()), gp.isCyclic());
        }
        if (p instanceof LinearGradientPaint) {
            LinearGradientPaint lp = (LinearGradientPaint) p;
            Color[] cs = lp.getColors().clone();
            for (int i = 0; i < cs.length; i++) cs[i] = DarkMode.map(cs[i]);
            return new LinearGradientPaint(lp.getStartPoint(), lp.getEndPoint(), lp.getFractions(), cs,
                                           lp.getCycleMethod(), lp.getColorSpace(), lp.getTransform());
        }
        return p;
    }

    private Image img(Image i) { return DarkMode.mapImage(i); }

    // ----- création / exclusions -----
    @Override public Graphics create() {
        Graphics2D n = (Graphics2D) g.create();
        n.setColor(color);
        n.setPaint(paint);
        n.setBackground(background);
        return new DarkGraphics(n, ctx, swatch);
    }

    @Override public Graphics create(int x, int y, int w, int h) {
        Graphics2D n = (Graphics2D) g.create(x, y, w, h);
        n.setColor(color);
        n.setPaint(paint);
        n.setBackground(background);
        if (ctx.excluded(n.getTransform(), w, h)) return n;
        mspaint.gui.toolbar.home.ColorPlateTool.ColorButton sw = ctx.swatch(n.getTransform(), w, h);
        return new DarkGraphics(n, ctx, sw != null ? sw : swatch);
    }

    // ----- couleurs -----
    @Override public Color getColor() { return color; }
    @Override public void setColor(Color c) {
        color = c;
        paint = c;
        if (swatch != null && c != null && c == swatch.getColor()) g.setColor(c);
        else g.setColor(DarkMode.map(c));
    }
    @Override public Paint getPaint() { return paint; }
    @Override public void setPaint(Paint p) {
        paint = p;
        if (p instanceof Color) color = (Color) p;
        g.setPaint(mapPaint(p));
    }
    @Override public Color getBackground() { return background; }
    @Override public void setBackground(Color c) { background = c; g.setBackground(DarkMode.map(c)); }
    @Override public void setXORMode(Color c) { g.setXORMode(DarkMode.map(c)); }
    @Override public void setPaintMode() { g.setPaintMode(); }

    // ----- délégation simple -----
    @Override public void draw(Shape s) { g.draw(s); }
    @Override public void fill(Shape s) { g.fill(s); }
    @Override public boolean hit(Rectangle r, Shape s, boolean onStroke) { return g.hit(r, s, onStroke); }
    @Override public GraphicsConfiguration getDeviceConfiguration() { return g.getDeviceConfiguration(); }
    @Override public void setComposite(Composite c) { g.setComposite(c); }
    @Override public Composite getComposite() { return g.getComposite(); }
    @Override public void setStroke(Stroke s) { g.setStroke(s); }
    @Override public Stroke getStroke() { return g.getStroke(); }
    @Override public void setRenderingHint(RenderingHints.Key k, Object v) { g.setRenderingHint(k, v); }
    @Override public Object getRenderingHint(RenderingHints.Key k) { return g.getRenderingHint(k); }
    @Override public void setRenderingHints(Map<?, ?> m) { g.setRenderingHints(m); }
    @Override public void addRenderingHints(Map<?, ?> m) { g.addRenderingHints(m); }
    @Override public RenderingHints getRenderingHints() { return g.getRenderingHints(); }
    @Override public void translate(int x, int y) { g.translate(x, y); }
    @Override public void translate(double x, double y) { g.translate(x, y); }
    @Override public void rotate(double t) { g.rotate(t); }
    @Override public void rotate(double t, double x, double y) { g.rotate(t, x, y); }
    @Override public void scale(double x, double y) { g.scale(x, y); }
    @Override public void shear(double x, double y) { g.shear(x, y); }
    @Override public void transform(AffineTransform t) { g.transform(t); }
    @Override public void setTransform(AffineTransform t) { g.setTransform(t); }
    @Override public AffineTransform getTransform() { return g.getTransform(); }
    @Override public void clip(Shape s) { g.clip(s); }
    @Override public FontRenderContext getFontRenderContext() { return g.getFontRenderContext(); }
    @Override public Font getFont() { return g.getFont(); }
    @Override public void setFont(Font f) { g.setFont(f); }
    @Override public FontMetrics getFontMetrics(Font f) { return g.getFontMetrics(f); }
    @Override public Rectangle getClipBounds() { return g.getClipBounds(); }
    @Override public void clipRect(int x, int y, int w, int h) { g.clipRect(x, y, w, h); }
    @Override public void setClip(int x, int y, int w, int h) { g.setClip(x, y, w, h); }
    @Override public Shape getClip() { return g.getClip(); }
    @Override public void setClip(Shape s) { g.setClip(s); }
    @Override public void copyArea(int x, int y, int w, int h, int dx, int dy) { g.copyArea(x, y, w, h, dx, dy); }
    @Override public void drawLine(int a, int b, int c, int d) { g.drawLine(a, b, c, d); }
    @Override public void fillRect(int x, int y, int w, int h) { g.fillRect(x, y, w, h); }
    @Override public void clearRect(int x, int y, int w, int h) { g.clearRect(x, y, w, h); }
    @Override public void drawRoundRect(int x, int y, int w, int h, int a, int b) { g.drawRoundRect(x, y, w, h, a, b); }
    @Override public void fillRoundRect(int x, int y, int w, int h, int a, int b) { g.fillRoundRect(x, y, w, h, a, b); }
    @Override public void drawOval(int x, int y, int w, int h) { g.drawOval(x, y, w, h); }
    @Override public void fillOval(int x, int y, int w, int h) { g.fillOval(x, y, w, h); }
    @Override public void drawArc(int x, int y, int w, int h, int s, int a) { g.drawArc(x, y, w, h, s, a); }
    @Override public void fillArc(int x, int y, int w, int h, int s, int a) { g.fillArc(x, y, w, h, s, a); }
    @Override public void drawPolyline(int[] x, int[] y, int n) { g.drawPolyline(x, y, n); }
    @Override public void drawPolygon(int[] x, int[] y, int n) { g.drawPolygon(x, y, n); }
    @Override public void fillPolygon(int[] x, int[] y, int n) { g.fillPolygon(x, y, n); }
    @Override public void drawString(String s, int x, int y) { g.drawString(s, x, y); }
    @Override public void drawString(String s, float x, float y) { g.drawString(s, x, y); }
    @Override public void drawString(AttributedCharacterIterator it, int x, int y) { g.drawString(it, x, y); }
    @Override public void drawString(AttributedCharacterIterator it, float x, float y) { g.drawString(it, x, y); }
    @Override public void drawGlyphVector(GlyphVector v, float x, float y) { g.drawGlyphVector(v, x, y); }
    @Override public void dispose() { g.dispose(); }

    // ----- images (icônes converties, images de dessin intactes) -----
    @Override public boolean drawImage(Image i, AffineTransform t, ImageObserver o) { return g.drawImage(img(i), t, o); }
    @Override public void drawImage(BufferedImage i, BufferedImageOp op, int x, int y) {
        Image m = img(i);
        if (m instanceof BufferedImage) g.drawImage((BufferedImage) m, op, x, y);
        else g.drawImage(i, op, x, y);
    }
    @Override public void drawRenderedImage(RenderedImage i, AffineTransform t) { g.drawRenderedImage(i, t); }
    @Override public void drawRenderableImage(RenderableImage i, AffineTransform t) { g.drawRenderableImage(i, t); }
    @Override public boolean drawImage(Image i, int x, int y, ImageObserver o) { return g.drawImage(img(i), x, y, o); }
    @Override public boolean drawImage(Image i, int x, int y, int w, int h, ImageObserver o) { return g.drawImage(img(i), x, y, w, h, o); }
    @Override public boolean drawImage(Image i, int x, int y, Color b, ImageObserver o) { return g.drawImage(img(i), x, y, DarkMode.map(b), o); }
    @Override public boolean drawImage(Image i, int x, int y, int w, int h, Color b, ImageObserver o) { return g.drawImage(img(i), x, y, w, h, DarkMode.map(b), o); }
    @Override public boolean drawImage(Image i, int a, int b, int c, int d, int e, int f, int h, int j, ImageObserver o) {
        return g.drawImage(img(i), a, b, c, d, e, f, h, j, o);
    }
    @Override public boolean drawImage(Image i, int a, int b, int c, int d, int e, int f, int h, int j, Color bg, ImageObserver o) {
        return g.drawImage(img(i), a, b, c, d, e, f, h, j, DarkMode.map(bg), o);
    }
}
