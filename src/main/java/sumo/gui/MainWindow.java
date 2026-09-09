package sumo.gui;

import java.net.URL;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import sumo.Sumo;

/** Controls Sumo's main chat window. */
public class MainWindow extends AnchorPane {
    private static final String USER_IMAGE_PATH = "/images/DaUser.png";
    private static final String SUMO_IMAGE_PATH = "/images/DaSumo.png";

    private final Image userImage = loadImage(USER_IMAGE_PATH);
    private final Image sumoImage = loadImage(SUMO_IMAGE_PATH);

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer;
    @FXML
    private TextField userInput;
    @FXML
    private Button sendButton;

    private Sumo sumo;

    /** Keeps the newest dialog visible when the conversation grows. */
    @FXML
    public void initialize() {
        dialogContainer.heightProperty().addListener(observable -> scrollPane.setVvalue(1.0));
    }

    /**
     * Supplies the Sumo instance that handles commands and displays its greeting.
     *
     * @param sumo Sumo application to use for this window
     */
    public void setSumo(Sumo sumo) {
        this.sumo = sumo;
        dialogContainer.getChildren().add(
                DialogBox.getSumoDialog("Hello! I'm Sumo.\nWhat can I do for you?", sumoImage));
    }

    /** Displays the user's command followed by Sumo's response. */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText().trim();
        if (input.isEmpty() || sumo == null || sumo.isExit()) {
            return;
        }

        String response = sumo.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getSumoDialog(response, sumoImage));
        userInput.clear();

        if (sumo.isExit()) {
            userInput.setDisable(true);
            sendButton.setDisable(true);
        }
    }

    /** Loads an optional avatar, returning {@code null} while the image is absent. */
    private static Image loadImage(String resourcePath) {
        URL imageUrl = MainWindow.class.getResource(resourcePath);
        return imageUrl == null ? null : new Image(imageUrl.toExternalForm());
    }
}
