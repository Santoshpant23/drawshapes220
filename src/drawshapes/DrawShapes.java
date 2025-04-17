package drawshapes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Stack;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.plaf.synth.SynthRadioButtonMenuItemUI;

@SuppressWarnings("serial")
public class DrawShapes extends JFrame {
    public enum ShapeType {
        SQUARE,
        CIRCLE,
        RECTANGLE
    }

    private DrawShapesPanel shapePanel;
    private Scene scene;
    private ShapeType shapeType = ShapeType.SQUARE;
    private Color color = Color.RED;
    private Point startDrag;
    int distance = 25;
    private Stack<Scene> undo = new Stack<>();
    private Stack<Scene> redo = new Stack<>();

    public DrawShapes(int width, int height) {
        setTitle("Draw Shapes!");
        scene = new Scene();
        undo.push(scene);

        // create our canvas, add to this frame's content pane
        shapePanel = new DrawShapesPanel(width, height, scene);
        this.getContentPane().add(shapePanel, BorderLayout.CENTER);
        this.setResizable(false);
        this.pack();
        this.setLocation(100, 100);

        // Add key and mouse listeners to our canvas
        initializeMouseListener();
        initializeKeyListener();

        // initialize the menu options
        initializeMenu();

        // Handle closing the window.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void initializeMouseListener() {
        MouseAdapter a = new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                System.out.printf("Mouse cliked at (%d, %d)\n", e.getX(), e.getY());

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (shapeType == ShapeType.SQUARE) {
                        Scene copy = scene.copy();
                        undo.push(copy);
                        scene.addShape(new Square(color,
                                e.getX(),
                                e.getY(),
                                100));
                    } else if (shapeType == ShapeType.CIRCLE) {
                        Scene copy = scene.copy();
                        undo.push(copy);
                        scene.addShape(new Circle(color,
                                e.getPoint(),
                                100));
                    } else if (shapeType == ShapeType.RECTANGLE) {
                        Scene copy = scene.copy();
                        undo.push(copy);
                        scene.addShape(new Rectangle(
                                e.getPoint(),
                                100,
                                200,
                                color));
                    }

                } else if (e.getButton() == MouseEvent.BUTTON2) {
                    // apparently this is middle click
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // right right-click
                    Point p = e.getPoint();
                    System.out.printf("Right click is (%d, %d)\n", p.x, p.y);
                    List<IShape> selected = scene.select(p);
                    if (selected.size() > 0) {
                        for (IShape s : selected) {
                            s.setSelected(true);
                        }
                    } else {
                        for (IShape s : scene) {
                            s.setSelected(false);
                        }
                    }
                    System.out.printf("Select %d shapes\n", selected.size());
                }
                repaint();
            }

            /*
             * (non-Javadoc)
             * 
             * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
             */
            public void mousePressed(MouseEvent e) {
                System.out.printf("mouse pressed at (%d, %d)\n", e.getX(), e.getY());
                scene.startDrag(e.getPoint());

            }

            /*
             * (non-Javadoc)
             * 
             * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
             */
            public void mouseReleased(MouseEvent e) {
                System.out.printf("mouse released at (%d, %d)\n", e.getX(), e.getY());
                scene.stopDrag();
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                System.out.printf("mouse drag! (%d, %d)\n", e.getX(), e.getY());
                scene.updateSelectRect(e.getPoint());
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // TODO use this to grow/shrink shapes
            }

        };
        shapePanel.addMouseMotionListener(a);
        shapePanel.addMouseListener(a);
    }

    /**
     * Initialize the menu options
     */
    private void initializeMenu() {
        // menu bar
        JMenuBar menuBar = new JMenuBar();

        // file menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        // load
        JMenuItem loadItem = new JMenuItem("Load");
        fileMenu.add(loadItem);
        loadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Loading will delete current file, are you sure you want to load?");
                try {
                    System.out.println(e.getActionCommand());
                    JFileChooser jfc = new JFileChooser(".");
                    int returnValue = jfc.showOpenDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = jfc.getSelectedFile();
                        System.out.println("load from " + selectedFile.getAbsolutePath());
                        Scene copy = scene.copy();
                        undo.push(copy);
                        scene.loadShapes(selectedFile);
                        repaint();
                    }
                } catch (Exception excep) {
                    JOptionPane.showMessageDialog(null, excep);
                }
            }
        });
        // save
        JMenuItem saveItem = new JMenuItem("Save");
        fileMenu.add(saveItem);
        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    System.out.println(e.getActionCommand());
                    JFileChooser jfc = new JFileChooser(".");

                    // int returnValue = jfc.showOpenDialog(null);
                    int returnValue = jfc.showSaveDialog(null);

                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = jfc.getSelectedFile();
                        System.out.println("save to " + selectedFile.getAbsolutePath());
                        String str = scene.toString();
                        System.out.println(str);
                        try (PrintWriter out = new PrintWriter(selectedFile)) {
                            out.println(str);
                            JOptionPane.showMessageDialog(null, "Saved");
                            scene.reload(new Scene());
                            repaint();
                        } catch (IOException err) {
                            JOptionPane.showMessageDialog(null, err);
                        }

                    }
                } catch (Exception excep) {
                    JOptionPane.showMessageDialog(null, excep);
                }
            }
        });
        fileMenu.addSeparator();
        // edit
        JMenuItem itemExit = new JMenuItem("Exit");
        fileMenu.add(itemExit);
        itemExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
                System.exit(0);
            }
        });

        // color menu
        JMenu colorMenu = new JMenu("Color");
        menuBar.add(colorMenu);

        // red color
        JMenuItem redColorItem = new JMenuItem("Red");
        colorMenu.add(redColorItem);
        redColorItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
                // change the color instance variable to red
                color = Color.RED;
            }
        });

        // blue color
        JMenuItem blueColorItem = new JMenuItem("Blue");
        colorMenu.add(blueColorItem);
        blueColorItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
                // change the color instance variable to blue
                color = Color.BLUE;
                System.out.println(color);
            }
        });

        // green color
        JMenuItem greenColorItem = new JMenuItem("Green");
        colorMenu.add(greenColorItem);
        greenColorItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
                // change the color instance variable to blue
                color = Color.GREEN;
                System.out.println(color);
            }
        });

        // yellow color
        JMenuItem yellowColorItem = new JMenuItem("Yellow");
        colorMenu.add(yellowColorItem);
        yellowColorItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
                // change the color instance variable to blue
                color = Color.YELLOW;
                System.out.println(color);
            }
        });

        // black color
        JMenuItem blackColorItem = new JMenuItem("Black");
        colorMenu.add(blackColorItem);
        blackColorItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
                // change the color instance variable to blue
                color = Color.BLACK;
                System.out.println(color);
            }
        });

        // cyan color
        JMenuItem cyanColorItem = new JMenuItem("Cyan");
        colorMenu.add(cyanColorItem);
        cyanColorItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
                // change the color instance variable to blue
                color = Color.CYAN;
                System.out.println(color);
            }
        });

        // shape menu
        JMenu shapeMenu = new JMenu("Shape");
        menuBar.add(shapeMenu);

        // square
        JMenuItem squareItem = new JMenuItem("Square");
        shapeMenu.add(squareItem);
        squareItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Square");
                shapeType = ShapeType.SQUARE;
            }
        });

        // circle
        JMenuItem circleItem = new JMenuItem("Circle");
        shapeMenu.add(circleItem);
        circleItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Circle");
                shapeType = ShapeType.CIRCLE;
            }
        });

        // rectangle
        JMenuItem rectangleMenu = new JMenuItem("Rectangle");
        shapeMenu.add(rectangleMenu);
        rectangleMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Rectangle");
                shapeType = ShapeType.RECTANGLE;
            }
        });

        // operation mode menu
        JMenu operationModeMenu = new JMenu("Operation");
        menuBar.add(operationModeMenu);

        // draw option
        JMenuItem drawItem = new JMenuItem("Resize");
        operationModeMenu.add(drawItem);
        drawItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
            }
        });

        // select option
        JMenuItem selectItem = new JMenuItem("Move");
        operationModeMenu.add(selectItem);
        selectItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String text = e.getActionCommand();
                System.out.println(text);
            }
        });

        // set the menu bar for this frame
        this.setJMenuBar(menuBar);
    }

    /**
     * Initialize the keyboard listener.
     */
    private void initializeKeyListener() {
        shapePanel.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                System.out.println("key typed: " + e.getKeyChar());
            }

            public void keyReleased(KeyEvent e) {
                // TODO: implement this method if you need it
            }

            public void keyTyped(KeyEvent e) {
                // TODO: implement this method if you need it
                char ch = e.getKeyChar();
                // moveUp
                if (ch == 'w') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    scene.move(0, -distance);
                }
                // moveDown
                if (ch == 's') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    scene.move(0, distance);
                }
                // moveLeft
                if (ch == 'a') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    scene.move(-distance, 0);
                }
                // moveRight
                if (ch == 'd') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    scene.move(distance, 0);
                }

                // scaleUp
                if (ch == 'u') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    for (IShape s : scene) {
                        if (s.isSelected()) {
                            s.scaleUp();
                        }
                    }
                }
                // scaleDown
                if (ch == 'l') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    for (IShape s : scene) {
                        if (s.isSelected()) {
                            s.scaleDown();
                        }
                    }
                }
                // undo
                if (ch == 'z') {
                    if (!undo.isEmpty()) {
                        Scene oldScene = undo.pop();
                        redo.push(scene.copy());
                        scene.reload(oldScene);
                    }
                }
                // redo
                if (ch == 'y') {
                    if (!redo.isEmpty()) {
                        Scene newScene = redo.pop();
                        undo.push(scene.copy());
                        scene.reload(newScene);

                    }
                }

                // animate
                if (ch == 'v') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    for (IShape s : scene) {
                        if (s.isSelected()) {
                            final int totalTime = 10000; // 10 seconds
                            final int interval = 300; // 500 milliseconds
                            final int totalIterations = totalTime / interval;

                            Thread timerThread = new Thread(() -> {
                                boolean animationState = true;
                                try {
                                    for (int i = 0; i < totalIterations; i++) {
                                        // Do something with the shape here
                                        if (animationState) {
                                            s.scaleUp();
                                            animationState = false;
                                        } else {
                                            s.scaleDown();
                                            animationState = true;
                                        }
                                        s.animate();
                                        repaint();
                                        Thread.sleep(interval);
                                    }
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            });
                            timerThread.start();
                        }
                    }
                }

                // dance
                if (ch == 'c') {
                    Scene copy = scene.copy();
                    undo.push(copy);
                    for (IShape s : scene) {
                        if (s.isSelected()) {
                            final int totalTime = 5000; // 5 seconds
                            final int interval = 100; // 100 milliseconds
                            final int totalIterations = totalTime / interval;

                            Thread danceThread = new Thread(() -> {
                                try {
                                    for (int i = 0; i < totalIterations; i++) {
                                        // Random direction movement
                                        int dx = (int) (Math.random() * 20) - 10;
                                        int dy = (int) (Math.random() * 20) - 10;
                                        s.move(dx, dy);

                                        repaint();
                                        Thread.sleep(interval);
                                    }
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            });
                            danceThread.start();
                        }
                    }
                }

                // bring one selected forward in the layering
                if (ch == 'f') {
                    scene.bringForward();
                }
                // bring one selected backward in the layering
                if (ch == 'b') {
                    scene.sendBackward();
                }

                repaint();
            }
        });
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        DrawShapes shapes = new DrawShapes(700, 600);
        shapes.setVisible(true);
    }

}
