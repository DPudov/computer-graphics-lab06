package sample;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

@SuppressWarnings("ALL")
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
    CheckBox delaySlider;

    @FXML
    Button addSeedButton;

    @FXML
    TextField inputXSeedField;
    @FXML
    TextField inputYSeedField;

    protected AnimationTimer timer;

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
                fillPolygon(delaySlider.isSelected());
            } catch (Exception e) {
                setAlert("Произошла ошибка JavaFX Thread при затравке с задержкой!");
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
                System.out.println("Введены неверные данные для новой точки");
            }
        });

        addSeedButton.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                addSeed(Integer.parseInt(inputXSeedField.getText()), Integer.parseInt(inputYSeedField.getText()));

            } catch (NumberFormatException e) {
                setAlert("Введены неверные данные для затравки!");
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

    private void fillPolygon(boolean delay) {
        performFill(delay);
    }

    private void performFill(boolean delay) {
        if (!delay) {
            for (Point seed : seeds) {
                LineByLineSeedAlgorithm(seed);
            }
        } else {
            for (Point seed : seeds) {
                AlgWithDelay(seed);
            }
        }

        seeds.clear();
    }

    private boolean checkSeedPoint(Point seed) {
        int xValue = seed.getX();
        int yValue = seed.getY();
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        return xValue >= 0 && xValue < width && yValue >= 0 && yValue < height;
    }

    private void setAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.setTitle("Произошла ошибка :(");
        alert.setHeaderText("ОШИБКА");
        alert.show();
    }

    private void AlgWithDelay(Point seed) {
        if (!checkSeedPoint(seed)) {
            setAlert("Точка затравки вне границ холста");
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();

        WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), image);
        PixelWriter writer = snapshot.getPixelWriter();

        Color fillColor = fillPicker.getValue();
        Color borderColor = boundPicker.getValue();
        Stack<Point> stack = new Stack<>();
        final Point[] currentPoint = new Point[1];
        stack.push(seed);


        timer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                if (stack.isEmpty()) {
                    timer.stop();
                    return;
                }
                currentPoint[0] = stack.pop();
                int x = currentPoint[0].getX();
                int y = currentPoint[0].getY();
                if (notInCanvasBorder(x, y)) {
                    return;
                }
                writer.setColor(x, y, fillColor);

                int tempX = x;
                // заполняем интервал справа от затравки
                x += 1;
                while (!areEqual(x, y, borderColor, snapshot)) {
                    if (notInCanvasBorder(x, y)) {
                        return;
                    }
                    writer.setColor(x, y, fillColor);
                    x += 1;
                }
                // сохраняем крайний справа пиксел
                int xRight = x - 1;
                x = tempX;
                // заполняем слева от затравки
                x -= 1;
                while (!areEqual(x, y, borderColor, snapshot)) {
                    if (notInCanvasBorder(x, y)) {
                        return;
                    }
                    writer.setColor(x, y, fillColor);
                    x -= 1;
                }

                // сохраняем крайний слева пиксел
                int xLeft = x + 1;
                /*
            Проверим, что строка выше не является ни границей многоугольника, ни уже полностью заполненной
            Если это не так, то найти затравку, начиная с левого края подынтервала сканирующей строки
             */
                x = xLeft;
                y += 1;

                checkLine(snapshot, fillColor, borderColor, stack, x, y, xRight);

            /*
            Проверим, что строка ниже не является ни границей многоугольника, ни уже полностью заполненной
            Если это не так, то найти затравку, начиная с левого края подынтервала сканирующей строки
             */
                x = xLeft;
                y -= 2;
                checkLine(snapshot, fillColor, borderColor, stack, x, y, xRight);
                gc.drawImage(snapshot, 0, 0);
            }
        };

        timer.start();
