import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import model.Pos;
import model.Vehicle;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class MainApplication extends Application {

    private Pane drawPane = new Pane();
    private TextField nField = new TextField();
    private TextField gpsErrorField = new TextField();
    private TextField numberOfPointsField = new TextField();
    private TextField algorithmErrorField = new TextField();
    private RadioButton follow = new RadioButton("Следовать друг за другом");
    private RadioButton repeat = new RadioButton("Повторять траекторию главного ТС");
    private RadioButton turnOffAlgorithm = new RadioButton("Без алгоритма");
    private TextField valueX = new TextField();
    private TextField valueY = new TextField();
    private Button excel = new Button("Записать результаты в excel");
    private TableView<PosTable> targetTable = new TableView<PosTable>();
    private Button addTargetButton = new Button("Добавить");
    private Button MAIN_RUN_BUTTON = new Button("Запустить");
    private Button space = new Button();
    private final ObservableList<PosTable> data = FXCollections.observableArrayList();
    private List<Color> colors = newArrayList(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.AQUAMARINE, Color.BLUE, Color.VIOLET, Color.BLACK);

    private Vehicle mainVehicle;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<ScheduledExecutorService> executors = new ArrayList<>();
    private int n = 0;
    private int mode = 0; //0 - ничего, 1 - следование, 2 - повторение, 3 - без алгоритма

    private static final long MAIN_VEHICLE_FREQUENCY = 5000;
    private static final long VEHICLE_FREQUENCY = 100;
    private static final long GET_COORDINATES_FREQUENCY = 1000;

    public static float GPS_MEASUREMENT_ERROR = 50;
    public static int NUMBER_OF_POINTS = 50;
    public static float ALGORITHM_MEASUREMENT_ERROR = 0.5f;

    private Long timer = 0L;

    private static final String FILE_NAME = "C:/Users/domni/IdeaProjects/master/src/main/resources/tmp/data.xlsx";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initGUI(stage);
        final float xPos = 100;
        final float yPos = 100;
        mainVehicle = createVehicle(0L, new Pos(xPos, yPos), Color.RED, drawPane);
        vehicles.add(mainVehicle);

        MAIN_RUN_BUTTON.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                n = Integer.parseInt(nField.getText());
                if (!StringUtils.isEmpty(gpsErrorField.getText())) {
                    GPS_MEASUREMENT_ERROR = Float.parseFloat(gpsErrorField.getText());
                }
                if (!StringUtils.isEmpty(numberOfPointsField.getText())) {
                    NUMBER_OF_POINTS = Integer.parseInt(numberOfPointsField.getText());
                }
                if (!StringUtils.isEmpty(algorithmErrorField.getText())) {
                    ALGORITHM_MEASUREMENT_ERROR = Float.parseFloat(algorithmErrorField.getText());
                }
                if (follow.isSelected()) {
                    mode = 1;
                } else if (repeat.isSelected()) {
                    mode = 2;
                } else if (turnOffAlgorithm.isSelected()) {
                    mode = 3;
                } else {
                    mode = 0;
                }
                //Берем таргет лист из списка
                if (!isEmpty(data)) {
                    final Queue<Pos> mainVehicleQueue = new LinkedList();
                    data.forEach(elem -> {
                        mainVehicleQueue.add(new Pos(Float.valueOf(elem.getXVal()), Float.valueOf(elem.getYVal())));
                    });
                    mainVehicle.setTargetList(mainVehicleQueue);
                    mainVehicle.getList().add(mainVehicle.getCurrentPos());
                    mainVehicle.getList().addAll(mainVehicleQueue);
                }
                Queue queue = new LinkedList();
                queue.add(new Pos(1000,100));
                queue.add(new Pos(1100,300));
                queue.add(new Pos(1000,500));
                queue.add(new Pos(200,500));
                queue.add(new Pos(100,700));
                queue.add(new Pos(200,800));
                queue.add(new Pos(900,800));
                mainVehicle.setTargetList(queue);
                mainVehicle.getList().add(mainVehicle.getCurrentPos());
                mainVehicle.getList().addAll(queue);

                //Создание ведомых ТС
                float currentYPos = yPos;
                for (int i = 1; i < n; i++) {
                    vehicles.add(createVehicle(Long.valueOf(i), new Pos(xPos, currentYPos += 50), colors.get(i), drawPane));
                }

                ScheduledExecutorService getCoordinatesExecutor = getCoordinatesExecutor();
                ScheduledExecutorService mainVehicleExecutor = runMainVehicleExecutor();
                executors.addAll(newArrayList(getCoordinatesExecutor, mainVehicleExecutor));
                for (int i = 1; i < vehicles.size(); i++) {
                    executors.add(createVehicleExecutor(vehicles.get(i)));
                }
            }
        });
        excel.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                shutDownAllExecutors(executors);
                writeToExcel();
            }
        });
    }

    private ScheduledExecutorService createVehicleExecutor(Vehicle vehicle) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                //System.out.println("New scheduled iteration. Vehicle2: " + vehicle2.getTargetList());
                if (!isEmpty(vehicle.getTargetList())) {
                    //System.out.println("Move vehicle 2 to new target");
                    try {
                        calculateWayByMode(vehicle);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, VEHICLE_FREQUENCY, TimeUnit.MILLISECONDS);
        return executor;
    }

    private void calculateWayByMode(Vehicle vehicle) throws InterruptedException {
        //0 - ничего, 1 - следование, 2 - повторение, 3 - без алгоритма
        switch (mode) {
            case 1:
                approximateWay(vehicle, NUMBER_OF_POINTS, ALGORITHM_MEASUREMENT_ERROR);
                moveVehicle(vehicle, 1, vehicle.getApproximateTargetList(), false, null);
                break;
            case 2:
                if (vehicle.getId() == 1) {
                    approximateWay(vehicle, NUMBER_OF_POINTS, ALGORITHM_MEASUREMENT_ERROR);
                    for (int i = 2; i < vehicles.size(); i++) {
                        vehicles.get(i).getTargetList().addAll(vehicle.getApproximateTargetList());
                    }
                    moveVehicle(vehicle, 1, vehicle.getApproximateTargetList(), false, null);
                } else {
                    moveVehicle(vehicle, 1, null, true, vehicles.get(1));
                }
                break;
            case 3:
                moveVehicle(vehicle, 1, null, false, null);
                break;
            default:
                break;
        }
    }

    private void getCoordinatesByMode(final Vehicle vehicle, Pos pos) {
        //0 - ничего, 1 - следование, 2 - повторение, 3 - без алгоритма
        switch (mode) {
            case 1:
                vehicle.getTargetList().add(pos);
                break;
            case 2:
                vehicle.getList().add(pos);
                break;
            case 3:
                vehicle.getTargetList().add(pos);
                break;
            default:
                break;
        }
    }

    private ScheduledExecutorService getCoordinatesExecutor() {
        ScheduledExecutorService getCoordinates = Executors.newSingleThreadScheduledExecutor();
        getCoordinates.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                //System.out.println("Get coordinates of main vehicle");
                Pos etalonPos = mainVehicle.getCurrentPosWithMeasurementError(GPS_MEASUREMENT_ERROR);
                if (!isEmpty(vehicles) && vehicles.size() > 1) {
                    vehicles.get(1).getTargetList().add(etalonPos);
                    vehicles.get(1).getList().add(etalonPos);
                    if (vehicles.size() > 2) {
                        for (int i = 2; i < vehicles.size(); i++) {
                            getCoordinatesByMode(vehicles.get(i), vehicles.get(i - 1).getCurrentPosWithMeasurementError(GPS_MEASUREMENT_ERROR));
                        }
                    }
                }
            }
        }, 0, GET_COORDINATES_FREQUENCY, TimeUnit.MILLISECONDS);
        return getCoordinates;
    }

    private ScheduledExecutorService runMainVehicleExecutor() {
        ScheduledExecutorService mainVehicleExecutor = Executors.newSingleThreadScheduledExecutor();
        mainVehicleExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                //System.out.println("New scheduled iteration. MainVehicle: " + mainVehicle.getTargetList().size());
                if (!isEmpty(mainVehicle.getTargetList())) {
                    //System.out.println("Move main vehicle to new target");
                    try {
                        System.out.println("Main vehicle coordinates: " + mainVehicle.getCurrentPos());
                        moveVehicle(mainVehicle, 1, mainVehicle.getTargetList(), false, null);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, MAIN_VEHICLE_FREQUENCY, TimeUnit.MILLISECONDS);
        return mainVehicleExecutor;
    }

    private void moveVehicle(Vehicle vehicle, int step, Queue<Pos> externalTargetList, boolean convergence, Vehicle previousVehicle) throws InterruptedException {
        Queue<Pos> targetList = externalTargetList != null ? externalTargetList : vehicle.getTargetList();
        if (isEmpty(targetList)) {
            return;
        }
        Pos to;
        if (convergence) {
            to = targetList.element();
            to = convergenceTwoWays(vehicle, previousVehicle, to);
            if (to == null) {
                return;
            } else {
                targetList.remove(to);
            }
        } else {
            to = targetList.remove();
        }
        float distance = getDistance(to, vehicle.getCurrentPos());
        System.out.println("Distance: " + distance);
        while (true) {
            final Pos from = vehicle.getCurrentPos();
            final Pos unitVector = getUnitVector(from, to);
            final Pos currentPoint = vehicle.getCurrentPos();
            final Pos nextPoint = new Pos((currentPoint.getX() + unitVector.getX() * step),
                    (currentPoint.getY() + unitVector.getY() * step));
            vehicle.setCurrentPos(nextPoint);
            if (((nextPoint.getX() <= to.getX() + 0.5) && (nextPoint.getX() >= to.getX() - 0.5))
                    && ((nextPoint.getY() <= to.getY() + 0.5) && (nextPoint.getY() >= to.getY() - 0.5))) {
                vehicle.setCurrentPos(to);
                vehicle.setDistance(vehicle.getDistance() + distance);
                System.out.println("Vehicle " + vehicle.getId() + " has reached the goal with distance: " + vehicle.getDistance());
                break;
            }
            Thread.sleep(50);

            Platform.runLater(() -> {
                vehicle.redrawCircle();
                timer++;
                long maxTimer = vehicles.size() * 10;
                vehicles.forEach(v -> {
                    if (timer > 0 && timer < v.getId() * 10) {
                        drawPane.getChildren().add(new Circle(v.getCurrentPos().getX(), v.getCurrentPos().getY(), 1, v.getCircle().getFill()));
                    }
                    if (timer == maxTimer) {
                        timer = 0L;
                    }
                });

                drawPane.getChildren().add(new Circle(mainVehicle.getCurrentPos().getX(), mainVehicle.getCurrentPos().getY(), 0.5, mainVehicle.getCircle().getFill()));
            });
        }
    }


    private Pos getUnitVector(Pos from, Pos to) {
        final Pos pos = new Pos();
        pos.setX(to.getX() - from.getX());
        pos.setY(to.getY() - from.getY());
        final float length = (float) Math.sqrt(pos.getX() * pos.getX() + pos.getY() * pos.getY());
        if (length > 0.05) {
            pos.setX(pos.getX() / length);
            pos.setY(pos.getY() / length);
        } else {
            System.out.println("Error! Length is zero! From:" + from + " To:" + to);
            return new Pos(0, 0);
        }
        return pos;
    }

    private void approximateWay(Vehicle vehicle, int part, float E) {
        if (!isEmpty(vehicle.getTargetList())) {
            final Queue<Pos> approximateWay = new LinkedList<>();
            final List<Pos> hugeCloud = newArrayList(vehicle.getTargetList());
            if (part > hugeCloud.size()) {
                part = hugeCloud.size();
            }
            List<Pos> cloud = hugeCloud.subList(0, part);
            if (isMainVehicleStopped(cloud, GPS_MEASUREMENT_ERROR * 2)) {
                return;
            }
            float angle = getLineProperties(cloud);
            float a = (float) Math.tan(angle);
            Pos centralPoint = getCentralPointInCloud(cloud);
            float b = centralPoint.getY() - a * centralPoint.getX();
            approximateWay.add(centralPoint);

            int upperBound = part;
            int bottomBound = 2 * part;
            int stopAlgorithm = (int) Math.ceil(hugeCloud.size() / part);
            for (int q = 1; q < stopAlgorithm; q++) {
                cloud = hugeCloud.subList(upperBound, bottomBound);
                angle = getLineProperties(cloud);
                float a1 = (float) Math.tan(angle);
                centralPoint = getCentralPointInCloud(cloud);
                float b1 = centralPoint.getY() - a1 * centralPoint.getX();
                float currentE = checkRejection(a1, b1, cloud);
                if (currentE > E) {
                    Pos intersectionPoint = getIntersectionPoint(a1, b1, a, b);
                    if (checkOutOfArea(cloud, intersectionPoint)) {
                        a = a1;
                        b = b1;
                        approximateWay.add(intersectionPoint);
                    } else {
                        approximateWay.add(centralPoint);
                    }
                } else {
                    approximateWay.add(centralPoint);
                }
                upperBound = upperBound + part;
                bottomBound = bottomBound + part;
            }

            vehicle.setApproximateTargetList(approximateWay);
            vehicle.getApproximateList().addAll(approximateWay);
            vehicle.getTargetList().clear();
        }
    }

    private float getLineProperties(List<Pos> cloud) {
        float angle = 0;
        final Pos centralPoint = getCentralPointInCloud(cloud);
        if (centralPoint != null) {
            angle = getAngle(centralPoint, cloud);
            float f1 = getValueOfObjectiveFunction(angle, centralPoint, cloud);
            float f2 = getValueOfObjectiveFunction((float) (angle + Math.PI / 2), centralPoint, cloud);
            if (f2 < f1) {
                angle = (float) (angle + Math.PI / 2);
            }
        }
        return angle;
    }

    private Pos getCentralPointInCloud(List<Pos> cloud) {
        if (!isEmpty(cloud)) {
            float sumX = 0;
            float sumY = 0;
            int n = cloud.size();
            for (int i = 0; i < n; i++) {
                final Pos pos = cloud.get(i);
                sumX = sumX + pos.getX();
                sumY = sumY + pos.getY();
            }
            return new Pos(sumX / n, sumY / n);
        }
        return null;
    }

    private float getAngle(Pos centralPoint, List<Pos> cloud) {
        if (!isEmpty(cloud) && centralPoint != null) {
            int n = cloud.size();
            float sumXY = 0;
            float sumXY2 = 0;
            for (int i = 0; i < n; i++) {
                Pos point = cloud.get(i);
                float divX = centralPoint.getX() - point.getX();
                float divY = centralPoint.getY() - point.getY();
                sumXY = sumXY + divX * divY;
                sumXY2 = sumXY2 + (divX * divX - divY * divY);
            }
            return (float) (Math.atan(2 * sumXY / sumXY2) / 2);
        }

        return 0;
    }

    private float getValueOfObjectiveFunction(float angle, Pos centralPoint, List<Pos> cloud) {
        float f = 0;
        int n = cloud.size();
        for (int i = 0; i < n; i++) {
            Pos point = cloud.get(i);
            float divX = centralPoint.getX() - point.getX();
            float divY = centralPoint.getY() - point.getY();
            f = (float) (f + Math.pow(Math.cos(angle) * divY - Math.sin(angle) * divX, 2));
        }
        return f;
    }

    private float checkRejection(float a, float b, List<Pos> cloud) {
        float E = 0;
        if (!isEmpty(cloud)) {
            for (int i = 0; i < cloud.size(); i++) {
                Pos point = cloud.get(i);
                float f = a * point.getX() + b;
                E = (float) (E + Math.pow(f - point.getY(), 2));
            }
            E = E / cloud.size();
        }
        return E;
    }

    private Pos getIntersectionPoint(float a1, float b1, float a2, float b2) {
        if (a1 != a2 && b1 != b2) {
            float x = (b2 - b1) / (a1 - a2);
            float y = a1 * x + b1;
            return new Pos(x, y);
        }
        return null;
    }

    private boolean checkOutOfArea(List<Pos> cloud, Pos intersectionPoint) {
        final Comparator<Pos> compX = (p1, p2) -> Float.compare(p1.getX(), p2.getX());
        final Comparator<Pos> compY = (p1, p2) -> Float.compare(p1.getY(), p2.getY());
        float maxBordersX = cloud.stream().max(compX).get().getX();
        float maxBordersY = cloud.stream().max(compY).get().getY();
        float minBordersX = cloud.stream().min(compX).get().getX();
        float minBordersY = cloud.stream().min(compY).get().getY();
        if (intersectionPoint.getX() < maxBordersX
                && intersectionPoint.getX() > minBordersX
                && intersectionPoint.getY() < maxBordersY
                && intersectionPoint.getY() > minBordersY) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isMainVehicleStopped(List<Pos> cloud, float measurementError) {
        if (!isEmpty(cloud) && cloud.size() > 1) {
            final Comparator<Pos> compX = (p1, p2) -> Float.compare(p1.getX(), p2.getX());
            final Comparator<Pos> compY = (p1, p2) -> Float.compare(p1.getY(), p2.getY());
            float maxBordersX = cloud.stream().max(compX).get().getX();
            float maxBordersY = cloud.stream().max(compY).get().getY();
            float minBordersX = cloud.stream().min(compX).get().getX();
            float minBordersY = cloud.stream().min(compY).get().getY();

            float delX = maxBordersX - minBordersX;
            float delY = maxBordersY - minBordersY;
            return delX < measurementError && delY < measurementError;
        }
        return false;
    }

    public void writeToExcel() {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Data");

        int rowNum = 0;
        System.out.println("Creating excel");

        Row heading = sheet.createRow(rowNum++);
        List<String> headers = new ArrayList<>();
        vehicles.forEach(vehicle -> {
            headers.add("Vehicle " + vehicle.getId() + " X");
            headers.add("Vehicle " + vehicle.getId() + " Y");
        });
        int headColNum = 0;
        for (String s : headers) {
            Cell cell = heading.createCell(headColNum++);
            cell.setCellValue(s);
        }

        final List<List<Pos>> listOfLists = new ArrayList<>();
        listOfLists.add(mainVehicle.getList());
        if (mode == 1 || mode == 2) {
            for (int i = 1; i < vehicles.size(); i++) {
                listOfLists.add(vehicles.get(i).getApproximateList());
            }
        } else if (mode == 3 ) {
            for (int i = 1; i < vehicles.size(); i++) {
                listOfLists.add(vehicles.get(i).getList());
            }
        }

        createListRecord(sheet, listOfLists);

        try {
            FileOutputStream outputStream = new FileOutputStream(FILE_NAME);
            workbook.write(outputStream);
            workbook.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Done");
    }

    private void createListRecord(XSSFSheet sheet, List<List<Pos>> list) {
        int rowNum = 1;
        int firstColNum = 0;
        int secondColNum = 1;
        int maxLength = 0;
        for (List<Pos> l : list) {
            if (l.size() > maxLength) {
                maxLength = l.size();
            }
        }
        for (int i = 1; i <= maxLength; i++) {
            sheet.createRow(i);
        }
        for (List<Pos> data : list) {
            for (Pos pos : data) {
                Row row = sheet.getRow(rowNum++);
                Cell cell1 = row.createCell(firstColNum);
                cell1.setCellValue(pos.getX());
                Cell cell2 = row.createCell(secondColNum);
                cell2.setCellValue(pos.getY());
            }
            firstColNum += 2;
            secondColNum += 2;
            rowNum = 1;
        }
    }

    public Vehicle createVehicle(Long id, Pos initialPos, Color color, Pane pane) {
        final Vehicle vehicle = new Vehicle(id, initialPos, new LinkedList<>(), new Circle(5, color));
        vehicle.getCircle().setCenterX(initialPos.getX());
        vehicle.getCircle().setCenterY(initialPos.getY());
        pane.getChildren().add(vehicle.getCircle());
        System.out.println("Created new vehicle with id: " + id);
        return vehicle;
    }

    private void initGUI(Stage stage) {
        final Group group = new Group();

        final HBox hBox = new HBox();
        hBox.setMinHeight(900);
        hBox.setMaxHeight(900);
        final Pane controlPane = new Pane();
        drawPane.setMinSize(1300, 900);
        drawPane.setMaxSize(1300, 900);
        drawPane.setLayoutX(150);
        drawPane.setLayoutY(100);
        controlPane.setMaxSize(300, 900);
        controlPane.setMinSize(300, 900);
        Separator hugeSeparator = new Separator();
        hugeSeparator.setOrientation(Orientation.VERTICAL);
        hBox.getChildren().addAll(drawPane, hugeSeparator, controlPane);

        final NumberAxis xAxis = new NumberAxis(0,1300,100);
        xAxis.setSide(Side.TOP);
        final NumberAxis yAxis = new NumberAxis(0,900,100);

        //creating the chart
        final LineChart<Number,Number> lineChart =
                new LineChart<Number,Number>(xAxis,yAxis);



        lineChart.setMinSize(1300, 900);
        lineChart.setMaxSize(1300, 900);

        drawPane.getChildren().add(lineChart);

        final VBox vBox = new VBox();
        final Label inputNumberOfVehiclesText = new Label("Введите количество машин");
        nField.setMaxWidth(50);
        valueX.setMaxWidth(50);
        valueY.setMaxWidth(50);
        final Label gpsErrorText = new Label("Введите погрешность GPS");
        final Label numberOfPointsText = new Label("Введите количество точек");
        final Label algorithmErrorText = new Label("Введите погрешность алгоритма");

        final ToggleGroup toggleGroup = new ToggleGroup();
        follow.setToggleGroup(toggleGroup);
        follow.setSelected(true);
        repeat.setToggleGroup(toggleGroup);
        turnOffAlgorithm.setToggleGroup(toggleGroup);

        createTargetTable(targetTable);

        final Label addTargetText = new Label("Введите новую цель");

        final HBox targetHBox = new HBox();
        final Label xText = new Label("x  ");
        final Label yText = new Label("y  ");
        targetHBox.getChildren().addAll(xText, valueX, yText, valueY, addTargetButton);
        space.setDisable(true);
        Separator separator = new Separator();

        addTargetButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                data.add(new PosTable(valueX.getText(), valueY.getText()));
                valueX.clear();
                valueY.clear();
            }
        });

        vBox.getChildren().addAll(inputNumberOfVehiclesText, nField, gpsErrorText, gpsErrorField, numberOfPointsText, numberOfPointsField , algorithmErrorText, algorithmErrorField,
                follow, repeat, turnOffAlgorithm, targetTable, addTargetText, targetHBox, separator, space, MAIN_RUN_BUTTON, excel);
        controlPane.getChildren().add(vBox);
        group.getChildren().add(hBox);

        final Scene scene = new Scene(group, 1600, 900);

        stage.setScene(scene);
        stage.show();
    }

    private void shutDownAllExecutors(List<ScheduledExecutorService> executors) {
        if (!isEmpty(executors)) {
            executors.forEach(executor -> executor.shutdownNow());
        }
    }

    private Pos convergenceTwoWays(final Vehicle vehicle, final Vehicle previousVehicle, final Pos nextPoint) {
        final Pos currentPos = vehicle.getCurrentPos();
        final Pos previousVehicleCurrentPos = previousVehicle.getCurrentPosWithMeasurementError(GPS_MEASUREMENT_ERROR);
        if (nextPoint != null && currentPos != null && previousVehicleCurrentPos != null) {
            float firstDist = getDistance(currentPos, previousVehicleCurrentPos);
            float secondDist = getDistance(currentPos, nextPoint);
            System.out.println("First dist: " + firstDist + " Second dist: " + secondDist);
            if (firstDist > secondDist || firstDist > GPS_MEASUREMENT_ERROR + 5) {
                return nextPoint;
            }
        }
        return null;
    }

    private float getDistance(final Pos first, final Pos second) {
        final float delX = first.getX() - second.getX();
        final float delY = first.getY() - second.getY();
        if (Math.abs(delX) > 0.05 || Math.abs(delY) > 0.05) {
            return (float) Math.sqrt(delX * delX + delY * delY);
        } else
            return 0;
    }

    private void createTargetTable(TableView table) {
        table.setEditable(true);
        table.setMaxWidth(250);
        table.setMinWidth(250);

        TableColumn xCol = new TableColumn("x");
        xCol.setMinWidth(125);
        xCol.setMaxWidth(125);
        xCol.setCellValueFactory(new PropertyValueFactory<PosTable, String>("xVal"));

        TableColumn yCol = new TableColumn("y");
        yCol.setMinWidth(125);
        yCol.setMaxWidth(125);
        yCol.setCellValueFactory(new PropertyValueFactory<PosTable, String>("yVal"));

        table.setItems(data);
        table.getColumns().addAll(xCol, yCol);
    }

    public static class PosTable {
        private final SimpleStringProperty xVal;
        private final SimpleStringProperty yVal;

        public PosTable(String x, String y) {
            this.xVal = new SimpleStringProperty(x);
            this.yVal = new SimpleStringProperty(y);
        }

        public String getXVal() {
            return xVal.get();
        }

        public void setXVal(String fName) {
            xVal.set(fName);
        }

        public String getYVal() {
            return yVal.get();
        }

        public void setYVal(String fName) {
            yVal.set(fName);
        }
    }

}