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

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
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
                        moveVehicle(mainVehicle, 1, mainVehicle.getTargetList());
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
                        approximateWay(vehicle1, 30, 0.5f);
                        moveVehicle(vehicle1, 1, vehicle1.getApproximateTargetList());
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
                        moveVehicle(vehicle2, 1, vehicle2.getTargetList());
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

    private void moveVehicle(Vehicle vehicle, int step, Queue<Pos> externalTargetList) throws InterruptedException {
        Queue<Pos> targetList = externalTargetList != null ? externalTargetList : vehicle.getTargetList();
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
                pane.getChildren().add(new Circle(vehicle.getCurrentPos().getX(), vehicle.getCurrentPos().getY(), 1, vehicle.getCircle().getFill()));
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

    private void approximateWay(Vehicle vehicle, int part, float E) {
        if (!isEmpty(vehicle.getTargetList())) {
            final Queue<Pos> approximateWay = new LinkedList<>();
            final List<Pos> hugeCloud = newArrayList(vehicle.getTargetList());
            if (part>hugeCloud.size()) {
                part = hugeCloud.size();
            }
            List<Pos> cloud = hugeCloud.subList(0, part);
            float angle = getLineProperties(cloud);
            float a = (float) Math.tan(angle);
            Pos centralPoint = getCentralPointInCloud(cloud);
            float b = centralPoint.getY() - a*centralPoint.getX();
            approximateWay.add(centralPoint);

            int upperBound = part;
            int bottomBound = 2*part;
            int stopAlgorithm = (int) Math.ceil(hugeCloud.size()/part);
            for (int q = 1; q<stopAlgorithm;q++) {
                cloud = hugeCloud.subList(upperBound, bottomBound);
                angle = getLineProperties(cloud);
                float a1 = (float) Math.tan(angle);
                centralPoint = getCentralPointInCloud(cloud);
                float b1 = centralPoint.getY() - a1*centralPoint.getX();
                float currentE = checkRejection(a1,b1,cloud);
                if (currentE > E) {
                    Pos intersectionPoint = getIntersectionPoint(a1,b1,a,b);
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
        }
    }

    private float getLineProperties(List<Pos> cloud) {
        float angle = 0;
        final Pos centralPoint = getCentralPointInCloud(cloud);
        if (centralPoint != null) {
            angle = getAngle(centralPoint, cloud);
            float f1 = getValueOfObjectiveFunction(angle, centralPoint, cloud);
            float f2 = getValueOfObjectiveFunction((float) (angle+Math.PI/2), centralPoint, cloud);
            if (f2<f1) {
                angle = (float) (angle + Math.PI/2);
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
            return new Pos(sumX/n, sumY/n);
        }
        return null;
    }

    private float getAngle(Pos centralPoint, List<Pos> cloud) {
        if (!isEmpty(cloud) && centralPoint != null) {
            int n = cloud.size();
            float sumXY = 0;
            float sumXY2 = 0;
            for (int i = 0; i< n; i++) {
                Pos point = cloud.get(i);
                float divX = centralPoint.getX() - point.getX();
                float divY = centralPoint.getY() - point.getY();
                sumXY = sumXY + divX*divY;
                sumXY2 = sumXY2 + (divX*divX - divY*divY);
            }
            return (float) (Math.atan(2*sumXY/sumXY2)/2);
        }

        return 0;
    }

    private float getValueOfObjectiveFunction(float angle, Pos centralPoint, List<Pos> cloud) {
        float f = 0;
        int n = cloud.size();
        for (int i = 0; i<n ; i++) {
            Pos point = cloud.get(i);
            float divX = centralPoint.getX() - point.getX();
            float divY = centralPoint.getY() - point.getY();
            f = (float) (f + Math.pow(Math.cos(angle)*divY - Math.sin(angle)*divX,2));
        }
        return f;
    }

    private float checkRejection(float a, float b, List<Pos> cloud) {
        float E = 0;
        if (!isEmpty(cloud)) {
            for (int i = 0; i < cloud.size(); i++) {
                Pos point = cloud.get(i);
                float f = a*point.getX() + b;
                E = (float) (E + Math.pow(f-point.getY(), 2));
            }
            E = E / cloud.size();
        }
        return E;
    }

    private Pos getIntersectionPoint(float a1, float b1, float a2, float b2) {
        if (a1!=a2 && b1!=b2) {
            float x = (b2-b1)/(a1-a2);
            float y = a1*x + b1;
            return new Pos(x,y);
        }
        return null;
    }

    private boolean checkOutOfArea(List<Pos> cloud, Pos intersectionPoint) {
        final Comparator<Pos> compX = (p1, p2) -> Float.compare( p1.getX(), p2.getX());
        final Comparator<Pos> compY = (p1, p2) -> Float.compare( p1.getY(), p2.getY());
        float maxBordersX = cloud.stream().max(compX).get().getX();
        float maxBordersY = cloud.stream().max(compY).get().getY();
        float minBordersX = cloud.stream().min(compX).get().getX();
        float minBordersY = cloud.stream().min(compY).get().getY();
        if (intersectionPoint.getX()<maxBordersX
                && intersectionPoint.getX()>minBordersX
                && intersectionPoint.getY()<maxBordersY
                && intersectionPoint.getY()>minBordersY) {
            return true;
        } else {
            return false;
        }
    }
}

//TODO сделать оси, чтобы понятен был масштаб