package mspaint.action.file;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import mspaint.action.MainViewAction;
import mspaint.gui.GUIHandler;
import mspaint.gui.menubar.FilePopupMenu;
import mspaint.gui.sidebar.LayersArea;
import mspaint.helper.Layer;
import mspaint.helper.TButton;

public class FileMenuAction implements ActionListener {
    private GUIHandler gui;
    private FilePopupMenu menu;
    private static File lastDir = new File(System.getProperty("user.home"));
    private static File currentFile = null;

    public FileMenuAction(GUIHandler gui, FilePopupMenu menu) {
        this.gui = gui;
        this.menu = menu;
    }

    public void actionPerformed(ActionEvent e) {
        TButton b = (TButton) e.getSource();
        if (menu != null && menu.isShowing()) menu.setVisible(false);
        mspaint.fix.Fixes.finishPending();
        run(b.getText(), b.getName());
        mspaint.fix.Undo.check();
    }

    /** Pour les raccourcis clavier (Ctrl+O, Ctrl+S…). */
    public static void perform(GUIHandler gui, String cmd) {
        new FileMenuAction(gui, null).run(cmd, null);
        mspaint.fix.Undo.check();
    }

    private void run(String t, String n) {
        if ("New".equals(t)) {
            if (gui.isSelectionActive) gui.viewAction.drawImageSelected();
            for (Layer l : gui.layers) MainViewAction.clearImage(l.image);
            MainViewAction.clearImage(gui.tempImage);
            currentFile = null;
            gui.frame.setTitle("Sans titre - Paint");
            gui.view.repaint();
        } else if ("Open".equals(t)) {
            openImage();
        } else if ("Save".equals(t)) {
            if (currentFile != null) writeImage(currentFile);
            else saveImage(null);
        } else if ("Save as".equals(t)) {
            saveImage(null);
        } else if ("print".equalsIgnoreCase(t)) {
            PrinterJob job = PrinterJob.getPrinterJob();
            if (job.printDialog()) {
                try { job.print(); } catch (PrinterException ex) { System.out.println(ex); }
            }
        } else if ("Set as Desktop Background".equals(t)) {
            setWallpaper();
        } else if ("About".equals(t)) {
            JOptionPane.showMessageDialog(gui.frame,
                "Paint (Java) – version Linux corrigée\n\nAnnuler/Rétablir, Texte, Pot de peinture, toutes les formes,\nRogner/Redimensionner/Faire pivoter, mode sombre, interface en français.",
                "À propos de Paint", JOptionPane.INFORMATION_MESSAGE);
        } else if ("Exit".equals(t)) {
            System.exit(0);
        } else if ("png".equals(n)) {
            saveImage("png");
        } else if ("jpeg".equals(n)) {
            saveImage("jpg");
        } else if ("bmp".equals(n)) {
            saveImage("bmp");
        } else if ("gif".equals(n)) {
            saveImage("gif");
        } else if ("other".equals(n)) {
            saveImage(null);
        }
    }

    // ---------- Ouvrir ----------

    private void openImage() {
        JFileChooser fc = new JFileChooser(lastDir);
        FileNameExtensionFilter f = new FileNameExtensionFilter(
            "Images (png, jpg, jpeg, bmp, gif, tif, tiff, wbmp)",
            ImageIO.getReaderFileSuffixes());
        fc.addChoosableFileFilter(f);
        fc.setFileFilter(f);
        if (fc.showOpenDialog(gui.frame) == JFileChooser.APPROVE_OPTION) {
            lastDir = fc.getCurrentDirectory();
            openFile(gui, fc.getSelectedFile());
        }
    }

    public static void openFile(GUIHandler gui, File file) {
        BufferedImage src;
        try {
            src = ImageIO.read(file);
        } catch (IOException ex) {
            src = null;
        }
        if (src == null) {
            JOptionPane.showMessageDialog(gui.frame,
                "Impossible de lire l'image :\n" + file.getAbsolutePath(),
                "Ouvrir", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (gui.isSelectionActive) gui.viewAction.drawImageSelected();

        int w = src.getWidth(), h = src.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        mspaint.fix.Fixes.cancelFloatingSelection();
        BufferedImage[] imgs = new BufferedImage[gui.layers.size()];
        for (int i = 0; i < imgs.length; i++)
            imgs[i] = (i == 0) ? img : new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        mspaint.fix.Fixes.replaceImages(imgs);
        setLabel(gui, "imageSizeLabel", Math.max(1, file.length() / 1024) + " Ko");

        currentFile = file;
        lastDir = file.getParentFile();
        gui.frame.setTitle(file.getName() + " - Paint");
        mspaint.fix.Recent.add(file);
        mspaint.fix.Undo.check();
    }

    private static void setField(Object o, String name, Object value) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(o, value);
        } catch (Exception ex) {
            System.err.println("setField " + name + ": " + ex);
        }
    }

