package mspaint.fix;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import mspaint.action.file.FileMenuAction;

/** « Images récentes » du menu Fichier (l'original affichait une fausse liste figée). */
public final class Recent {
    private static final int MAX = 4;
    private static final List<AbstractButton> buttons = new ArrayList<>();
    private static List<String> files = new ArrayList<>();

    private Recent() {}

    private static Preferences prefs() { return Preferences.userRoot().node("mspaint-linux"); }

    static void register(final AbstractButton b) {
        if (buttons.contains(b)) return;
        final int idx = buttons.size();
        buttons.add(b);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (idx >= files.size()) return;
                final File f = new File(files.get(idx));
                Component p = SwingUtilities.getAncestorOfClass(JPopupMenu.class, b);
                if (p != null) p.setVisible(false);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Fixes.finishPending();
                        FileMenuAction.openFile(Fixes.gui, f);
                    }
                });
            }
        });
    }

    static void refresh() {
        files = new ArrayList<>();
        try {
            String s = prefs().get("recent", "");
            for (String l : s.split("\n")) if (!l.trim().isEmpty() && new File(l).isFile()) files.add(l);
        } catch (Throwable ignore) { }
        for (int i = 0; i < buttons.size(); i++) {
            AbstractButton b = buttons.get(i);
            if (i < files.size()) {
                b.setText((i + 1) + ". " + new File(files.get(i)).getName());
                b.setToolTipText(files.get(i));
                b.setVisible(true);
            } else {
                b.setText("");
                b.setVisible(false);
            }
            if (b.getParent() != null) {
                b.getParent().invalidate();
                b.getParent().validate();
                b.getParent().repaint();
            }
        }
    }

    public static void add(File f) {
        try {
            String path = f.getAbsolutePath();
            List<String> l = new ArrayList<>();
            l.add(path);
            for (String s : prefs().get("recent", "").split("\n"))
                if (!s.trim().isEmpty() && !s.equals(path) && l.size() < MAX) l.add(s);
            prefs().put("recent", String.join("\n", l));
            prefs().flush();
        } catch (Throwable ignore) { }
        refresh();
    }
}
