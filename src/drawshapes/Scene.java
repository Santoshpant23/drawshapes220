package drawshapes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.security.sasl.SaslException;

/**
 * A scene of shapes. Uses the Model-View-Controller (MVC) design pattern,
 * though note that model knows something about the view, as the draw()
 * method both in Scene and in Shape uses the Graphics object. That's kind of
 * sloppy,
 * but it also helps keep things simple.
 * 
 * This class allows us to talk about a "scene" of shapes,
 * rather than individual shapes, and to apply operations
 * to collections of shapes.
 * 
 * @author jspacco
 *
 */
public class Scene implements Iterable<IShape> {
    private List<IShape> shapeList = new LinkedList<IShape>();
    private SelectionRectangle selectRect;
    private boolean isDrag;
    private Point startDrag;
    Scene copy;

    public void updateSelectRect(Point drag) {
        for (IShape s : this) {
            s.setSelected(false);
        }
        if (drag.x > startDrag.x) {
            if (drag.y > startDrag.y) {
                // top-left to bottom-right
                selectRect = new SelectionRectangle(startDrag.x, drag.x, startDrag.y, drag.y);
            } else {
                // bottom-left to top-right
                selectRect = new SelectionRectangle(startDrag.x, drag.x, drag.y, startDrag.y);
            }
        } else {
            if (drag.y > startDrag.y) {
                // top-right to bottom-left
                selectRect = new SelectionRectangle(drag.x, startDrag.x, startDrag.y, drag.y);
            } else {
                // bottom-left to top-right
                selectRect = new SelectionRectangle(drag.x, startDrag.x, drag.y, startDrag.y);
            }
        }
        List<IShape> selectedShapes = this.select(selectRect);
        for (IShape s : selectedShapes) {
            s.setSelected(true);
        }
    }

    public void stopDrag() {
        this.isDrag = false;
    }

    public void startDrag(Point p) {
        this.isDrag = true;
        this.startDrag = p;
    }

    /**
     * Draw all the shapes in the scene using the given Graphics object.
     * 
     * @param g
     */
    public void draw(Graphics g) {
        for (IShape s : shapeList) {
            if (s != null) {
                s.draw(g);
            }
        }
        if (isDrag) {
            selectRect.draw(g);
        }
    }

    /**
     * Get an iterator that can iterate through all the shapes
     * in the scene.
     */
    public Iterator<IShape> iterator() {
        return shapeList.iterator();
    }

    /**
     * Return a list of shapes that contain the given point.
     * 
     * @param point The point
     * @return A list of shapes that contain the given point.
     */
    public List<IShape> select(Point point) {
        List<IShape> selected = new LinkedList<IShape>();
        for (IShape s : shapeList) {
            if (s.contains(point)) {
                selected.add(s);
            }
        }
        return selected;
    }

    /**
     * Return a list of shapes in the scene that intersect the given shape.
     * 
     * @param s The shape
     * @return A list of shapes intersecting the given shape.
     */
    public List<IShape> select(IShape shape) {
        List<IShape> selected = new LinkedList<IShape>();
        for (IShape s : shapeList) {
            if (s.intersects(shape)) {
                selected.add(s);
            }
        }
        return selected;
    }

    /**
     * Add a shape to the scene. It will be rendered next time
     * the draw() method is invoked.
     * 
     * @param s
     */
    public void addShape(IShape s) {
        shapeList.add(s);
    }

    /**
     * Remove a list of shapes from the given scene.
     * 
     * @param shapesToRemove
     */
    public void removeShapes(Collection<IShape> shapesToRemove) {
        shapeList.removeAll(shapesToRemove);
    }

    @Override
    public String toString() {
        System.out.println("I am inside toString of Scene ");
        String shapeText = "";
        for (IShape s : shapeList) {
            shapeText += s.toString() + "\n";
        }
        return shapeText;
    }

    public void move(int dx, int dy) {
        for (IShape s : shapeList) {
            if (s.isSelected()) {
                s.move(dx, dy);
            }
        }
    }

    @SuppressWarnings("resource")
    public void loadShapes(File selectedFile) throws Exception {
        shapeList.clear();
        Scanner sc = new Scanner(new FileInputStream(selectedFile));

        while (sc.hasNext()) {
            String shape = sc.next();

            if (shape.equals("SQUARE")) {
                // SQUARE 185 110 100 RED false
                int x = sc.nextInt();
                int y = sc.nextInt();
                int length = sc.nextInt();
                String color = sc.next();
                Color clr = Util.stringToColor(color);
                Boolean isSelected = sc.nextBoolean();
                Square square = new Square(clr, x, y, length);
                square.setSelected(isSelected);
                addShape(square);
            } else if (shape.equals("RECTANGLE")) {
                // RECTANGLE 424 311 100 200 RED false
                int x = sc.nextInt();
                int y = sc.nextInt();
                int width = sc.nextInt();
                int height = sc.nextInt();
                String color = sc.next();
                Boolean isSelected = sc.nextBoolean();
                Color clr = Util.stringToColor(color);
                Rectangle rectangle = new Rectangle(new Point(x, y), width, height, clr);
                rectangle.setSelected(isSelected);
                addShape(rectangle);
            } else if (shape.equals("CIRCLE")) {
                // CIRCLE 243 211 100 RED false
                int x = sc.nextInt();
                int y = sc.nextInt();
                int diameter = sc.nextInt();
                String color = sc.next();
                Boolean isSelected = sc.nextBoolean();
                Color clr = Util.stringToColor(color);
                Circle circle = new Circle(clr, new Point(x, y), diameter);
                circle.setSelected(isSelected);
                addShape(circle);
            } else {
                throw new UnsupportedOperationException("File cannot start with " + shape);
            }

        }
    }

    public Scene copy() {
        Scene sc = new Scene();
        for (IShape shape : shapeList) {
            sc.addShape(shape.copy());
        }

        return sc;
    }

    public void reload(Scene otherScene) {
        this.shapeList = otherScene.shapeList;
    }

    public void bringForward() {
        for (int i = shapeList.size() - 2; i >= 0; i--) {
            IShape current = shapeList.get(i);
            if (current.isSelected()) {
                // Swap with shape in front of it
                IShape next = shapeList.get(i + 1);
                shapeList.set(i + 1, current);
                shapeList.set(i, next);
                break;
            }
        }
    }

    public void sendBackward() {
        for (int i = 1; i < shapeList.size(); i++) {
            IShape current = shapeList.get(i);
            if (current.isSelected()) {
                // Swap with shape behind it
                IShape prev = shapeList.get(i - 1);
                shapeList.set(i - 1, current);
                shapeList.set(i, prev);
                break;
            }
        }
    }

}
