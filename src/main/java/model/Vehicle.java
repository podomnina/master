package model;

import javafx.scene.shape.Circle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
public class Vehicle {
    private Long id;
    private Pos currentPos;
    private Queue<Pos> targetList;
    private Circle circle;
    private Queue<Pos> approximateTargetList;
    private Queue<Pos> etalonTargetList = new LinkedList<>();

    private List<Pos> approximateList = new ArrayList<>();
    private List<Pos> list = new ArrayList<>();


    public static final float GPS_MEASUREMENT_ERROR = 50;

    public Vehicle(Long id, Pos currentPos, Queue<Pos> targetList, Circle circle) {
        this.id = id;
        this.currentPos = currentPos;
        this.targetList = targetList != null ? targetList : new LinkedList<>();
        this.circle = circle;
        if (this.circle != null && this.currentPos != null) {
            this.circle.setCenterX(this.currentPos.getX());
            this.circle.setCenterY(this.currentPos.getY());
        }
        this.approximateTargetList = new LinkedList<>();
    }

    public void redrawCircle() {
        circle.setCenterX(currentPos.getX());
        circle.setCenterY(currentPos.getY());
    }

    public Pos getCurrentPosWithMeasurementError() {
        if (currentPos != null) {
            float resX = (float) (Math.random()*2*GPS_MEASUREMENT_ERROR - GPS_MEASUREMENT_ERROR);
            float resY = (float) (Math.random()*2*GPS_MEASUREMENT_ERROR - GPS_MEASUREMENT_ERROR);
            return new Pos(currentPos.getX() + resX, currentPos.getY() + resY);
        }
        return currentPos;
    }
}