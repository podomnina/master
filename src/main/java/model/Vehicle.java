package model;

import javafx.scene.shape.Circle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

@Data
public class Vehicle {
    private Long id;
    private Pos currentPos;
    private Queue<Pos> targetList;
    private Circle circle;
    private Queue<Pos> approximateTargetList;

    public Vehicle(Long id, Pos currentPos, Queue<Pos> targetList, Circle circle) {
        this.id = id;
        this.currentPos = currentPos;
        this.targetList = targetList != null ? targetList : new LinkedList<>();
        this.circle = circle;
        if (this.circle != null && this.currentPos != null) {
            this.circle.setCenterX(this.currentPos.getX());
            this.circle.setCenterX(this.currentPos.getX());
        }
        this.approximateTargetList = new LinkedList<>();
    }

    public void redrawCircle() {
        circle.setCenterX(currentPos.getX());
        circle.setCenterY(currentPos.getY());
    }

    public Pos getCurrentPosWithMeasurementError() {
        if (currentPos != null) {
            float res = (float) (Math.random()*40 - 20);
            return new Pos(currentPos.getX() + res, currentPos.getY() + res);
        }
        return currentPos;
    }
}