package service;


import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

    private final Logger logger = LoggerFactory.getLogger(TrackingService.class);

    public void justDoIt(float x, float y, Pane pane) throws IOException, InterruptedException {
        final Vehicle mainVehicle = new Vehicle(1L, new Pos(10,10), new Stack<Pos>(), new Circle(5, Color.RED));
        pane.getChildren().add(mainVehicle.getCircle());
        logger.debug("Created vehicle number '{}' with position '{}'", mainVehicle.getId(), mainVehicle.getCurrentPos());
        final Pos newTarget = new Pos(x,y);
        mainVehicle.getTargetList().push(newTarget);

        moveVehicle(mainVehicle,1);
    }

    private void moveVehicle(Vehicle vehicle, int step) throws InterruptedException {
        final Pos to = vehicle.getTargetList().pop();
        while (true) {
            final Pos from = vehicle.getCurrentPos();
            final Pos unitVector = getUnitVector(from, to);
            final Pos currentPoint = vehicle.getCurrentPos();
            final Pos nextPoint = new Pos((currentPoint.getX() + unitVector.getX() * step),
                    (currentPoint.getY() + unitVector.getY() * step));
            vehicle.setCurrentPos(nextPoint);

            vehicle.getCircle().setTranslateX(vehicle.getCurrentPos().getX());
            vehicle.getCircle().setTranslateY(vehicle.getCurrentPos().getY());
            createAndPlayTransition(vehicle.getCircle(), from, nextPoint);
            if (((nextPoint.getX() <= to.getX() + 0.5) && (nextPoint.getX() >= to.getX() - 0.5))
                    && ((nextPoint.getY() <= to.getY() + 0.5) && (nextPoint.getY() >= to.getY() - 0.5))) {
                vehicle.setCurrentPos(to);
                vehicle.getCircle().setCenterX(vehicle.getCurrentPos().getX());
                vehicle.getCircle().setCenterY(vehicle.getCurrentPos().getY());
                //TODO ADD CREATEANDPLATTRANSITION
                System.out.println("Vehicle has reached the goal");
                break;
            }
            Thread.sleep(10);
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

    private void createAndPlayTransition(Circle circle, Pos from, Pos to) {
        TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(1),circle);
        translateTransition.setFromX(from.getX());
        translateTransition.setFromY(from.getY());
        translateTransition.setToX(to.getX());
        translateTransition.setToY(to.getY());
        translateTransition.play();
    }
}
