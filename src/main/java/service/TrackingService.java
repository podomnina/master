package service;


import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Pos;
import model.Vehicle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;


public class TrackingService {

    public Vehicle createVehicle(Long id, Pos initialPos, Color color, Pane pane) {
        final Vehicle vehicle = new Vehicle(id, initialPos, new Stack<Pos>(), new Circle(5, color));
        vehicle.getCircle().setCenterX(initialPos.getX());
        vehicle.getCircle().setCenterY(initialPos.getY());
        pane.getChildren().add(vehicle.getCircle());
        System.out.println("Created new vehicle with id: " + id);
        return vehicle;
    }

    public void justDoIt(float x, float y, Pane pane) throws IOException, InterruptedException {
        final Vehicle mainVehicle = new Vehicle(1L, new Pos(10,10), new Stack<Pos>(), new Circle(5, Color.RED));
        pane.getChildren().add(mainVehicle.getCircle());
        final Pos newTarget = new Pos(x,y);
        mainVehicle.getTargetList().push(newTarget);

        moveVehicle(mainVehicle,1, pane);
    }

    private void moveVehicle(Vehicle vehicle, int step, Pane pane) throws InterruptedException {
        final Pos to = vehicle.getTargetList().pop();
        while (true) {
            final Pos from = vehicle.getCurrentPos();
            final Pos unitVector = getUnitVector(from, to);
            final Pos currentPoint = vehicle.getCurrentPos();
            final Pos nextPoint = new Pos((currentPoint.getX() + unitVector.getX() * step),
                    (currentPoint.getY() + unitVector.getY() * step));
            vehicle.setCurrentPos(nextPoint);
            final javafx.scene.shape.Rectangle rectangle = new Rectangle(600,300);
            rectangle.setFill(Color.WHITE);
            Circle circle = new Circle(5, Color.RED);
            pane.getChildren().addAll(circle, rectangle);
            rectangle.toFront();
            circle.toFront();
            circle.setTranslateX(vehicle.getCurrentPos().getX());
            circle.setTranslateY(vehicle.getCurrentPos().getY());
            if (((nextPoint.getX() <= to.getX() + 0.5) && (nextPoint.getX() >= to.getX() - 0.5))
                    && ((nextPoint.getY() <= to.getY() + 0.5) && (nextPoint.getY() >= to.getY() - 0.5))) {
                vehicle.setCurrentPos(to);
                vehicle.getCircle().setCenterX(vehicle.getCurrentPos().getX());
                vehicle.getCircle().setCenterY(vehicle.getCurrentPos().getY());
                System.out.println("Vehicle has reached the goal");
                break;
            }
            Thread.sleep(1000);
        }
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
