package model;

import javafx.scene.shape.Circle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Stack;

@Data
public class Vehicle {
    private Long id;
    private Pos currentPos;
    private Stack<Pos> targetList = new Stack<Pos>();
    private Circle circle;

    public Vehicle(Long id, Pos currentPos, Stack<Pos> targetList, Circle circle) {
        this.id = id;
        this.currentPos = currentPos;
        this.targetList = targetList != null ? targetList : new Stack<Pos>();
        this.circle = circle;
        if (this.circle != null && this.currentPos != null) {
            this.circle.setCenterX(this.currentPos.getX());
            this.circle.setCenterX(this.currentPos.getX());
        }
    }
}