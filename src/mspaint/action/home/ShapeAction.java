package mspaint.action.home;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import mspaint.action.MainViewAction;
import mspaint.fix.Fixes;
import mspaint.fix.Shapes;
import mspaint.gui.GUIHandler;
import mspaint.gui.toolbar.home.ShapeTools;
import mspaint.helper.Point;

/**
 * Remplacement de l'original : l'ancien code découpait la zone en supposant un tracé
 * vers la droite/le bas (sinon la forme était perdue ou réduite à un point) puis
 * basculait l'outil sur « Sélection ». Ici la forme est dessinée en aperçu puis
 * appliquée directement sur le calque actif, et l'outil reste actif.
 */
public class ShapeAction {
    private static GUIHandler gui;
    private static ShapeTools shapeTools;
    private static boolean isDragging;

    public ShapeAction(GUIHandler g, ShapeTools tools) {
        gui = g;
        shapeTools = tools;
        tools.activeOutline = "Solid";
        tools.activeFill = "No Fill";
        Fixes.selectMenuItem(tools, "noOutline", false);
        Fixes.selectMenuItem(tools, "solid1", true);
        Fixes.selectMenuItem(tools, "noFill", true);
    }

    public static boolean isShapeTool(String t) {
        return "Line".equals(t) || Shapes.isBoxShape(t);
    }

    public static boolean mouseDragged(Point a, Point b) {
        String tool = gui.activeToolName;
        if (!isShapeTool(tool)) return false;
        MainViewAction.clearImage(gui.shapeImage);
        Graphics2D g = gui.shapeImage.createGraphics();
        Shape s;
        boolean closed;
        if ("Line".equals(tool)) {
            s = Shapes.line(a.x, a.y, b.x, b.y);
            closed = false;
        } else {
            double x = Math.min(a.x, b.x), y = Math.min(a.y, b.y);
            double w = Math.abs(b.x - a.x), h = Math.abs(b.y - a.y);
            s = Shapes.build(tool, x, y, w, h);
            closed = true;
        }
        Shapes.paint(g, s, closed, gui);
        g.dispose();
        isDragging = true;
        return true;
    }

    public static boolean mouseReleased(MouseEvent e) {
        if (!isDragging) return false;
        isDragging = false;
        Fixes.commitShapeImage();
        return true;
    }
}
