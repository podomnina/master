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

import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;


public class Main extends Application {

    private Pane pane = new Pane();
    private TextField valueX = new TextField();
    private TextField valueY = new TextField();
    private Button ok = new Button("ok");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initGUI(stage);

        final Vehicle mainVehicle = createVehicle(0L, new Pos(10,10), Color.RED, pane);
        final Vehicle vehicle1 = createVehicle(1L, new Pos(10,50), Color.BLUE, pane);

        ok.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                final String valX = valueX.getText();
                final String valY = valueY.getText();
                final Pos newTarget = new Pos(Float.valueOf(valX), Float.valueOf(valY));
                mainVehicle.getTargetList().push(newTarget);
                System.out.println("Added new target: " + newTarget);
            }
        });

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("New scheduled iteration. MainVehicle: " + mainVehicle.getTargetList().size());
                if (!isEmpty(mainVehicle.getTargetList())) {
                    System.out.println("Move main vehicle to new target");
                    try {
                        moveVehicle(mainVehicle, 1, vehicle1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 5, TimeUnit.SECONDS);

        ScheduledExecutorService service1 = Executors.newSingleThreadScheduledExecutor();
        service1.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("New scheduled iteration. Vehicle1: " + vehicle1.getTargetList());
                if (!isEmpty(vehicle1.getTargetList())) {
                    System.out.println("Move vehicle 1 to new target");
                    try {
                        moveVehicle(vehicle1, 1, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

    }

    private void moveVehicle(Vehicle vehicle, int step, Vehicle nextVehicle) throws InterruptedException {
        final Pos to = vehicle.getTargetList().pop();
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
                System.out.println("Vehicle has reached the goal");
                break;
            }
            Thread.sleep(10);

            Platform.runLater(() -> {
                vehicle.redrawCircle();
            });
            if (nextVehicle != null) {
                nextVehicle.getTargetList().push(vehicle.getCurrentPos());
            }
        }
    }

    public Vehicle createVehicle(Long id, Pos initialPos, Color color, Pane pane) {
        final Vehicle vehicle = new Vehicle(id, initialPos, new Stack<Pos>(), new Circle(5, color));
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
        pos.setX(pos.getX()/length);
        pos.setY(pos.getY()/length);
        return pos;
    }

}
