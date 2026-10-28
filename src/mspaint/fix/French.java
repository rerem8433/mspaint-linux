package mspaint.fix;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import mspaint.gui.GUIHandler;

/**
 * Traduction de l'interface.
 * Le code d'origine se sert des libellés anglais comme clés (ex. getText().equals("Crop")).
 * On affiche donc le français, et on remet l'anglais le temps d'exécuter les actions du bouton.
 */
public final class French {
    private French() {}

    private static final Map<String, String> T = new HashMap<>();

    private static void t(String en, String fr) { T.put(en, fr); }

    private static String html(String s) { return "<HTML><p style=\"text-align:center;\">" + s + "</p>"; }

    private static String fmt(String title, String desc) { return "<HTML><strong>" + title + "</strong><br>" + desc; }

    static {
        // onglets / menu Fichier
        t("File", "Fichier");
        t("Home", "Accueil");
        t("View", "Affichage");
        t("Text", "Texte");
        t("New", "Nouveau");
        t("Open", "Ouvrir");
        t("Save", "Enregistrer");
        t("Save as", "Enregistrer sous");
        t("Print", "Imprimer");
        t("Set as Desktop Background", "Définir comme arrière-plan du Bureau");
        t("About", "À propos de Paint");
        t("Exit", "Quitter");
        t("Recent pictures", "Images récentes");
        t("<HTML><strong>PNG picture</strong><br>Save a photo or drawing with hight quality and use it on your computer or on the web.",
          fmt("Image PNG", "Enregistrer une photo ou un dessin en haute qualité<br>pour votre ordinateur ou le Web."));
        t("<HTML><strong>JPEG picture</strong><br>Save a photo with good quality and use iton your computer, in email, or on the web.",
          fmt("Image JPEG", "Enregistrer une photo de bonne qualité pour votre<br>ordinateur, un courrier électronique ou le Web."));
        t("<HTML><strong>BMP picture</strong><br>Save any kind of picture with hight quality and use it on your computer.",
          fmt("Image BMP", "Enregistrer tout type d'image en haute qualité<br>pour l'utiliser sur votre ordinateur."));
        t("<HTML><strong>GIF picture</strong><br>Save a simple drawing with lower qualityand use it in email or on the web.",
          fmt("Image GIF", "Enregistrer un dessin simple en qualité réduite<br>pour un courrier électronique ou le Web."));
        t("<HTML><strong>Other formats</strong><br>Open the Save As dialog box to select fromall possible file types.",
          fmt("Autres formats", "Ouvrir la boîte de dialogue Enregistrer sous<br>pour choisir parmi tous les types de fichiers."));

        // Accueil
        t("Clipboard", "Presse-papiers");
        t("Paste", "Coller");
        t("Import Paste", "Coller à partir de…");
        t("Cut", "Couper");
        t("Copy", "Copier");
        t("Image", "Image");
        t("Select", "Sélection");
        t("Crop", "Rogner");
        t("Resize", "Redimensionner");
        t("Rotate", "Pivoter");
        t("Rotate Right 90 dgree", "Faire pivoter à droite de 90°");
        t("Rotate Left 90 dgree", "Faire pivoter à gauche de 90°");
        t("Rotate 180 dgree", "Faire pivoter de 180°");
        t("Flip Vertical", "Retourner verticalement");
        t("Flip Horizonal", "Retourner horizontalement");
        t("Section Shapes", "Formes de sélection");
        t("Section Options", "Options de sélection");
        t("Rectangular selection", "Sélection rectangulaire");
        t("Free-from selection", "Sélection libre");
        t("Select all", "Sélectionner tout");
        t("Delete", "Supprimer");
        t("Tools", "Outils");
        t("Brushes", "Pinceaux");
        t("Shapes", "Formes");
        t("Outline", "Contour");
        t("Fill", "Remplir");
        t("No Outline", "Aucun contour");
        t("No Fill", "Aucun remplissage");
        t("Solid", "Couleur unie");
        t("Brush", "Pinceau");
        t("Oil", "Huile");
        t("Watercolor", "Aquarelle");
        t(html("Size"), html("Taille"));
        t(html("Color<br>1"), html("Couleur<br>1"));
        t(html("Color<br>2"), html("Couleur<br>2"));
        t(html("Edit<br>Color"), html("Modifier<br>couleurs"));
        t("Colors", "Couleurs");
        t("Layers", "Calques");
        t(html("Layers"), html("Calques"));
        t("Filters", "Filtres");
        t(html("Filters"), html("Filtres"));

        // Texte
        t("Font", "Police");
        t("Background", "Arrière-plan");
        t("Transparent", "Transparent");
        t("Opaque", "Opaque");

        // Affichage
        t("Zoom", "Zoom");
        t(html("Zoom<br>In"), html("Zoom<br>avant"));
        t(html("Zoom<br>Out"), html("Zoom<br>arrière"));
        t("Show and Hide", "Afficher ou masquer");
        t("Rulers", "Règles");
        t("Gridlines", "Quadrillage");
        t("Status bar", "Barre d'état");
        t(html("Full<br>Screen"), html("Plein<br>écran"));
        t(html("Float<br>Canvas"), html("Canevas<br>flottant"));

        // Calques / filtres
        t("Add Layer", "Ajouter un calque");
        t("Delete Layer", "Supprimer le calque");
        t("Main Layer", "Calque principal");
        t("Show/Hide Layer", "Afficher/masquer le calque");
        t("Settings", "Paramètres");
        t("Filter 1", "Filtre 1");
        t("Filter 2", "Filtre 2");
        t("Filter 3", "Filtre 3");

        // infobulles : outils
        t("Pencil Tool", "Crayon");
        t("Fill Tool", "Remplissage (pot de peinture)");
        t("Text Tool", "Texte");
        t("Eraser Tool", "Gomme");
        t("Color Picker Tool", "Sélecteur de couleurs");
        t("Zoom Tool", "Loupe");
        t("Rectangular Selection Tool", "Sélection rectangulaire");
        t("Free-form Selection Tool", "Sélection libre");
        // pinceaux
        t("Line Brush", "Pinceau");
        t("Calligraphy Brush 1", "Pinceau calligraphique 1");
        t("Calligraphy Brush 2", "Pinceau calligraphique 2");
        t("Air Brush", "Aérographe");
        t("Oil Brush", "Pinceau à huile");
        t("Crayon", "Pastel");
        t("Marker", "Marqueur");
        t("Color Pencil", "Crayon de couleur");
        t("Watercolor Brush", "Pinceau aquarelle");
        // formes
        t("Line", "Ligne");
        t("Curve", "Courbe");
        t("Oval", "Ovale");
        t("Rectangle", "Rectangle");
        t("Round Rec", "Rectangle arrondi");
        t("Polygon", "Polygone (double-clic pour fermer)");
        t("Triangle", "Triangle");
        t("Right Triangle", "Triangle rectangle");
        t("Diamond", "Losange");
        t("Pentagon", "Pentagone");
        t("Hexagon", "Hexagone");
        t("Right Arrow", "Flèche droite");
        t("Left Arrow", "Flèche gauche");
        t("Up Arrow", "Flèche haut");
        t("Down Arrow", "Flèche bas");
        t("4 Point Star", "Étoile à 4 branches");
        t("5 Point Star", "Étoile à 5 branches");
        t("6 Point Star", "Étoile à 6 branches");
        t("Round Rec Callout", "Légende rectangulaire arrondie");
        t("Oval Callout", "Légende ovale");
        t("Cloud Callout", "Légende nuage");
        t("Heart", "Cœur");
        t("Lightening", "Éclair");
    }

