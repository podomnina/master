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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class MainApplication extends Application {

    private Pane drawPane = new Pane();
    private TextField n = new TextField();
    private RadioButton follow = new RadioButton("Следовать друг за другом");
    private RadioButton repeat = new RadioButton("Повторять траекторию главного ТС");
    private TextField valueX = new TextField();
    private TextField valueY = new TextField();
    private Button ok = new Button("ok");
    private Button excel = new Button("Записать результаты в excel");
    private TableView<PosTable> targetTable = new TableView<PosTable>();
    private Button addTargetButton = new Button("Добавить");
    private Button MAIN_RUN_BUTTON = new Button("Запустить");
    private Button space = new Button();
    private final ObservableList<PosTable> data =  FXCollections.observableArrayList();

    private Vehicle mainVehicle;
    private List<Vehicle> vehicles;
    private List<Color> colors = newArrayList(Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE);

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

        mainVehicle = createVehicle(0L, new Pos(100, 50), Color.RED, drawPane);

        ok.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                final String valX = valueX.getText();
                final String valY = valueY.getText();
                final Pos newTarget = new Pos(Float.valueOf(valX), Float.valueOf(valY));
                mainVehicle.getTargetList().add(newTarget);
                System.out.println("Added new target: " + newTarget);
            }
        });
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
        n.setMaxWidth(50);
        valueX.setMaxWidth(50);
        valueY.setMaxWidth(50);

        final ToggleGroup toggleGroup = new ToggleGroup();
        follow.setToggleGroup(toggleGroup);
        follow.setSelected(true);
        repeat.setToggleGroup(toggleGroup);

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

        vBox.getChildren().addAll(inputNumberOfVehiclesText, n, follow, repeat, targetTable, addTargetText, targetHBox, separator, space, MAIN_RUN_BUTTON, excel);
        controlPane.getChildren().add(vBox);
        group.getChildren().add(hBox);

        final Scene scene = new Scene(group, 1600, 800);

        stage.setScene(scene);
        stage.show();
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