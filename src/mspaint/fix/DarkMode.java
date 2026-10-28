package mspaint.fix;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import mspaint.gui.GUIHandler;

/**
 * Mode sombre : chaque fenêtre reçoit un JLayeredPane qui peint ses enfants à travers
 * DarkGraphics (inversion de la luminosité en gardant la teinte).
 * Le dessin lui-même (canevas) et les cases de la palette ne sont jamais modifiés.
 */
public final class DarkMode {
    static volatile boolean dark;
    private static GUIHandler gui;
    private static final Map<Integer, Color> colorCache = Collections.synchronizedMap(new java.util.HashMap<Integer, Color>());
    private static final Map<Image, Image> imageCache = Collections.synchronizedMap(new WeakHashMap<Image, Image>());
    private static final List<Component> excluded = new ArrayList<>();
    private static JButton sunButton;

    private DarkMode() {}

    private static Preferences prefs() {
        return Preferences.userRoot().node("mspaint-linux");
    }

    static void install(GUIHandler g) {
        gui = g;
        // zones jamais converties
        excluded.add(g.holder);
        Object btns = Fixes.get(g.homeToolBar.colorPlate, "button");
        if (btns instanceof Object[]) for (Object b : (Object[]) btns) if (b instanceof Component) excluded.add((Component) b);

        wrap(g.frame);
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e.getID() == WindowEvent.WINDOW_OPENED && e.getSource() instanceof Window) {
                    wrap((Window) e.getSource());
                }
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
        addSunButton();
        boolean d = false;
        try { d = prefs().getBoolean("dark", false); } catch (Throwable ignore) { }
        setDark(d);
    }

    /** Remplace le JLayeredPane de la fenêtre (contenu + popups légers + infobulles). */
    static void wrap(Window w) {
        if (!(w instanceof RootPaneContainer)) return;
        JRootPane rp = ((RootPaneContainer) w).getRootPane();
        if (rp == null || rp.getLayeredPane() instanceof DarkLayeredPane) return;
        JLayeredPane old = rp.getLayeredPane();
        DarkLayeredPane n = new DarkLayeredPane();
        Component[] cs = old.getComponents();
        for (int i = cs.length - 1; i >= 0; i--) {
            Component c = cs[i];
            int layer = old.getLayer(c);
            old.remove(c);
            n.add(c, Integer.valueOf(layer));
        }
        rp.setLayeredPane(n);
        rp.revalidate();
        rp.repaint();
    }

    public static boolean isDark() { return dark; }

    public static void setDark(boolean d) {
        dark = d;
        try { prefs().putBoolean("dark", d); prefs().flush(); } catch (Throwable ignore) { }
        if (sunButton != null) sunButton.setToolTipText(d ? "Passer en mode clair" : "Passer en mode sombre");
        for (Window w : Window.getWindows()) {
            wrap(w);
            w.repaint();
        }
    }

    // ------------------------------------------------------------------
    // Conversion des couleurs : luminosité inversée, teinte conservée
    // ------------------------------------------------------------------

    static Color map(Color c) {
        if (c == null) return null;
        int argb = c.getRGB();
        Color m = colorCache.get(argb);
        if (m == null) {
            m = new Color(mapRGB(argb), true);
            colorCache.put(argb, m);
        }
        return m;
    }

    static int mapRGB(int argb) {
        int a = argb >>> 24;
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f, h = 0, s = 0;
        if (max != min) {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) h = (g - b) / d + (g < b ? 6 : 0);
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6f;
        }
        // blanc -> gris très sombre, noir -> gris très clair
        float nl = 0.11f + (1f - l) * 0.80f;
        // couleurs vives de taille moyenne (icônes, onglet Fichier) : on les garde proches
        if (s > 0.45f && l > 0.3f && l < 0.7f) nl = l;
        return (a << 24) | hslToRgb(h, s, nl);
    }

    private static int hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hue(p, q, h + 1f / 3);
            g = hue(p, q, h);
            b = hue(p, q, h - 1f / 3);
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private static float hue(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6) return p + (q - p) * 6 * t;
        if (t < 1f / 2) return q;
        if (t < 2f / 3) return p + (q - p) * (2f / 3 - t) * 6;
        return p;
    }

    /** Icônes : on éclaircit les pixels gris/sombres, on garde les pixels colorés. */
    static Image mapImage(Image img) {
        if (img == null || gui == null) return img;
        if (img == gui.tempImage || img == gui.shapeImage || img == gui.mainImage) return img;
        for (int i = 0; i < gui.layers.size(); i++) if (gui.layers.get(i).image == img) return img;
        Image cached = imageCache.get(img);
        if (cached != null) return cached;
        int w = img.getWidth(null), h = img.getHeight(null);
        if (w <= 0 || h <= 0 || w > 160 || h > 160) return img;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        boolean ok = g.drawImage(img, 0, 0, null);
        g.dispose();
        if (!ok) return img;
        int[] px = out.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            if ((p >>> 24) == 0) continue;
            int r = (p >> 16) & 255, gg = (p >> 8) & 255, b = p & 255;
            int max = Math.max(r, Math.max(gg, b)), min = Math.min(r, Math.min(gg, b));
            float l = (max + min) / 510f;
            float sat = max == 0 ? 0 : (max - min) / (float) max;
            if (sat < 0.2f || l < 0.3f) px[i] = mapRGB(p);
        }
        out.setRGB(0, 0, w, h, px, 0, w);
        imageCache.put(img, out);
        return out;
    }

    // ------------------------------------------------------------------
    // Contexte d'un rendu : repère du JLayeredPane + zones exclues
    // ------------------------------------------------------------------

    static final class Ctx {
        final AffineTransform inv;
        final JComponent root;
        final List<Rectangle> ex = new ArrayList<>();
        final List<Component> exComp = new ArrayList<>();
        final List<Rectangle> swRect = new ArrayList<>();
        final List<mspaint.gui.toolbar.home.ColorPlateTool.ColorButton> swBtn = new ArrayList<>();

        Ctx(JComponent root, AffineTransform base) {
            AffineTransform i;
            try { i = base.createInverse(); } catch (Exception e) { i = new AffineTransform(); }
            inv = i;
            this.root = root;
            for (Component c : excluded) {
                if (c == null || !c.isShowing() || c.getParent() == null) continue;
                if (!SwingUtilities.isDescendingFrom(c, root)) continue;
                ex.add(SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), root));
                exComp.add(c);
            }
            if (gui != null) {
                for (mspaint.gui.toolbar.home.ColorPlateTool.ColorButton b : new mspaint.gui.toolbar.home.ColorPlateTool.ColorButton[]{
                        gui.homeToolBar.colorPlate.color1Button, gui.homeToolBar.colorPlate.color2Button}) {
                    if (b == null || !b.isShowing() || !SwingUtilities.isDescendingFrom(b, root)) continue;
                    swRect.add(SwingUtilities.convertRectangle(b.getParent(), b.getBounds(), root));
                    swBtn.add(b);
                }
            }
        }

        /** Bouton « Couleur 1/2 » dont c'est la zone : sa pastille garde la vraie couleur. */
        mspaint.gui.toolbar.home.ColorPlateTool.ColorButton swatch(AffineTransform childTx, int w, int h) {
            if (swRect.isEmpty()) return null;
            Point2D p = inv.transform(childTx.transform(new Point2D.Double(0, 0), null), null);
            int x = (int) Math.round(p.getX()), y = (int) Math.round(p.getY());
            for (int i = 0; i < swRect.size(); i++) {
                Rectangle r = swRect.get(i);
                if (Math.abs(x - r.x) <= 1 && Math.abs(y - r.y) <= 1 && Math.abs(w - r.width) <= 1 && Math.abs(h - r.height) <= 1)
                    return swBtn.get(i);
            }
            return null;
        }

        boolean excluded(AffineTransform childTx, int w, int h) {
            if (ex.isEmpty()) return false;
            Point2D p = inv.transform(childTx.transform(new Point2D.Double(0, 0), null), null);
            int x = (int) Math.round(p.getX()), y = (int) Math.round(p.getY());
            for (int i = 0; i < ex.size(); i++) {
                Rectangle r = ex.get(i);
                if (x >= r.x - 1 && y >= r.y - 1 && x + w <= r.x + r.width + 1 && y + h <= r.y + r.height + 1) {
                    // vérifier que c'est bien ce composant qui est dessiné ici (pas un menu par-dessus)
                    Component top = SwingUtilities.getDeepestComponentAt(root, x + Math.max(0, w / 2), y + Math.max(0, h / 2));
                    if (top != null && SwingUtilities.isDescendingFrom(top, exComp.get(i))) return true;
                }
            }
            return false;
        }
    }

    static final class DarkLayeredPane extends JLayeredPane {
        @Override public boolean isPaintingOrigin() { return dark || super.isPaintingOrigin(); }

        @Override public void paint(Graphics g) {
            if (!dark || !(g instanceof Graphics2D) || g instanceof DarkGraphics) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            Ctx ctx = new Ctx(this, g2.getTransform());
            DarkGraphics dg = new DarkGraphics(g2, ctx);
            try {
                super.paint(dg);
            } finally {
                dg.dispose();
            }
        }
    }

    // ------------------------------------------------------------------
    // Bouton soleil en haut à droite
    // ------------------------------------------------------------------

    private static void addSunButton() {
        Object o = Fixes.get(gui, "topMenubar");
        if (!(o instanceof JPanel)) return;
        JPanel bar = (JPanel) o;
        final JButton b = new JButton(new SunIcon());
        sunButton = b;
        b.setFocusable(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setMargin(new Insets(0, 6, 0, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(34, 23));
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { setDark(!dark); }
        });
        Container parent = bar.getParent();
        if (parent != null && parent.getLayout() instanceof BorderLayout) {
            BorderLayout bl = (BorderLayout) parent.getLayout();
            Object cons = bl.getConstraints(bar);
            parent.remove(bar);
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(bar.isOpaque());
            row.setBackground(bar.getBackground());
            row.add(bar, BorderLayout.CENTER);
            row.add(b, BorderLayout.EAST);
            parent.add(row, cons == null ? BorderLayout.NORTH : cons);
            parent.revalidate();
        } else {
            bar.add(b);
            bar.revalidate();
        }
    }

    /** Soleil (mode clair) / lune (mode sombre), dessiné en vectoriel. */
    static final class SunIcon implements Icon {
        public int getIconWidth() { return 18; }
        public int getIconHeight() { return 18; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            Color sun = new Color(245, 160, 20);
            g.setColor(sun);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (!dark) {
                g.fillOval(5, 5, 8, 8);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4;
                    g.drawLine((int) Math.round(9 + 6 * Math.cos(a)), (int) Math.round(9 + 6 * Math.sin(a)),
                               (int) Math.round(9 + 8 * Math.cos(a)), (int) Math.round(9 + 8 * Math.sin(a)));
                }
            } else {
                // soleil plein, plus lumineux, sur fond sombre
                g.setColor(new Color(255, 200, 40));
                g.fillOval(4, 4, 10, 10);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4 + Math.PI / 8;
                    g.drawLine((int) Math.round(9 + 7 * Math.cos(a)), (int) Math.round(9 + 7 * Math.sin(a)),
                               (int) Math.round(9 + 8.6 * Math.cos(a)), (int) Math.round(9 + 8.6 * Math.sin(a)));
                }
            }
            g.dispose();
        }
    }
}
