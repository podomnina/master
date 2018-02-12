import javafx.application.Application;
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
import javafx.stage.Stage;
import service.TrackingService;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class Main extends Application {

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) throws Exception {


        final Group group = new Group();

        final Label inputX = new Label("Input target x:");
        final Label inputY = new Label("Input target y:");
        final TextField valueX = new TextField();
        final TextField valueY = new TextField();
        final Button ok = new Button("ok");
        final VBox vBox = new VBox();

        final Pane pane = new Pane();
        pane.setMinSize(600,300);
        pane.setMaxSize(600,300);

        vBox.getChildren().addAll(inputX, valueX, inputY, valueY, ok, pane);
        group.getChildren().add(vBox);

        final Scene scene = new Scene(group, 600, 400);

        stage.setScene(scene);
        stage.show();

        final TrackingService trackingService = new TrackingService();

        ok.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                final String valX = valueX.getText();
                final String valY = valueX.getText();

                if (!isEmpty(valX) && !isEmpty(valY)) {
                    Task <Void> task = new Task<Void>() {
                        @Override public Void call()
                                trackingService.justDoIt(Float.parseFloat(valX), Float.parseFloat(valY), pane);

                }
            }
        });
    }
}
