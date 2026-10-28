package mspaint;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import mspaint.action.file.FileMenuAction;
import mspaint.gui.GUIHandler;
import mspaint.style.DefaultStyle;
import mspaint.style.Style;
import mspaint.theme.DefaultTheme;
import mspaint.theme.Theme;

public class MainLauncher {
    GUIHandler gui;
    public static GUIHandler GUI;
    Style style = new DefaultStyle();
    Theme theme = new DefaultTheme();

    private MainLauncher() {
        gui = new GUIHandler(style, theme);
        GUI = gui;
        gui.handle();
    }

    public static void main(final String[] args) {
        // Le LAF GTK reprend le thème sombre du système (texte blanc) alors que
        // l'appli peint ses fonds en blanc en dur : on force le LAF Metal.
        java.util.Locale.setDefault(java.util.Locale.FRANCE);
        javax.swing.JComponent.setDefaultLocale(java.util.Locale.FRANCE);
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // ignore
        }
        final MainLauncher app = new MainLauncher();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { mspaint.fix.Fixes.install(app.gui); }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (args.length > 0) {
            final File f = new File(args[0]);
            if (f.isFile()) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { FileMenuAction.openFile(app.gui, f); }
                });
            }
        }
    }
}
