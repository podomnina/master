import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import service.TrackingService;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class Main extends Application {

    private TrackingService trackingService = new TrackingService();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {


        final Group group = new Group();

        final Label inputX = new Label("Input target x:");
        final Label inputY = new Label("Input target y:");
        final TextField valueX = new TextField();
        final TextField valueY = new TextField();
        final Button ok = new Button("ok");
        final VBox vBox = new VBox();

        final Rectangle rectangle = new Rectangle(600,300);
        rectangle.setFill(Color.WHITE);

        final Pane pane = new Pane();
        pane.setMinSize(600,300);
        pane.setMaxSize(600,300);

        pane.getChildren().add(rectangle);

        vBox.getChildren().addAll(inputX, valueX, inputY, valueY, ok, pane);
        group.getChildren().add(vBox);

        final Scene scene = new Scene(group, 600, 400);

        stage.setScene(scene);
        stage.show();

        ok.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                final String valX = valueX.getText();
                final String valY = valueX.getText();
                try {
                    trackingService.justDoIt(Float.parseFloat(valX), Float.parseFloat(valY), pane);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //startTask(valX, valY, pane);
            }
        });
    }

    public void startTask(final String valX, final String valY, final Pane pane) {
        Runnable task = new Runnable() {
            public void run() {
                runTask(valX, valY, pane);
            }
        };
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }

    public void runTask(final String valX, final String valY, final Pane pane) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        trackingService.justDoIt(Float.parseFloat(valX), Float.parseFloat(valY), pane);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

    }

}