    private static void setLabel(GUIHandler gui, String name, String text) {
        try {
            Field f = GUIHandler.class.getDeclaredField(name);
            f.setAccessible(true);
            Object l = f.get(gui);
            if (l instanceof javax.swing.JLabel) ((javax.swing.JLabel) l).setText(text);
        } catch (Exception ex) {
            // optionnel
        }
    }

    // ---------- Enregistrer ----------

    private void saveImage(String forcedExt) {
        JFileChooser fc = new JFileChooser(lastDir);
        fc.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter png = new FileNameExtensionFilter("Image PNG (*.png)", "png");
        FileNameExtensionFilter jpg = new FileNameExtensionFilter("Image JPEG (*.jpg)", "jpg", "jpeg");
        FileNameExtensionFilter bmp = new FileNameExtensionFilter("Image BMP (*.bmp)", "bmp");
        FileNameExtensionFilter gif = new FileNameExtensionFilter("Image GIF (*.gif)", "gif");
        fc.addChoosableFileFilter(png);
        fc.addChoosableFileFilter(jpg);
        fc.addChoosableFileFilter(bmp);
        fc.addChoosableFileFilter(gif);
        FileNameExtensionFilter sel = png;
        if ("jpg".equals(forcedExt)) sel = jpg;
        else if ("bmp".equals(forcedExt)) sel = bmp;
        else if ("gif".equals(forcedExt)) sel = gif;
        fc.setFileFilter(sel);
        String base = currentFile != null ? stripExt(currentFile.getName()) : "image";
        fc.setSelectedFile(new File(lastDir, base + "." + sel.getExtensions()[0]));

        if (fc.showSaveDialog(gui.frame) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        String ext = extOf(file);
        if (ext == null || !isWritableExt(ext)) {
            ext = ((FileNameExtensionFilter) fc.getFileFilter()).getExtensions()[0];
            file = new File(file.getParentFile(), file.getName() + "." + ext);
        }
        if (file.exists() && JOptionPane.showConfirmDialog(gui.frame,
                file.getName() + " existe déjà.\nVoulez-vous le remplacer ?", "Enregistrer sous",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        lastDir = file.getParentFile();
        if (writeImage(file)) {
            currentFile = file;
            gui.frame.setTitle(file.getName() + " - Paint");
        }
    }

    private boolean writeImage(File file) {
        if (gui.isSelectionActive) gui.viewAction.drawImageSelected();
        String ext = extOf(file);
        if (ext == null) ext = "png";
        if (ext.equals("jpeg")) ext = "jpg";
        BufferedImage flat = flatten(!(ext.equals("png") || ext.equals("gif")));
        try {
            if (!ImageIO.write(flat, ext, file)) throw new IOException("format non pris en charge : " + ext);
            mspaint.fix.Recent.add(file);
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(gui.frame, "Échec de l'enregistrement :\n" + ex.getMessage(),
                "Enregistrer", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Fusionne les calques visibles. opaque=true → fond blanc, RGB (JPG/BMP). */
    private BufferedImage flatten(boolean opaque) {
        int w = gui.mainImage.getWidth(), h = gui.mainImage.getHeight();
        BufferedImage out = new BufferedImage(w, h,
            opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        if (opaque) { g.setColor(Color.WHITE); g.fillRect(0, 0, w, h); }
        for (Layer l : gui.layers) if (l.isVisible) g.drawImage(l.image, 0, 0, null);
        g.dispose();
        return out;
    }

    // ---------- Fond d'écran ----------

    private void setWallpaper() {
        try {
            File dir = new File(System.getProperty("user.home"), ".local/share/mspaint");
            dir.mkdirs();
            File f = new File(dir, "wallpaper.png");
            ImageIO.write(flatten(false), "png", f);
            String uri = f.toURI().toString();
            String[][] cmds = {
                {"gsettings", "set", "org.cinnamon.desktop.background", "picture-uri", uri},
                {"gsettings", "set", "org.gnome.desktop.background", "picture-uri", uri},
                {"gsettings", "set", "org.gnome.desktop.background", "picture-uri-dark", uri},
                {"gsettings", "set", "org.mate.background", "picture-filename", f.getAbsolutePath()},
            };
            for (String[] c : cmds) {
                try { new ProcessBuilder(c).redirectErrorStream(true).start().waitFor(); }
                catch (Exception ignore) { }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(gui.frame, "Échec : " + ex.getMessage(),
                "Arrière-plan du Bureau", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- utilitaires ----------

    private static String extOf(File f) {
        String s = f.getName();
        int i = s.lastIndexOf('.');
        return (i > 0 && i < s.length() - 1) ? s.substring(i + 1).toLowerCase() : null;
    }

    private static String stripExt(String s) {
        int i = s.lastIndexOf('.');
        return i > 0 ? s.substring(0, i) : s;
    }

    private static boolean isWritableExt(String e) {
        return e.equals("png") || e.equals("jpg") || e.equals("jpeg") || e.equals("bmp") || e.equals("gif");
    }
}