//        gc.drawImage(snapshot, 0, 0);

    }

    private void LineByLineSeedAlgorithm(Point seed) {
        if (!checkSeedPoint(seed)) {
            setAlert("Точка затравки вне границ холста");
            return;
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();

        WritableImage image = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), image);
        PixelWriter writer = snapshot.getPixelWriter();

        Color fillColor = fillPicker.getValue();
        Color borderColor = boundPicker.getValue();
        Stack<Point> stack = new Stack<>();
        final Point[] currentPoint = new Point[1];
        stack.push(seed);
        while (!stack.isEmpty()) {


            currentPoint[0] = stack.pop();
            int x = currentPoint[0].getX();
            int y = currentPoint[0].getY();
            if (notInCanvasBorder(x, y)) {
                return;
            }
            writer.setColor(x, y, fillColor);

            int tempX = x;
            // заполняем интервал справа от затравки
            x += 1;
            while (!areEqual(x, y, borderColor, snapshot)) {
                if (notInCanvasBorder(x, y)) {
                    return;
                }
                writer.setColor(x, y, fillColor);
                x += 1;
            }
            // сохраняем крайний справа пиксел
            int xRight = x - 1;
            x = tempX;
            // заполняем слева от затравки
            x -= 1;
            while (!areEqual(x, y, borderColor, snapshot)) {
                if (notInCanvasBorder(x, y)) {
                    return;
                }
                writer.setColor(x, y, fillColor);
                x -= 1;
            }

            // сохраняем крайний слева пиксел
            int xLeft = x + 1;
                /*
            Проверим, что строка выше не является ни границей многоугольника, ни уже полностью заполненной
            Если это не так, то найти затравку, начиная с левого края подынтервала сканирующей строки
             */
            x = xLeft;
            y += 1;

            checkLine(snapshot, fillColor, borderColor, stack, x, y, xRight);

            /*
            Проверим, что строка ниже не является ни границей многоугольника, ни уже полностью заполненной
            Если это не так, то найти затравку, начиная с левого края подынтервала сканирующей строки
             */
            x = xLeft;
            y -= 2;
            checkLine(snapshot, fillColor, borderColor, stack, x, y, xRight);

        }


        gc.drawImage(snapshot, 0, 0);
    }

    private void checkLine(WritableImage snapshot, Color fillColor, Color borderColor, Stack<Point> stack, int x, int y, int xRight) {
        if (!areEqual(x, y, borderColor, snapshot) && !areEqual(x, y, fillColor, snapshot)) {
            if (notInCanvasBorder(x, y)) {
                return;
            }
            snapshot.getPixelWriter().setColor(x, y, fillColor);
            stack.push(new Point(x, y));

        }
        while (x <= xRight) {
            boolean flag = false;
            while (!areEqual(x, y, borderColor, snapshot) && !areEqual(x, y, fillColor, snapshot) && x <= xRight) {
                if (!flag) {
                    flag = true;
                }
                x += 1;
            }

            if (flag) {
                if (x == xRight && !areEqual(x, y, borderColor, snapshot) && !areEqual(x, y, fillColor, snapshot)) {
                    stack.push(new Point(x, y));
                } else {
                    stack.push(new Point(x - 1, y));
                }
            }
            // продолжим проверку, если интервал был прерван
            int xEnter = x;
            while ((areEqual(x, y, borderColor, snapshot) || areEqual(x, y, fillColor, snapshot)) && x < xRight) {
                x += 1;
            }
            // удостоверимся, что координата пиксела увеличена
            if (x == xEnter) {
                x += 1;
            }

        }
    }

    private boolean areEqual(int x, int y, Color colorNew, WritableImage image) {
        if (notInCanvasBorder(x, y)) {
            return true;
        }
        Color colorCanvas = image.getPixelReader().getColor(x, y);
        return colorCanvas.equals(colorNew);
    }

    private boolean notInCanvasBorder(int x, int y) {
        return x < 0 || !(x < canvas.getWidth()) || y < 0 || !(y < canvas.getHeight());
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
}