    public static String tr(String en) {
        String f = T.get(en);
        return f != null ? f : en;
    }

    private static final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    static void apply(GUIHandler gui) {
        seen.clear();
        walk(gui.frame);
        scan(gui, 0);
        if ("MSPaint".equals(gui.frame.getTitle())) gui.frame.setTitle("Sans titre - Paint");
        // la barre d'outils (GridBag) écrase tout à la taille minimale quand elle déborde :
        // on bloque la galerie de formes et la palette à leur taille normale
        for (Component c : new Component[]{gui.homeToolBar.shapeTools, gui.homeToolBar.colorPlate}) {
            if (c != null) c.setMinimumSize(c.getPreferredSize());
        }
        // fenêtre un peu plus large si l'écran le permet
        java.awt.Rectangle scr = gui.frame.getGraphicsConfiguration().getBounds();
        int need = gui.homeToolBar.getPreferredSize().width + 40;
        if (gui.frame.getWidth() < need && (gui.frame.getExtendedState() & java.awt.Frame.MAXIMIZED_BOTH) == 0) {
            int w = Math.min(need, scr.width - 20);
            gui.frame.setSize(w, gui.frame.getHeight());
            gui.frame.setLocation(scr.x + (scr.width - w) / 2, gui.frame.getY());
        }
    }

