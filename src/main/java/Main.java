import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import model.Pos;
import model.Vehicle;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;


public class Main extends Application {

    private Pane pane = new Pane();
    private TextField valueX = new TextField();
    private TextField valueY = new TextField();
    private Button ok = new Button("ok");

    private Vehicle mainVehicle;
    private Vehicle vehicle1;
    private Vehicle vehicle2;

    private static final long MAIN_VEHICLE_FREQUENCY = 5000;
    private static final long VEHICLE1_FREQUENCY = 100;
    private static final long GET_COORDINATES_FREQUENCY = 1000;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initGUI(stage);

        mainVehicle = createVehicle(0L, new Pos(10,10), Color.RED, pane);
        vehicle1 = createVehicle(1L, new Pos(10,50), Color.BLUE, pane);
        vehicle2 = createVehicle(2L, new Pos(10,100), Color.GREEN, pane);

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

        runMainVehicleExecutor();
        getCoordinatesExecutor();
        runVehicle1Executor();
        runVehicle2Executor();
    }

    private void runMainVehicleExecutor() {
        ScheduledExecutorService mainVehicleExecutor = Executors.newSingleThreadScheduledExecutor();
        mainVehicleExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                System.out.println("New scheduled iteration. MainVehicle: " + mainVehicle.getTargetList().size());
                if (!isEmpty(mainVehicle.getTargetList())) {
                    System.out.println("Move main vehicle to new target");
                    try {
                        moveVehicle(mainVehicle, 1);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, MAIN_VEHICLE_FREQUENCY, TimeUnit.MILLISECONDS);
    }

    private void runVehicle1Executor() {
        ScheduledExecutorService vehicle1Executor = Executors.newSingleThreadScheduledExecutor();
        vehicle1Executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                System.out.println("New scheduled iteration. Vehicle1: " + vehicle1.getTargetList());
                if (!isEmpty(vehicle1.getTargetList())) {
                    System.out.println("Move vehicle 1 to new target");
                    try {
                        moveVehicle(vehicle1, 1);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, VEHICLE1_FREQUENCY, TimeUnit.MILLISECONDS);
    }

    private void runVehicle2Executor() {
        ScheduledExecutorService vehicle2Executor = Executors.newSingleThreadScheduledExecutor();
        vehicle2Executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                System.out.println("New scheduled iteration. Vehicle2: " + vehicle2.getTargetList());
                if (!isEmpty(vehicle2.getTargetList())) {
                    System.out.println("Move vehicle 2 to new target");
                    try {
                        moveVehicle(vehicle2, 1);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, VEHICLE1_FREQUENCY, TimeUnit.MILLISECONDS);
    }

    private void getCoordinatesExecutor() {
        ScheduledExecutorService getCoordinates = Executors.newSingleThreadScheduledExecutor();
        getCoordinates.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                System.out.println("Get coordinates of main vehicle");
                if (isEmpty(vehicle1.getTargetList()) || !mainVehicle.getCurrentPos().equals(vehicle1.getTargetList().element())) {
                    vehicle1.getTargetList().add(mainVehicle.getCurrentPosWithMeasurementError());
                }
                if (isEmpty(vehicle2.getTargetList()) || !vehicle1.getCurrentPos().equals(vehicle2.getTargetList().element())) {
                    vehicle2.getTargetList().add(vehicle1.getCurrentPosWithMeasurementError());
                }
            }
        }, 0, GET_COORDINATES_FREQUENCY, TimeUnit.MILLISECONDS);
    }

    private void moveVehicle(Vehicle vehicle, int step) throws InterruptedException {
        final Pos to = vehicle.getTargetList().remove();
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
            });
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

        final Label inputX = new Label("Input target x:");
        final Label inputY = new Label("Input target y:");
        final VBox vBox = new VBox();

        pane.setMinSize(600,300);
        pane.setMaxSize(600,300);

        vBox.getChildren().addAll(inputX, valueX, inputY, valueY, ok, pane);
        group.getChildren().add(vBox);

        final Scene scene = new Scene(group, 600, 400);

        stage.setScene(scene);
        stage.show();
    }

    private Pos getUnitVector(Pos from, Pos to) {
        final Pos pos = new Pos();
        pos.setX(to.getX() - from.getX());
        pos.setY(to.getY() - from.getY());
        final float length = (float) Math.sqrt(pos.getX()*pos.getX() + pos.getY()*pos.getY());
        if (length > 0.05) {
            pos.setX(pos.getX() / length);
            pos.setY(pos.getY() / length);
        } else {
            System.out.println("Error! Length is zero!");
            return new Pos(0,0);
        }
        return pos;
    }

}
