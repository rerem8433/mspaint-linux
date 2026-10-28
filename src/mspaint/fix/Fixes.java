package mspaint.fix;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mspaint.action.MainViewAction;
import mspaint.action.file.FileMenuAction;
import mspaint.gui.GUIHandler;
import mspaint.gui.sidebar.LayersArea;
import mspaint.gui.toolbar.text.BackgroundTools;
import mspaint.gui.toolbar.text.FontTool;
import mspaint.helper.Layer;
import mspaint.helper.TButton;

/** Corrections du portage : outils manquants, Annuler/Rétablir, raccourcis. */
public final class Fixes {
    static GUIHandler gui;

    private Fixes() {}

    // =====================================================================
    // Installation
    // =====================================================================

    public static void install(GUIHandler g) {
        gui = g;
        fixToolNames();
        installInterceptor();
        hookImageTools();
        hookTextToolbar();
        installShortcuts();
        addUndoButtons();
        setWindowIcon();
        fixColorButtons();
        Undo.install(g);
        French.apply(g);
        Recent.refresh();
        DarkMode.install(g);
        // termine texte/polygone/courbe si l'utilisateur change d'outil
        new Timer(200, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pendingTool != null && !pendingTool.equals(gui.activeToolName)) finishPending();
            }
        }).start();
    }

    /** Couleur 1 / Couleur 2 : un seul sélectionné à la fois (sinon la couleur 2 devenait inaccessible). */
    private static void fixColorButtons() {
        final TButton c1 = gui.homeToolBar.colorPlate.color1Button, c2 = gui.homeToolBar.colorPlate.color2Button;
        if (c1 == null || c2 == null) return;
        c1.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                c1.setSelected(true); c2.setSelected(false); c1.repaint(); c2.repaint(); } });
        } });
        c2.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                c2.setSelected(true); c1.setSelected(false); c1.repaint(); c2.repaint(); } });
        } });
    }

    private static void setWindowIcon() {
        java.util.List<java.awt.Image> icons = new java.util.ArrayList<>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            java.net.URL u = Fixes.class.getResource("/icons/mspaint-linux_" + s + ".png");
            if (u != null) icons.add(new javax.swing.ImageIcon(u).getImage());
        }
        if (!icons.isEmpty()) gui.frame.setIconImages(icons);
    }

    /** Les boutons Pot de peinture / Texte n'avaient pas de nom d'outil (""). */
    private static void fixToolNames() {
        Object mt = gui.homeToolBar.mainTools;
        nameTool((TButton) get(mt, "fillTool"), "Fill Tool", null);
        nameTool((TButton) get(mt, "textTool"), "Text Tool", Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    private static void nameTool(final TButton b, final String name, final Cursor cur) {
        if (b == null) return;
        b.setToolTipText(name);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        gui.activeToolName = name;
                        if (cur != null) gui.view.setCursor(cur);
                    }
                });
            }
        });
    }

    // =====================================================================
    // Aides générales
    // =====================================================================

    static Object get(Object o, String field) {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    static void set(Object o, String field, Object v) {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                f.set(o, v);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (Exception e) {
                return;
            }
        }
    }

    public static void selectMenuItem(Object owner, String field, boolean sel) {
        Object o = get(owner, field);
        if (o instanceof TButton) ((TButton) o).setSelected(sel);
    }

    public static int activeIndex() {
        int i = LayersArea.activeLayer;
        return (i < 0 || i >= gui.layers.size()) ? 0 : i;
    }

    public static BufferedImage activeImage() {
        return gui.layers.get(activeIndex()).image;
    }

    /** Applique l'aperçu des formes (shapeImage) sur le calque actif. */
    public static void commitShapeImage() {
        Graphics2D g = activeImage().createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(gui.shapeImage, 0, 0, null);
        g.dispose();
        MainViewAction.clearImage(gui.shapeImage);
        gui.view.repaint();
    }

    /** Remplace les images de tous les calques (changement de taille du canevas). */
    public static void replaceImages(BufferedImage[] imgs) {
        int w = imgs[0].getWidth(), h = imgs[0].getHeight();
        for (int i = 0; i < gui.layers.size() && i < imgs.length; i++) gui.layers.get(i).image = imgs[i];
        gui.mainImage = gui.layers.get(0).image;
        gui.graphics = activeImage().createGraphics();
        gui.tempImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        gui.tempGraphics = gui.tempImage.createGraphics();
        gui.shapeImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        gui.shapeGraphics = gui.shapeImage.createGraphics();
        set(gui.view, "image", gui.mainImage);
        set(gui.view, "tempImage", gui.tempImage);
        set(gui.view, "shapeImage", gui.shapeImage);
        gui.view.resetSize();
        gui.view.revalidate();
        gui.holder.revalidate();
        gui.imageViewPort.revalidate();
        gui.view.repaint();
        gui.holder.repaint();
        Object l = get(gui, "imageResoLabel");
        if (l instanceof JLabel) ((JLabel) l).setText(w + " x " + h);
    }

    /** Annule une sélection flottante (sans la recoller). */
    public static void cancelFloatingSelection() {
        MainViewAction va = gui.viewAction;
        gui.isSelectionActive = false;
        va.sImage = null;
        va.isInSelection = false;
        va.isDragging = false;
        va.isDraggingImage = false;
        MainViewAction.clearImage(gui.tempImage);
        if (gui.selectionResoLabel != null) gui.selectionResoLabel.setText("");
        try {
            gui.homeToolBar.clipboardTools.cutTool.setTEnabled(false);
            gui.homeToolBar.clipboardTools.copyTool.setTEnabled(false);
            gui.homeToolBar.imageTools.cropTool.setTEnabled(false);
        } catch (Exception ignore) { }
        gui.view.repaint();
    }

    /** Termine l'opération en cours (texte, polygone, courbe). */
    public static void finishPending() {
        if (textArea != null) commitText();
        if (polyPts != null) commitPolygon(true);
        if (curveState > 0) commitCurve();
        pendingTool = null;
    }

    public static boolean isEditingText() {
        return textArea != null && textArea.isFocusOwner();
    }

    // =====================================================================
    // Interception de la souris sur le canevas
    // =====================================================================

    static String pendingTool;

    private static void installInterceptor() {
        final MainViewAction va = gui.viewAction;
        gui.view.removeMouseListener(va);
        gui.view.removeMouseMotionListener(va);
        Interceptor ic = new Interceptor(va);
        gui.view.addMouseListener(ic);
        gui.view.addMouseMotionListener(ic);
        gui.view.setLayout(null);
    }

    static Point2D.Double imgPoint(MouseEvent e) {
        double s = gui.view.scale <= 0 ? 1 : gui.view.scale;
        return new Point2D.Double(e.getX() / s, e.getY() / s);
    }

    private static boolean mine(String t) {
        return "Fill Tool".equals(t) || "Text Tool".equals(t) || "Polygon".equals(t) || "Curve".equals(t)
            || "Eraser Tool".equals(t);
    }

    static final class Interceptor implements MouseListener, MouseMotionListener {
        final MainViewAction va;
        Interceptor(MainViewAction va) { this.va = va; }

        public void mousePressed(MouseEvent e) {
            String t = gui.activeToolName;
            if (pendingTool != null && !pendingTool.equals(t)) finishPending();
            if (!mine(t)) { va.mousePressed(e); return; }
            if (gui.isSelectionActive) va.drawImageSelected();
            Point2D.Double p = imgPoint(e);
            if ("Eraser Tool".equals(t)) {
                eraseLast = p;
                erase(p, p);
            } else if ("Fill Tool".equals(t)) {
                fill((int) p.x, (int) p.y, SwingUtilities.isRightMouseButton(e) ? gui.activeColor2 : gui.activeColor1);
            } else if ("Text Tool".equals(t)) {
                if (textArea != null) commitText();
                else openText((int) Math.round(p.x), (int) Math.round(p.y));
            } else if ("Polygon".equals(t)) {
                polygonPress(p, e);
            } else if ("Curve".equals(t)) {
                curvePress(p);
            }
        }

        public void mouseDragged(MouseEvent e) {
            String t = gui.activeToolName;
            if (!mine(t)) { va.mouseDragged(e); return; }
            Point2D.Double p = imgPoint(e);
            if ("Polygon".equals(t)) polygonMove(p);
            else if ("Curve".equals(t)) curveDrag(p);
            else if ("Eraser Tool".equals(t) && eraseLast != null) { erase(eraseLast, p); eraseLast = p; }
        }

        public void mouseReleased(MouseEvent e) {
            String t = gui.activeToolName;
            if (!mine(t)) { va.mouseReleased(e); return; }
            Point2D.Double p = imgPoint(e);
            if ("Polygon".equals(t)) polygonRelease(p);
            else if ("Curve".equals(t)) curveRelease(p);
            else if ("Eraser Tool".equals(t)) eraseLast = null;
        }

        public void mouseMoved(MouseEvent e) {
            va.mouseMoved(e);
            if ("Polygon".equals(gui.activeToolName) && polyPts != null) polygonMove(imgPoint(e));
        }

        public void mouseClicked(MouseEvent e) { if (!mine(gui.activeToolName)) va.mouseClicked(e); }
        public void mouseEntered(MouseEvent e) { va.mouseEntered(e); }
        public void mouseExited(MouseEvent e) {
            if (polyPts == null && curveState == 0) va.mouseExited(e);
        }
    }

    // =====================================================================
    // Gomme : trait continu (l'original effaçait des carrés isolés, avec des trous)
    // =====================================================================

    static Point2D.Double eraseLast;

    static void erase(Point2D.Double a, Point2D.Double b) {
        int size = Math.max(4, gui.activeSize);
        Graphics2D g = activeImage().createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.setStroke(new BasicStroke(size, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        g.draw(new java.awt.geom.Line2D.Double(a.x, a.y, b.x, b.y));
        g.dispose();
        // aperçu du carré de gomme
        MainViewAction.clearImage(gui.tempImage);
        Graphics2D t = gui.tempImage.createGraphics();
        int x = (int) b.x - size / 2, y = (int) b.y - size / 2;
        t.setColor(Color.WHITE);
        t.fillRect(x, y, size, size);
        t.setColor(Color.BLACK);
        t.drawRect(x, y, size, size);
        t.dispose();
        gui.view.repaint();
    }

    // =====================================================================
    // Pot de peinture
    // =====================================================================

    static void fill(int x, int y, Color c) {
        BufferedImage img = activeImage();
        int w = img.getWidth(), h = img.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h || c == null) return;
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        int target = px[y * w + x], repl = c.getRGB();
        if (target == repl) return;
        final int tol = 40;
        boolean[] seen = new boolean[w * h];
        int[] stack = new int[w * h];
        int sp = 0;
        stack[sp++] = y * w + x;
        while (sp > 0) {
            int i = stack[--sp];
            if (seen[i]) continue;
            int cx = i % w, cy = i / w;
            int l = cx, r = cx;
            while (l > 0 && !seen[cy * w + l - 1] && close(px[cy * w + l - 1], target, tol)) l--;
            while (r < w - 1 && !seen[cy * w + r + 1] && close(px[cy * w + r + 1], target, tol)) r++;
            if (!close(px[i], target, tol)) continue;
            for (int k = l; k <= r; k++) {
                int j = cy * w + k;
                seen[j] = true;
                px[j] = repl;
                if (cy > 0) { int u = j - w; if (!seen[u] && close(px[u], target, tol)) stack[sp++] = u; }
                if (cy < h - 1) { int d = j + w; if (!seen[d] && close(px[d], target, tol)) stack[sp++] = d; }
                if (sp >= stack.length - 2) break;
            }
        }
        img.setRGB(0, 0, w, h, px, 0, w);
        gui.view.repaint();
    }

    private static boolean close(int a, int b, int tol) {
        int aa = a >>> 24, ba = b >>> 24;
        if (aa == 0 && ba == 0) return true;
        return Math.abs(aa - ba) <= tol
            && Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)) <= tol
            && Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)) <= tol
            && Math.abs((a & 255) - (b & 255)) <= tol;
    }

    // =====================================================================
    // Texte
    // =====================================================================

    static JTextArea textArea;
    static int textX, textY;
    private static final int TEXT_PAD = 3;

    private static Object fontTool() { return find(gui.textToolBar, FontTool.class); }
    private static Object bgTool() { return find(gui.textToolBar, BackgroundTools.class); }

    private static Object find(Container c, Class<?> k) {
        if (c == null) return null;
        for (Component x : c.getComponents()) {
            if (k.isInstance(x)) return x;
            if (x instanceof Container) {
                Object r = find((Container) x, k);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static boolean btn(Object owner, String f) {
        if (owner == null) return false;
        Object b = get(owner, f);
        return b instanceof TButton && ((TButton) b).isSelected();
    }

    static Font currentFont(double scale) {
        Object ft = fontTool();
        String name = "SansSerif";
        int size = 11;
        if (ft != null) {
            Object nb = get(ft, "nameBox");
            if (nb instanceof JComboBox && ((JComboBox<?>) nb).getSelectedItem() != null)
                name = String.valueOf(((JComboBox<?>) nb).getSelectedItem());
            Object sb = get(ft, "sizeBox");
            if (sb instanceof JComboBox) {
                Object v = ((JComboBox<?>) sb).getSelectedItem();
                try { size = Integer.parseInt(String.valueOf(v).trim()); } catch (Exception ignore) { }
            }
        }
        Map<TextAttribute, Object> at = new HashMap<>();
        at.put(TextAttribute.FAMILY, name);
        // tailles en points (comme Paint) -> pixels à 96 dpi
        at.put(TextAttribute.SIZE, (float) (size * 96.0 / 72.0 * scale));
        if (btn(ft, "bold")) at.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        if (btn(ft, "italic")) at.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        if (btn(ft, "underline")) at.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        if (btn(ft, "strike")) at.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        return new Font(at);
    }

    static boolean opaqueText() { return btn(bgTool(), "opaque"); }

    static void openText(int x, int y) {
        textX = x;
        textY = y;
        final JTextArea a = new JTextArea();
        textArea = a;
        pendingTool = "Text Tool";
        styleText();
        a.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(new Color(90, 90, 90), 3, 2),
            BorderFactory.createEmptyBorder(TEXT_PAD - 1, TEXT_PAD - 1, TEXT_PAD - 1, TEXT_PAD - 1)));
        a.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { layoutText(); }
            public void removeUpdate(DocumentEvent e) { layoutText(); }
            public void changedUpdate(DocumentEvent e) { layoutText(); }
        });
        gui.view.add(a);
        layoutText();
        clickTab("textButton");
        a.requestFocusInWindow();
        gui.view.repaint();
    }

    static void styleText() {
        JTextArea a = textArea;
        if (a == null) return;
        double s = gui.view.scale <= 0 ? 1 : gui.view.scale;
        a.setFont(currentFont(s));
        Color c1 = gui.activeColor1 != null ? gui.activeColor1 : Color.BLACK;
        a.setForeground(c1);
        a.setCaretColor(c1);
        boolean op = opaqueText();
        a.setOpaque(op);
        a.setBackground(op && gui.activeColor2 != null ? gui.activeColor2 : new Color(0, 0, 0, 0));
        layoutText();
    }

    static void layoutText() {
        JTextArea a = textArea;
        if (a == null) return;
        double s = gui.view.scale <= 0 ? 1 : gui.view.scale;
        Dimension d = a.getPreferredSize();
        int w = Math.max(d.width + 8, (int) (120 * s)), h = d.height;
        Insets in = a.getInsets();
        int vx = (int) Math.round(textX * s) - in.left, vy = (int) Math.round(textY * s) - in.top;
        a.setBounds(vx, vy, w, h);
        a.revalidate();
        gui.view.repaint();
    }

    static void commitText() {
        JTextArea a = textArea;
        if (a == null) return;
        textArea = null;
        String txt = a.getText();
        gui.view.remove(a);
        gui.view.repaint();
        if (txt != null && !txt.isEmpty()) {
            Graphics2D g = activeImage().createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            Font f = currentFont(1.0);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            String[] lines = txt.split("\n", -1);
            int lh = fm.getHeight();
            if (opaqueText() && gui.activeColor2 != null) {
                int mw = 0;
                for (String l : lines) mw = Math.max(mw, fm.stringWidth(l));
                g.setColor(gui.activeColor2);
                g.fillRect(textX, textY, mw + 2, lh * lines.length);
            }
            g.setColor(gui.activeColor1 != null ? gui.activeColor1 : Color.BLACK);
            for (int i = 0; i < lines.length; i++) {
                if (!lines[i].isEmpty()) g.drawString(lines[i], textX, textY + fm.getAscent() + i * lh);
            }
            g.dispose();
        }
        if (polyPts == null && curveState == 0) pendingTool = null;
        clickTab("homeButton");
        gui.view.repaint();
        Undo.check();
    }

    private static void clickTab(String field) {
        Object b = get(gui, field);
        if (b instanceof AbstractButton) ((AbstractButton) b).doClick(0);
    }

    private static void hookTextToolbar() {
        Object ft = fontTool();
        Object bt = bgTool();
        ActionListener restyle = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() { public void run() { styleText(); } });
            }
        };
        for (String f : new String[]{"bold", "italic", "underline", "strike"}) {
            Object b = ft == null ? null : get(ft, f);
            if (b instanceof AbstractButton) ((AbstractButton) b).addActionListener(restyle);
        }
        for (String f : new String[]{"nameBox", "sizeBox"}) {
            Object b = ft == null ? null : get(ft, f);
            if (b instanceof JComboBox) ((JComboBox<?>) b).addActionListener(restyle);
        }
        for (String f : new String[]{"transparent", "opaque"}) {
            Object b = bt == null ? null : get(bt, f);
            if (b instanceof AbstractButton) ((AbstractButton) b).addActionListener(restyle);
        }
        // police par défaut lisible (la liste commence souvent par une police exotique)
        Object nb = ft == null ? null : get(ft, "nameBox");
        if (nb instanceof JComboBox) {
            JComboBox<?> cb = (JComboBox<?>) nb;
            String[] pref = {"Ubuntu", "Noto Sans", "DejaVu Sans", "Liberation Sans", "Arial", "Cantarell", "FreeSans"};
            outer:
            for (String p : pref) {
                for (int i = 0; i < cb.getItemCount(); i++) {
                    if (p.equalsIgnoreCase(String.valueOf(cb.getItemAt(i)))) { cb.setSelectedIndex(i); break outer; }
                }
            }
        }
        Object sb = ft == null ? null : get(ft, "sizeBox");
        if (sb instanceof JComboBox) {
            JComboBox<?> cb = (JComboBox<?>) sb;
            for (int i = 0; i < cb.getItemCount(); i++) {
                if ("12".equals(String.valueOf(cb.getItemAt(i)))) { cb.setSelectedIndex(i); break; }
            }
        }
        // fond transparent par défaut
        if (bt != null && !btn(bt, "opaque") && !btn(bt, "transparent")) {
            Object tr = get(bt, "transparent");
            if (tr instanceof TButton) ((TButton) tr).setSelected(true);
        }
    }

    // =====================================================================
    // Polygone : cliquer chaque sommet, double-clic (ou clic sur le 1er point / Entrée) pour fermer
    // =====================================================================

    static List<Point2D.Double> polyPts;
    static Point2D.Double polyCursor;

    static void polygonPress(Point2D.Double p, MouseEvent e) {
        pendingTool = "Polygon";
        if (polyPts == null) {
            polyPts = new ArrayList<>();
            polyPts.add(p);
            polyCursor = p;
        } else {
            Point2D.Double first = polyPts.get(0);
            double tolr = 6 / (gui.view.scale <= 0 ? 1 : gui.view.scale);
            if (e.getClickCount() >= 2 || (polyPts.size() >= 2 && first.distance(p) < tolr)) {
                commitPolygon(true);
                return;
            }
            polyPts.add(p);
            polyCursor = p;
        }
        previewPolygon();
    }

    static void polygonMove(Point2D.Double p) {
        if (polyPts == null) return;
        polyCursor = p;
        previewPolygon();
    }

    static void polygonRelease(Point2D.Double p) {
        if (polyPts == null) return;
        Point2D.Double last = polyPts.get(polyPts.size() - 1);
        if (last.distance(p) > 2) polyPts.add(p);
        polyCursor = p;
        previewPolygon();
    }

    private static Path2D polyPath(boolean close, boolean withCursor) {
        Path2D path = new Path2D.Double();
        path.moveTo(polyPts.get(0).x, polyPts.get(0).y);
        for (int i = 1; i < polyPts.size(); i++) path.lineTo(polyPts.get(i).x, polyPts.get(i).y);
        if (withCursor && polyCursor != null) path.lineTo(polyCursor.x, polyCursor.y);
        if (close) path.closePath();
        return path;
    }

    static void previewPolygon() {
        MainViewAction.clearImage(gui.shapeImage);
        Graphics2D g = gui.shapeImage.createGraphics();
        Shapes.paint(g, polyPath(false, true), false, gui);
        g.dispose();
        gui.view.repaint();
    }

    static void commitPolygon(boolean apply) {
        List<Point2D.Double> pts = polyPts;
        polyPts = null;
        MainViewAction.clearImage(gui.shapeImage);
        if (apply && pts != null && pts.size() >= 2) {
            polyPts = pts;
            Graphics2D g = gui.shapeImage.createGraphics();
            Shapes.paint(g, polyPath(pts.size() >= 3, false), pts.size() >= 3, gui);
            g.dispose();
            polyPts = null;
            commitShapeImage();
        }
        polyCursor = null;
        if (textArea == null && curveState == 0) pendingTool = null;
        gui.view.repaint();
        Undo.check();
    }

    // =====================================================================
    // Courbe : tracer une ligne, puis cliquer-glisser pour la courber
    // =====================================================================

    static int curveState;
    static Point2D.Double cA, cB, cCtrl;

    static void curvePress(Point2D.Double p) {
        pendingTool = "Curve";
        if (curveState == 0) {
            cA = p;
            cB = p;
            cCtrl = null;
            curveState = 1;
        } else if (curveState == 2) {
            curveState = 3;
            bend(p);
        }
        previewCurve();
    }

    static void curveDrag(Point2D.Double p) {
        if (curveState == 1) cB = p;
        else if (curveState == 3) bend(p);
        previewCurve();
    }

    static void curveRelease(Point2D.Double p) {
        if (curveState == 1) {
            cB = p;
            if (cA.distance(cB) < 1) { curveState = 0; MainViewAction.clearImage(gui.shapeImage); gui.view.repaint(); return; }
            curveState = 2;
            previewCurve();
        } else if (curveState == 3) {
            bend(p);
            commitCurve();
        }
    }

    private static void bend(Point2D.Double p) {
        // point de contrôle tel que la courbe passe par le point de la souris
        cCtrl = new Point2D.Double(2 * p.x - (cA.x + cB.x) / 2, 2 * p.y - (cA.y + cB.y) / 2);
    }

    private static Shape curveShape() {
        if (cCtrl == null) return Shapes.line(cA.x, cA.y, cB.x, cB.y);
        return new QuadCurve2D.Double(cA.x, cA.y, cCtrl.x, cCtrl.y, cB.x, cB.y);
    }

    static void previewCurve() {
        MainViewAction.clearImage(gui.shapeImage);
        if (curveState > 0) {
            Graphics2D g = gui.shapeImage.createGraphics();
            Shapes.paint(g, curveShape(), false, gui);
            g.dispose();
        }
        gui.view.repaint();
    }

    static void commitCurve() {
        if (curveState >= 2) {
            previewCurve();
            commitShapeImage();
        } else {
            MainViewAction.clearImage(gui.shapeImage);
        }
        curveState = 0;
        cA = cB = cCtrl = null;
        if (textArea == null && polyPts == null) pendingTool = null;
        gui.view.repaint();
        Undo.check();
    }

    // =====================================================================
    // Image : Rogner, Redimensionner, Rotation, Retournement
    // =====================================================================

    private static void hookImageTools() {
        final mspaint.gui.toolbar.home.ImageTools it = gui.homeToolBar.imageTools;
        on(it.cropTool, new Runnable() { public void run() { crop(); } });
        on(it.resizeTool, new Runnable() { public void run() { resizeDialog(); } });
        on(it.rotateRight_90, new Runnable() { public void run() { transform(1); } });
        on(it.rotateLeft_90, new Runnable() { public void run() { transform(2); } });
        on(it.rotate_180, new Runnable() { public void run() { transform(3); } });
        on(it.flipHorizonal, new Runnable() { public void run() { transform(4); } });
        on(it.flipVertical, new Runnable() { public void run() { transform(5); } });
        // « Sélectionner tout » ne marchait qu'avec l'outil Sélection déjà actif
        if (it.selectAllItem != null) it.selectAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String t = gui.activeToolName;
                if (!"Rectangular Selection Tool".equals(t) && !"Free-form Selection Tool".equals(t)) {
                    finishPending();
                    gui.activeToolName = "Rectangular Selection Tool";
                    if (it.rectangularSecItem != null) it.rectangularSecItem.doClick(0);
                    // relance une fois l'outil Sélection actif
                    SwingUtilities.invokeLater(new Runnable() { public void run() { it.selectAllItem.doClick(0); } });
                }
            }
        });
    }

    private static void on(AbstractButton b, final Runnable r) {
        if (b == null) return;
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(r);
            }
        });
    }

    static void crop() {
        if (!gui.isSelectionActive) return;
        finishPending();
        Object s = get(gui.viewAction, "temps"), en = get(gui.viewAction, "tempe");
        if (!(s instanceof mspaint.helper.Point) || !(en instanceof mspaint.helper.Point)) return;
        mspaint.helper.Point a = (mspaint.helper.Point) s, b = (mspaint.helper.Point) en;
        gui.viewAction.drawImageSelected();
        int W = gui.mainImage.getWidth(), H = gui.mainImage.getHeight();
        int x0 = Math.max(0, Math.min(a.x, b.x)), y0 = Math.max(0, Math.min(a.y, b.y));
        int x1 = Math.min(W, Math.max(a.x, b.x)), y1 = Math.min(H, Math.max(a.y, b.y));
        if (x1 - x0 < 1 || y1 - y0 < 1) return;
        BufferedImage[] out = new BufferedImage[gui.layers.size()];
        for (int i = 0; i < out.length; i++) {
            BufferedImage src = gui.layers.get(i).image;
            BufferedImage d = new BufferedImage(x1 - x0, y1 - y0, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = d.createGraphics();
            g.drawImage(src, -x0, -y0, null);
            g.dispose();
            out[i] = d;
        }
        cancelFloatingSelection();
        replaceImages(out);
        Undo.check();
    }

    /** 1=droite 90, 2=gauche 90, 3=180, 4=miroir H, 5=miroir V */
    static void transform(int op) {
        finishPending();
        if (gui.isSelectionActive) gui.viewAction.drawImageSelected();
        cancelFloatingSelection();
        BufferedImage[] out = new BufferedImage[gui.layers.size()];
        for (int i = 0; i < out.length; i++) {
            BufferedImage s = gui.layers.get(i).image;
            int w = s.getWidth(), h = s.getHeight();
            boolean swap = op == 1 || op == 2;
            BufferedImage d = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = d.createGraphics();
            switch (op) {
                case 1: g.translate(h, 0); g.rotate(Math.PI / 2); g.drawImage(s, 0, 0, null); break;
                case 2: g.translate(0, w); g.rotate(-Math.PI / 2); g.drawImage(s, 0, 0, null); break;
                case 3: g.translate(w, h); g.rotate(Math.PI); g.drawImage(s, 0, 0, null); break;
                case 4: g.drawImage(s, w, 0, -w, h, null); break;
                default: g.drawImage(s, 0, h, w, -h, null); break;
            }
            g.dispose();
            out[i] = d;
        }
        replaceImages(out);
        Undo.check();
    }

    static void resizeDialog() {
        finishPending();
        if (gui.isSelectionActive) gui.viewAction.drawImageSelected();
        cancelFloatingSelection();
        final int W = gui.mainImage.getWidth(), H = gui.mainImage.getHeight();
        final JSpinner sw = new JSpinner(new SpinnerNumberModel(W, 1, 20000, 1));
        final JSpinner sh = new JSpinner(new SpinnerNumberModel(H, 1, 20000, 1));
        final JSpinner sp = new JSpinner(new SpinnerNumberModel(100, 1, 1000, 1));
        final JCheckBox keep = new JCheckBox("Conserver les proportions", true);
        final boolean[] lock = {false};
        sw.addChangeListener(new ChangeListener() { public void stateChanged(ChangeEvent e) {
            if (lock[0]) return; lock[0] = true;
            int w = (Integer) sw.getValue();
            if (keep.isSelected()) sh.setValue(Math.max(1, (int) Math.round(w * (double) H / W)));
            sp.setValue(Math.max(1, (int) Math.round(w * 100.0 / W)));
            lock[0] = false; } });
        sh.addChangeListener(new ChangeListener() { public void stateChanged(ChangeEvent e) {
            if (lock[0]) return; lock[0] = true;
            int h = (Integer) sh.getValue();
            if (keep.isSelected()) sw.setValue(Math.max(1, (int) Math.round(h * (double) W / H)));
            lock[0] = false; } });
        sp.addChangeListener(new ChangeListener() { public void stateChanged(ChangeEvent e) {
            if (lock[0]) return; lock[0] = true;
            int p = (Integer) sp.getValue();
            sw.setValue(Math.max(1, (int) Math.round(W * p / 100.0)));
            sh.setValue(Math.max(1, (int) Math.round(H * p / 100.0)));
            lock[0] = false; } });
        JPanel pn = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; pn.add(new JLabel("Pourcentage :"), c);
        c.gridx = 1; pn.add(sp, c);
        c.gridx = 0; c.gridy = 1; pn.add(new JLabel("Largeur (px) :"), c);
        c.gridx = 1; pn.add(sw, c);
        c.gridx = 0; c.gridy = 2; pn.add(new JLabel("Hauteur (px) :"), c);
        c.gridx = 1; pn.add(sh, c);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; pn.add(keep, c);
        if (JOptionPane.showConfirmDialog(gui.frame, pn, "Redimensionner", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        int nw = (Integer) sw.getValue(), nh = (Integer) sh.getValue();
        if (nw == W && nh == H) return;
        BufferedImage[] out = new BufferedImage[gui.layers.size()];
        for (int i = 0; i < out.length; i++) {
            BufferedImage d = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = d.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(gui.layers.get(i).image, 0, 0, nw, nh, null);
            g.dispose();
            out[i] = d;
        }
        replaceImages(out);
        Undo.check();
    }

    // =====================================================================
    // Raccourcis clavier + boutons Annuler/Rétablir
    // =====================================================================

    private static void installShortcuts() {
        JComponent root = gui.frame.getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        int C = InputEvent.CTRL_DOWN_MASK, S = InputEvent.SHIFT_DOWN_MASK;
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Z, C), "undo", new Runnable() { public void run() { Undo.undo(); } });
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Y, C), "redo", new Runnable() { public void run() { Undo.redo(); } });
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Z, C | S), "redo2", new Runnable() { public void run() { Undo.redo(); } });
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_N, C), "new", file("New"));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_O, C), "open", file("Open"));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_S, C), "save", file("Save"));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_S, C | S), "saveas", file("Save as"));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_P, C), "print", file("print"));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_A, C), "selall", click(gui.homeToolBar.imageTools.selectAllItem));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete", click(gui.homeToolBar.imageTools.deleteItem));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_C, C), "copy", click(gui.homeToolBar.clipboardTools.copyTool));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_X, C), "cut", click(gui.homeToolBar.clipboardTools.cutTool));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_V, C), "paste", click(gui.homeToolBar.clipboardTools.pasteItem));
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc", new Runnable() { public void run() {
            if (textArea != null) commitText();
            else if (polyPts != null) commitPolygon(true);
            else if (curveState > 0) commitCurve();
            else if (gui.isSelectionActive) gui.viewAction.drawImageSelected();
        } });
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter", new Runnable() { public void run() {
            if (polyPts != null) commitPolygon(true);
            else if (curveState > 0) commitCurve();
        } });
    }

    private static Runnable file(final String cmd) {
        return new Runnable() { public void run() { finishPending(); FileMenuAction.perform(gui, cmd); } };
    }

    private static Runnable click(final AbstractButton b) {
        return new Runnable() { public void run() {
            if (b == null) return;
            if (b instanceof TButton && !((TButton) b).isTEnabled()) return;
            b.doClick(0);
        } };
    }

    private static void bind(InputMap im, ActionMap am, KeyStroke ks, String name, final Runnable r) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { r.run(); Undo.schedule(); }
        });
    }

    private static void addUndoButtons() {
        Object o = get(gui, "topMenubar");
        if (!(o instanceof JPanel)) return;
        JPanel bar = (JPanel) o;
        JButton u = smallButton("↶", "Annuler (Ctrl+Z)", new Runnable() { public void run() { Undo.undo(); } });
        JButton r = smallButton("↷", "Rétablir (Ctrl+Y)", new Runnable() { public void run() { Undo.redo(); } });
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setOpaque(false);
        p.add(u);
        p.add(r);
        if (bar.getLayout() instanceof GridBagLayout) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = bar.getComponentCount() + 1;
            c.gridy = 0;
            c.insets = new Insets(0, 12, 0, 0);
            bar.add(p, c);
        } else {
            bar.add(p);
        }
        bar.revalidate();
    }

    private static JButton smallButton(String txt, String tip, final Runnable r) {
        JButton b = new JButton(txt);
        b.setToolTipText(tip);
        b.setFocusable(false);
        b.setMargin(new Insets(0, 6, 0, 6));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(new Color(40, 40, 40));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { r.run(); } });
        return b;
    }

}
