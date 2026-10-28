package mspaint.fix;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.SwingUtilities;
import mspaint.gui.GUIHandler;

/**
 * Annuler / Rétablir par instantanés des calques.
 * Après chaque relâchement souris/clavier, on compare l'état courant au dernier état
 * validé ; s'il a changé, l'ancien état part dans la pile d'annulation.
 * Fonctionne donc pour tous les outils (pinceaux, formes, texte, pot, rotation…).
 */
public final class Undo {
    private static final long MAX_BYTES = 400L * 1024 * 1024;
    private static GUIHandler gui;
    private static final Deque<BufferedImage[]> undo = new ArrayDeque<>();
    private static final Deque<BufferedImage[]> redo = new ArrayDeque<>();
    private static BufferedImage[] committed;
    private static boolean scheduled;
    private static boolean restoring;

    private Undo() {}

    static void install(GUIHandler g) {
        gui = g;
        committed = snapshot();
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                int id = e.getID();
                if (id == MouseEvent.MOUSE_RELEASED || id == KeyEvent.KEY_RELEASED) schedule();
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    public static void schedule() {
        if (scheduled || gui == null) return;
        scheduled = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { scheduled = false; check(); }
                });
            }
        });
    }

    /** Enregistre un pas d'annulation si l'image a changé. */
    public static void check() {
        if (gui == null || restoring) return;
        // une sélection flottante découpe temporairement le calque : état transitoire
        if (gui.isSelectionActive) return;
        int n = gui.layers.size();
        if (committed == null || committed.length != n) {
            // le nombre de calques a changé : on repart d'une base propre
            undo.clear();
            redo.clear();
            committed = snapshot();
            return;
        }
        boolean changed = false;
        for (int i = 0; i < n && !changed; i++) {
            if (!same(committed[i], gui.layers.get(i).image)) changed = true;
        }
        if (!changed) return;
        undo.push(committed);
        redo.clear();
        committed = snapshot();
        trim();
    }

    public static boolean canUndo() { return !undo.isEmpty(); }
    public static boolean canRedo() { return !redo.isEmpty(); }

    public static void undo() {
        if (Fixes.isEditingText()) return;
        Fixes.finishPending();
        if (gui.isSelectionActive) gui.viewAction.drawImageSelected();
        check();
        if (undo.isEmpty()) return;
        redo.push(committed);
        restore(undo.pop());
    }

    public static void redo() {
        if (Fixes.isEditingText()) return;
        Fixes.finishPending();
        if (gui.isSelectionActive) gui.viewAction.drawImageSelected();
        check();
        if (redo.isEmpty()) return;
        undo.push(committed);
        restore(redo.pop());
    }

    private static void restore(BufferedImage[] snap) {
        restoring = true;
        try {
            Fixes.cancelFloatingSelection();
            int n = gui.layers.size();
            boolean sameDims = snap.length == n;
            for (int i = 0; i < n && sameDims; i++) {
                BufferedImage cur = gui.layers.get(i).image;
                if (cur.getWidth() != snap[i].getWidth() || cur.getHeight() != snap[i].getHeight()) sameDims = false;
            }
            if (sameDims) {
                for (int i = 0; i < n; i++) copyInto(snap[i], gui.layers.get(i).image);
                gui.view.repaint();
            } else if (snap.length == n) {
                BufferedImage[] imgs = new BufferedImage[n];
                for (int i = 0; i < n; i++) imgs[i] = copy(snap[i]);
                Fixes.replaceImages(imgs);
            }
            committed = snap;
        } finally {
            restoring = false;
        }
    }

    private static BufferedImage[] snapshot() {
        int n = gui.layers.size();
        BufferedImage[] s = new BufferedImage[n];
        for (int i = 0; i < n; i++) s[i] = copy(gui.layers.get(i).image);
        return s;
    }

    static BufferedImage copy(BufferedImage src) {
        BufferedImage d = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copyInto(src, d);
        return d;
    }

    private static void copyInto(BufferedImage src, BufferedImage dst) {
        Graphics2D g = dst.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(src, 0, 0, null);
        g.dispose();
    }

    private static boolean same(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        int w = a.getWidth(), h = a.getHeight();
        if (a.getType() == BufferedImage.TYPE_INT_ARGB && b.getType() == BufferedImage.TYPE_INT_ARGB) {
            int[] pa = ((DataBufferInt) a.getRaster().getDataBuffer()).getData();
            int[] pb = ((DataBufferInt) b.getRaster().getDataBuffer()).getData();
            return java.util.Arrays.equals(pa, pb);
        }
        int[] ra = new int[w], rb = new int[w];
        for (int y = 0; y < h; y++) {
            a.getRGB(0, y, w, 1, ra, 0, w);
            b.getRGB(0, y, w, 1, rb, 0, w);
            if (!java.util.Arrays.equals(ra, rb)) return false;
        }
        return true;
    }

    private static long bytes(BufferedImage[] s) {
        long t = 0;
        for (BufferedImage b : s) t += 4L * b.getWidth() * b.getHeight();
        return t;
    }

    private static void trim() {
        long total = 0;
        for (BufferedImage[] s : undo) total += bytes(s);
        for (BufferedImage[] s : redo) total += bytes(s);
        while ((total > MAX_BYTES || undo.size() > 100) && undo.size() > 1) {
            total -= bytes(undo.removeLast());
        }
    }
}