    private static void walk(Component c) {
        if (c == null || !seen.add(c)) return;
        if (c instanceof AbstractButton && String.valueOf(((AbstractButton) c).getText()).matches("\\d\\. .*")) {
            Recent.register((AbstractButton) c);
        } else if (c instanceof AbstractButton) translateButton((AbstractButton) c);
        else if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            String f = T.get(l.getText());
            if (f != null) l.setText(f);
        }
        if (c instanceof JComponent) {
            JComponent j = (JComponent) c;
            String tip = j.getToolTipText();
            if (tip != null && T.containsKey(tip)) j.setToolTipText(T.get(tip));
        }
        if (c instanceof Container) for (Component x : ((Container) c).getComponents()) walk(x);
        scan(c, 0);
    }

    /** Parcourt les champs des objets de l'appli pour trouver les menus déroulants non affichés. */
    private static void scan(Object o, int depth) {
        if (o == null || depth > 6) return;
        Class<?> k = o.getClass();
        while (k != null && (k.getName().startsWith("mspaint") || k.getName().startsWith("com.gmail"))) {
            for (Field f : k.getDeclaredFields()) {
                if (f.getType().isPrimitive() || java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(o);
                    if (v instanceof Component) {
                        if (!seen.contains(v)) walk((Component) v);
                    } else if (v instanceof Object[]) {
                        for (Object e : (Object[]) v) if (e instanceof Component && !seen.contains(e)) walk((Component) e);
                    } else if (v instanceof java.util.List) {
                        for (Object e : (java.util.List<?>) v) if (e instanceof Component && !seen.contains(e)) walk((Component) e);
                    } else if (v != null && v.getClass().getName().startsWith("mspaint") && seen.add(v)) {
                        scan(v, depth + 1);
                    }
                } catch (Throwable ignore) { }
            }
            k = k.getSuperclass();
        }
    }

    private static void translateButton(final AbstractButton b) {
        final String en = b.getText();
        final String fr = en == null ? null : T.get(en);
        if (fr == null || b.getClientProperty("mspaint.en") != null) return;
        b.putClientProperty("mspaint.en", en);
        final ActionListener[] ls = b.getActionListeners();
        for (ActionListener l : ls) b.removeActionListener(l);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                b.setText(en);
                try {
                    // même ordre que Swing : dernier ajouté appelé en premier
                    for (int i = ls.length - 1; i >= 0; i--) ls[i].actionPerformed(e);
                } finally {
                    b.setText(fr);
                }
            }
        });
        b.setText(fr);
        // onglets à largeur fixe : on élargit si le français ne tient pas
        Dimension p = b.getPreferredSize();
        if (b.isPreferredSizeSet() && !fr.startsWith("<HTML>")) {
            FontMetrics fm = b.getFontMetrics(b.getFont());
            int need = fm.stringWidth(fr) + 20;
            if (need > p.width) b.setPreferredSize(new Dimension(need, p.height));
        }
    }
}
