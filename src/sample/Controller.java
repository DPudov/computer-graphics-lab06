package sample;

import javafx.animation.AnimationTimer;
import javafx.beans.property.IntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

public class Controller {
    @FXML
    Button clearAllButton;
    @FXML
    TextField inputXField;
    @FXML
    TextField inputYField;
    @FXML
    Button addPointButton;

    @FXML
    Canvas canvas;
    @FXML
    Label cursorLabel;

    @FXML
    ColorPicker fillPicker;
    @FXML
    ColorPicker backgroundPicker;
    @FXML
    ColorPicker boundPicker;

    @FXML
    Button fillPolygonButton;
    @FXML
    Button clojureButton;

    @FXML
    Slider delaySlider;

    @FXML
    Button addSeedButton;

    @FXML
    TextField inputXSeedField;
    @FXML
    TextField inputYSeedField;

    AnimationTimer loop;

    private final LinkedList<Point> allVertices = new LinkedList<>();
    private final LinkedList<Edge> allEdges = new LinkedList<>();
    private final LinkedList<Edge> currentPolygon = new LinkedList<>();
    private final ArrayList<Polygon> polygons = new ArrayList<>();
    private final LinkedList<Point> seeds = new LinkedList<>();
    private Edge currentEdge = new Edge(new Point(0, 0), new Point(0, 0));

