import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
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
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class MainApplication extends Application {

    private Pane drawPane = new Pane();
    private TextField nField = new TextField();
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
    private final ObservableList<PosTable> data =  FXCollections.observableArrayList();
    private List<Color> colors = newArrayList(Color.ORANGE, Color.YELLOW, Color.GREEN, Color.AQUAMARINE, Color.BLUE, Color.VIOLET, Color.BLACK);

    private Vehicle mainVehicle;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<ScheduledExecutorService> executors = new ArrayList<>();
    private int n = 0;
    private int mode = 0; //0 - ничего, 1 - следование, 2 - повторение, 3 - без алгоритма

    private static final long MAIN_VEHICLE_FREQUENCY = 5000;
    private static final long VEHICLE1_FREQUENCY = 100;
    private static final long GET_COORDINATES_FREQUENCY = 1000;

    public static final float GPS_MEASUREMENT_ERROR = 50;
    public static final int NUMBER_OF_POINTS = 50;
    public static final float ALGORITHM_MEASUREMENT_ERROR = 0.5f;

    private Long timer = 0L;

    private static final String FILE_NAME = "C:\\Users\\podo0716\\sandbox\\master\\src\\main\\resources\\tmp\\data.xlsx";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initGUI(stage);
        final float xPos = 100;
        final float yPos = 50;
        mainVehicle = createVehicle(0L, new Pos(xPos, yPos), Color.RED, drawPane);

        MAIN_RUN_BUTTON.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                n = Integer.parseInt(nField.getText());
                if (follow.isSelected()) {
                    mode
                }


                if (!isEmpty(data)) {
                    final Queue<Pos> mainVehicleQueue = new LinkedList();
                    data.forEach(elem -> {
                        mainVehicleQueue.add(new Pos(Float.valueOf(elem.getXVal()), Float.valueOf(elem.getYVal())));
                    });
                    mainVehicle.setTargetList(mainVehicleQueue);
                }
                for (int i = 1; i < n; i++) {
                    float currentYPos = yPos;
                    vehicles.add(createVehicle(Long.valueOf(i), new Pos(xPos, currentYPos += 50), colors.get(i), drawPane));
                }


            }
        });
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
                        moveVehicle(mainVehicle, 1, mainVehicle.getTargetList());
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, MAIN_VEHICLE_FREQUENCY, TimeUnit.MILLISECONDS);
        return mainVehicleExecutor;
    }

    private void moveVehicle(Vehicle vehicle, int step, Queue<Pos> externalTargetList) throws InterruptedException {
        Queue<Pos> targetList = externalTargetList != null ? externalTargetList : vehicle.getTargetList();
        if (isEmpty(targetList)) {
            return;
        }
        final Pos to = targetList.remove();
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
                System.out.println("Vehicle " + vehicle.getId() + " has reached the goal");
                break;
            }
            Thread.sleep(50);

            Platform.runLater(() -> {
                vehicle.redrawCircle();
                timer++;
                if (timer > 0 && timer < 5) {
                    pane.getChildren().add(new Circle(vehicle1.getCurrentPos().getX(), vehicle1.getCurrentPos().getY(), 1, vehicle1.getCircle().getFill()));
                }
                if (timer > 0 && timer < 20) {
                    pane.getChildren().add(new Circle(vehicle2.getCurrentPos().getX(), vehicle2.getCurrentPos().getY(), 1, vehicle2.getCircle().getFill()));
                }
                if (timer > 0 && timer < 30) {
                    pane.getChildren().add(new Circle(vehicle3.getCurrentPos().getX(), vehicle3.getCurrentPos().getY(), 1, vehicle3.getCircle().getFill()));
                }
                if (timer == 50) {
                    timer = 0L;
                }
                pane.getChildren().add(new Circle(mainVehicle.getCurrentPos().getX(), mainVehicle.getCurrentPos().getY(), 0.5, mainVehicle.getCircle().getFill()));
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
            if (isMainVehicleStopped(cloud, GPS_MEASUREMENT_ERROR*2)) {
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
        if (!isEmpty(cloud) && cloud.size() >1) {
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
        String[] head = {"main X","main Y","vehicle 1 X","vehicle 1 Y","vehicle 2 X","vehicle 2 Y","vehicle 3 X","vehicle 3 Y"};
        int headColNum = 0;
        for (String s : head) {
            Cell cell = heading.createCell(headColNum++);
            cell.setCellValue(s);
        }

        createListRecord(sheet,
                newArrayList(mainVehicle.getList(),vehicle1.getApproximateList(),
                        vehicle2.getApproximateList(), vehicle3.getApproximateList()));

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
        for (int i = 1;i<=maxLength;i++) {
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
            firstColNum+=2;
            secondColNum+=2;
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
        hBox.setMinHeight(800);
        hBox.setMaxHeight(800);
        final Pane controlPane = new Pane();
        drawPane.setMinHeight(800);
        drawPane.setMaxHeight(800);
        Separator hugeSeparator = new Separator();
        hugeSeparator.setOrientation(Orientation.VERTICAL);
        hBox.getChildren().addAll(controlPane, hugeSeparator, drawPane);

        final VBox vBox = new VBox();
        final Label inputNumberOfVehiclesText = new Label("Введите количество машин");
        nField.setMaxWidth(50);
        valueX.setMaxWidth(50);
        valueY.setMaxWidth(50);

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

        vBox.getChildren().addAll(inputNumberOfVehiclesText, nField, follow, repeat, targetTable, addTargetText, targetHBox, separator, space, MAIN_RUN_BUTTON, excel);
        controlPane.getChildren().add(vBox);
        group.getChildren().add(hBox);

        final Scene scene = new Scene(group, 1600, 800);

        stage.setScene(scene);
        stage.show();
    }

    private void shutDownAllExecutors(List<ScheduledExecutorService> executors) {
        executors.forEach(executor -> executor.shutdownNow());
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