    @FXML
    public void initialize() {
        setupColors();
        setupCanvasListeners();

        fillPolygonButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                double delay = delaySlider.getValue();
                fillPolygon(delay);
            } catch (Exception e) {
                System.out.println("Wrong values!");
            }
        });

        clojureButton.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            closePolygon();
        });

        clearAllButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            clearCanvas();
            clearData();
        });

        addPointButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                addPoint(Integer.parseInt(inputXField.getText()), Integer.parseInt(inputYField.getText()));
            } catch (NumberFormatException e) {
                System.out.println("Wrong values!");
            }
        });

        addSeedButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                addSeed(Integer.parseInt(inputXSeedField.getText()), Integer.parseInt(inputYSeedField.getText()));

            } catch (NumberFormatException e) {
                System.out.println("Wrong values!");
            }
        });
    }

    private void setupCanvasListeners() {
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            MouseButton b = e.getButton();
            boolean hasShift = e.isShiftDown();
            boolean hasControl = e.isControlDown();
            boolean hasAlt = e.isAltDown();

            if (b == MouseButton.PRIMARY && hasShift && hasControl) {
                //Прямая
                addPoint((int) e.getX(), (int) e.getY());
            } else if (b == MouseButton.PRIMARY && hasShift) {
                // горизонтальная
                addPointHorizontal((int) e.getX(), (int) e.getY());
            } else if (b == MouseButton.PRIMARY && hasControl) {
                // вертикальная
                addPointVertical((int) e.getX(), (int) e.getY());
            } else if (b == MouseButton.PRIMARY && hasAlt) {
                addSeed((int) e.getX(), (int) e.getY());
            } else if (b == MouseButton.PRIMARY) {
                addPoint((int) e.getX(), (int) e.getY());
                // Прямая
            } else if (b == MouseButton.SECONDARY) {
                // замкнуть многоугольник
                closePolygon();
            }

            if (b == MouseButton.PRIMARY) {
                allVertices.add(new Point((int) e.getX(), (int) e.getY()));
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, mouseEvent -> {
            cursorLabel.setText("Координата курсора: " + mouseEvent.getX() + " ; " + mouseEvent.getY());
        });
    }

    private void setupColors() {
        boundPicker.setValue(Color.BLACK);
        fillPicker.setValue(Color.LIME);
        backgroundPicker.setValue(Color.WHITE);
        clearCanvas();
    }

    private void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(backgroundPicker.getValue());
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void clearData() {
        currentEdge.clear();
        polygons.clear();
        currentPolygon.clear();
        allEdges.clear();
        allVertices.clear();
        seeds.clear();
    }

    private void fillPolygon(double delay) {
//        clearCanvas();
        Point topLeft = getMinimumPoint();
        Point bottomRight = getMaximumPoint();
        if (topLeft != null && bottomRight != null) {
            performFill(delay);
        }
//        redrawPolygons();
    }

    private void redrawPolygons() {
        for (Polygon p : polygons) {
            for (Edge e : p.getEdges()) {
                drawLine(e);
            }
        }
        for (Edge e : new Polygon(currentPolygon).getEdges()) {
            drawLine(e);
        }
    }

    private void performFill(double delay) {
        for (Point seed : seeds) {
            SeedAlgorithm(seed);
        }
    }

    private void LineByLineSeedAlgorithm(Point seed) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        PixelWriter writer = gc.getPixelWriter();
        // TODO: simple line by line algo
    }


    private void SeedAlgorithm(Point seed) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        PixelWriter writer = gc.getPixelWriter();
        PixelReader reader = canvas.snapshot(null, null).getPixelReader();
        Color fillColor = fillPicker.getValue();
        Color borderColor = boundPicker.getValue();
        Stack<Point> stack = new Stack<>();
        Point currentPoint;
        Color currentColor;
        stack.push(seed);
        while (!stack.isEmpty()) {
            currentPoint = stack.pop();
            currentColor = reader.getColor(currentPoint.getX(), currentPoint.getY());
            int x = currentPoint.getX();
            int y = currentPoint.getY();
            if (currentColor != fillColor && currentColor != borderColor) {
                writer.setColor(x, y, fillColor);
            }
            if (!isDone(x + 1, y)) {
                stack.push(new Point(x + 1, y));
            }
            if (!isDone(x, y + 1)) {
                stack.push(new Point(x, y + 1));
            }
            if (!isDone(x - 1, y)) {
                stack.push(new Point(x - 1, y));
            }
            if (!isDone(x, y - 1)) {
                stack.push(new Point(x, y - 1));
            }
        }

    }

    private boolean isDone(int x, int y) {
        Color fill = fillPicker.getValue();
        Color border = boundPicker.getValue();
        Color pixel = canvas.snapshot(null, null).getPixelReader().getColor(x, y);
        return pixel.equals(fill) || pixel.equals(border);
    }

    private void closePolygon() {
        if (currentPolygon.size() > 1) {
            addPoint(currentPolygon.get(0).getBegin().getX(), currentPolygon.get(0).getBegin().getY());
            LinkedList<Edge> copy = new LinkedList<>(currentPolygon);
            polygons.add(new Polygon(copy));
            currentPolygon.clear();
            currentEdge.setBeginInit(false);
            currentEdge.setEndInit(false);
        }
    }

    private void addSeed(int x, int y) {
        seeds.add(new Point(x, y));
    }

    private void addPointHorizontal(int x, int y) {
        if (currentEdge.isEndInit()) {
            addPoint(x, currentEdge.getEnd().getY());
        } else if (currentEdge.isBeginInit()) {
            addPoint(x, currentEdge.getBegin().getY());
        } else {
            addPoint(x, y);
        }
    }

    private void addPointVertical(int x, int y) {
        if (currentEdge.isEndInit()) {
            addPoint(currentEdge.getEnd().getX(), y);
        } else if (currentEdge.isBeginInit()) {
            addPoint(currentEdge.getBegin().getX(), y);
        } else {
            addPoint(x, y);
        }
    }

    private void addPoint(int x, int y) {
        if (!currentEdge.isBeginInit()) {
            currentEdge.setBegin(new Point(x, y));
            currentEdge.setBeginInit(true);
        } else if (!currentEdge.isEndInit()) {
            if (!(currentEdge.getBegin().getX() == x && currentEdge.getBegin().getY() == y)) {
                currentEdge.setEnd(new Point(x, y));
                currentEdge.setEndInit(true);
                doUpdate();
            }
        } else {
            currentEdge.setBegin(currentEdge.getEnd());
            currentEdge.setEnd(new Point(x, y));
            doUpdate();
        }
    }

    private void doUpdate() {
        Edge copy = new Edge(currentEdge.getBegin(), currentEdge.getEnd());
        allEdges.add(copy);
        currentPolygon.add(copy);
        drawLine(copy);
        currentEdge = new Edge(new Point(currentEdge.getBegin()), currentEdge.getEnd());
        currentEdge.setBeginInit(true);
        currentEdge.setEndInit(true);
    }

    private void drawLine(Edge edge) {
        Point beg = edge.getBegin();
        Point end = edge.getEnd();
        drawLine(beg.getX(), beg.getY(), end.getX(), end.getY());
    }

    private void drawLine(int xBegin, int yBegin, int xEnd, int yEnd) {
        LineDrawer.DigitalDiffAnalyzeDraw(canvas, xBegin, yBegin, xEnd, yEnd, boundPicker.getValue());
    }


    private Point getMaximumPoint() {
        if (!allEdges.isEmpty()) {
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Edge edge : allEdges) {
                Point currentBegin = edge.getBegin();
                Point currentEnd = edge.getEnd();
                int x0 = currentBegin.getX();
                int y0 = currentBegin.getY();
                int xe = currentEnd.getX();
                int ye = currentEnd.getY();
                maxX = max(x0, xe, maxX);
                maxY = max(y0, ye, maxY);
            }
            return new Point(maxX + 10, maxY + 10);
        }
        return null;
    }

    private Point getMinimumPoint() {
        if (!allEdges.isEmpty()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            for (Edge edge : allEdges) {
                Point currentBegin = edge.getBegin();
                Point currentEnd = edge.getEnd();
                int x0 = currentBegin.getX();
                int y0 = currentBegin.getY();
                int xe = currentEnd.getX();
                int ye = currentEnd.getY();
                minX = min(x0, xe, minX);
                minY = min(y0, ye, minY);
            }
            return new Point(minX - 10, minY - 10);
        }
        return null;
    }

    private int max(int a, int b, int c) {
        if (a > b) {
            if (a > c) {
                return a;
            } else {
                return c;
            }
        } else {
            if (b > c) {
                return b;
            } else {
                return c;
            }
        }
    }

    private int min(int a, int b, int c) {
        if (a < b) {
            if (a < c) {
                return a;
            } else {
                return c;
            }
        } else {
            if (b < c) {
                return b;
            } else {
                return c;
            }
        }
    }
}
