package SmartRailSimulator;

import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.application.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.*;
import javafx.scene.control.Label;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.shape.Line;
import javafx.scene.Group;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * The SimulationGUI class is responsible for all the GUI related aspects of
 * the simulation. It gets the location of stations, switches, and track
 * segments and places them on the screen. The two-click simulation system
 * is controlled through here using event handlers on the stations. Paths are
 * locked and turned red when a path is reserved for a train. Train colors are
 * changed based on their status, illustrated in the assignment PDF.
 */

public class SimulationGUI extends Application {
    private static List<Station> stations;
    private static List<TrackSegment> trackSegments;
    private static List<Switch> switches;
    private Station startingStation = null;
    private Pane mainPane;
    private final Map<String, List<Rectangle>> trackVisuals = new HashMap<>();
    private static SimulationGUI instance;
    private Label errorMessageLabel;


    /**
     * Initializes and starts the JavaFX application.
     *
     * @param primaryStage The primary stage for this application, onto which
     *                     the application scene can be set.
     *
     **/
    @Override
    public void start(Stage primaryStage) {
        instance = this;
        mainPane = new Pane();

        // Create error message label
        errorMessageLabel = new Label();
        errorMessageLabel.setStyle("-fx-background-color: #ffebee; " +
                "-fx-padding: 10; " + "-fx-border-color: #ef5350; " +
                "-fx-border-width: 1;" + "-fx-background-radius: 5; " +
                "-fx-border-radius: 5;");
        errorMessageLabel.setTextFill(Color.web("#c62828"));
        errorMessageLabel.setFont(new Font(14));
        errorMessageLabel.setVisible(false);
        errorMessageLabel.setLayoutX(10);
        errorMessageLabel.setLayoutY(10);

        // Create main layout
        BorderPane mainBP = new BorderPane(mainPane);
        Scene scene = new Scene(mainBP, 1000, 800);

        // Draw stations, track segments, and switches on the main pane
        drawStations(mainPane);
        drawTrackSegments(mainPane);
        drawSwitches(mainPane);

        // Add error label to the main pane
        mainPane.getChildren().add(errorMessageLabel);

        // Set the title and scene for the primary stage and show it
        primaryStage.setTitle("Smart Rail Train Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Displays an error message in the GUI and fades it out after a delay.
     *
     * @param message The error message to be displayed.
     */
    private void showErrorMessage(String message) {
        Platform.runLater(() -> {
            errorMessageLabel.setText(message);
            errorMessageLabel.setVisible(true);

            // Create fade out transition
            FadeTransition fadeOut = new
                      FadeTransition(Duration.seconds(3), errorMessageLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            // Show message for 2 seconds before fading
            fadeOut.setDelay(Duration.seconds(2));
            fadeOut.setOnFinished(event -> errorMessageLabel.setVisible(false));

            // Reset opacity and start animation
            errorMessageLabel.setOpacity(1.0);
            fadeOut.play();
        });
    }

    /**
     * Draws the stations on the provided pane.
     *
     * @param p The pane on which the stations will be drawn.
     */
    private void drawStations(Pane p) {
        int offset = 50;

        for (Station station : stations) {
            // Coordinates given from file
            double x = station.getStationLocation().getX();
            double y = station.getStationLocation().getY();

            StackPane stationPane = new StackPane();
            stationPane.setLayoutX(x * 100 + offset);
            stationPane.setLayoutY(y * 100 + offset);

            Rectangle stationBox = new Rectangle(100, 100);
            stationBox.setFill(Color.SLATEBLUE);
            stationBox.setStroke(Color.BLACK);

            Label stationLabel = new Label("S");
            stationLabel.setStyle
                            ("-fx-border-color: black; -fx-alignment: center;");
            stationLabel.setFont(new Font(50));
            stationLabel.setAlignment(Pos.CENTER);

            stationPane.getChildren().addAll(stationBox, stationLabel);

            p.getChildren().add(stationPane);

            // Set click event handler for the station
            stationPane.setOnMouseClicked(event -> handleStationClick(station));
        }
    }

    /**
     * Handles the click event on a station. If no starting station is selected,
     * it sets the clicked station as the starting station. If starting station
     * is already selected, it sets the clicked station as the end station and
     * initiates the train journey.
     *
     * @param station The station that was clicked.
     */
    private void handleStationClick(Station station) {
        if (startingStation == null) {
            startingStation = station;
            System.out.println("Station selected: " + station);
            showErrorMessage("Selected starting station: " + station);
        } else {
            System.out.println("End station: " + station);
            if (startingStation == station) {
                showErrorMessage("Error: Cannot select same station " +
                                 "as start and end point");
                startingStation = null;
                return;
            }
            goTrain(startingStation, station);
            startingStation = null;
        }
    }

    /**
     * Initiates the train journey from the starting station to the end station.
     * Creates a visual representation of the train and starts the journey.
     *
     * @param start The starting station of the train journey.
     * @param end The end station of the train journey.
     */
    private void goTrain(Station start, Station end) {
        System.out.println("Starting train from " + start.getStationLocation() +
                " to " + end.getStationLocation());

        double startX = start.getStationLocation().getX() * 100 + 50;
        double startY = start.getStationLocation().getY() * 100 + 50;

        Rectangle trainBox = new Rectangle(20, 20, Color.YELLOW);
        trainBox.setLayoutX(startX);
        trainBox.setLayoutY(startY);
        mainPane.getChildren().add(trainBox);

        System.out.println("Created train visual at (" +
                            startX + "," + startY + ")");

        Train train = new Train
                           (start.getStationLocation(), end, "Right") {
            @Override
            protected void onNoPathFound() {
                // Override the method to show error in GUI
                showErrorMessage("Error: No valid path found " +
                                    "between selected stations");
                Platform.runLater(() -> mainPane.getChildren().remove(trainBox));
            }
        };

        train.setVisualComponent(trainBox);
        train.start();
        train.beginJourney();
    }

    /**
     * Draws the track segments on the provided pane.
     *
     * @param p The pane on which the track segments will be drawn.
     */
    public void drawTrackSegments(Pane p) {
        int offset = 50;

        for (TrackSegment segment : trackSegments) {
            Group trackGroup = new Group();

            // Calculate exact pixel coordinates including offset
            double startX = segment.getStartingLocation().getX() * 100 + offset;
            double startY = segment.getStartingLocation().getY() * 100 + offset;
            double endX = segment.getEndingLocation().getX() * 100 + offset;
            double endY = segment.getEndingLocation().getY() * 100 + offset;

            // Create main track lines (double line for rails)
            double railGap = 6; // Distance between the parallel rails

            // Calculate perpendicular offset for parallel lines
            double dx = endX - startX;
            double dy = endY - startY;
            double length = Math.sqrt(dx * dx + dy * dy);
            double normalizedDx = dx / length;
            double normalizedDy = dy / length;

            // Calculate perpendicular vector
            double perpX = -normalizedDy * railGap / 2;
            double perpY = normalizedDx * railGap / 2;

            // Draw the two parallel rail lines
            Line rail1 = new Line(
                    startX + perpX, startY + perpY,
                    endX + perpX, endY + perpY
            );
            Line rail2 = new Line(
                    startX - perpX, startY - perpY,
                    endX - perpX, endY - perpY
            );

            // Style the rails
            rail1.setStrokeWidth(3);
            rail2.setStrokeWidth(3);
            rail1.setStroke(Color.DARKGRAY);
            rail2.setStroke(Color.DARKGRAY);

            // Add railroad ties
            int numTies = (int) (length / 20); // One tie every 20 pixels
            for (int i = 0; i <= numTies; i++) {
                double t = i / (double) numTies;
                double tieX = startX + dx * t;
                double tieY = startY + dy * t;

                Line tie = new Line(
                        tieX + perpX * 1.5, tieY + perpY * 1.5,
                        tieX - perpX * 1.5, tieY - perpY * 1.5
                );
                tie.setStrokeWidth(3);
                tie.setStroke(Color.SADDLEBROWN);
                trackGroup.getChildren().add(tie);
            }

            // Add endpoint indicators
            double connectorSize = 8;
            Circle startConnector = new Circle(startX, startY, connectorSize);
            Circle endConnector = new Circle(endX, endY, connectorSize);

            startConnector.setFill(Color.DARKGRAY);
            startConnector.setStroke(Color.BLACK);
            startConnector.setStrokeWidth(1);
            endConnector.setFill(Color.DARKGRAY);
            endConnector.setStroke(Color.BLACK);
            endConnector.setStrokeWidth(1);

            // Add all elements to the track group
            trackGroup.getChildren()
                            .addAll(rail1, rail2, startConnector, endConnector);

            // Create unique key for this track segment
            String trackKey = String.format("%.1f,%.1f-%.1f,%.1f",
                    segment.getStartingLocation().getX(),
                    segment.getStartingLocation().getY(),
                    segment.getEndingLocation().getX(),
                    segment.getEndingLocation().getY());

            // Store references for state updates
            List<Rectangle> segmentVisuals = new ArrayList<>();
            // Dummy rectangle for compatibility
            segmentVisuals.add(new Rectangle());
            trackVisuals.put(trackKey, segmentVisuals);

            // Add state change handler, store segment reference for state updates
            trackGroup.setUserData(segment);

            p.getChildren().add(trackGroup);
        }
    }

    /**
     * Draws the switches on the provided pane.
     *
     * @param p The pane on which the switches will be drawn.
     */
    public void drawSwitches(Pane p) {
        int offset = 50;

        for (Switch trackSwitch : switches) {
            double switchX = trackSwitch
                                     .getSwitchLocation().getX() * 100 + offset;
            double switchY = trackSwitch
                                     .getSwitchLocation().getY() * 100 + offset;

            List<TrackSegment> connectedTracks =
                                             trackSwitch.getConnectedSegments();
            if (connectedTracks.isEmpty()) continue;

            Group switchGroup = new Group();

            // Find the incoming and outgoing tracks
            TrackSegment incomingTrack = null;
            List<TrackSegment> outgoingTracks = new ArrayList<>();

            for (TrackSegment track : connectedTracks) {
                if (track.getStartingLocation()
                                     .equals(trackSwitch.getSwitchLocation())) {
                    outgoingTracks.add(track);
                } else if (track.getEndingLocation()
                                     .equals(trackSwitch.getSwitchLocation())) {
                    incomingTrack = track;
                }
            }

            if (incomingTrack == null || outgoingTracks.isEmpty()) continue;

            // Draw the switch mechanism - just a simple circle now
            Circle switchBase = new Circle(switchX, switchY, 8);
            switchBase.setFill(Color.GOLDENROD);
            switchBase.setStroke(Color.BLACK);
            switchBase.setStrokeWidth(1);

            // Draw the branching tracks
            double branchLength = 30; // Length of the visible switch branches

            // Calculate and draw connected lines
            for (TrackSegment track : connectedTracks) {
                double endX, endY;
                if (track.getStartingLocation()
                                     .equals(trackSwitch.getSwitchLocation())) {
                    endX = track.getEndingLocation().getX() * 100 + offset;
                    endY = track.getEndingLocation().getY() * 100 + offset;
                } else {
                    endX = track.getStartingLocation().getX() * 100 + offset;
                    endY = track.getStartingLocation().getY() * 100 + offset;
                }

                double angle = Math.atan2(endY - switchY, endX - switchX);
                Line branch = new Line(
                        switchX,
                        switchY,
                        switchX + Math.cos(angle) * branchLength,
                        switchY + Math.sin(angle) * branchLength
                );
                branch.setStrokeWidth(3);
                branch.setStroke(Color.DARKGRAY);
                switchGroup.getChildren().add(branch);
            }

            switchGroup.getChildren().add(switchBase);
            p.getChildren().add(switchGroup);
        }
    }


    /**
     * Launches the GUI application with the provided lists of stations,
     * track segments, and switches.
     * Initializes the static lists and starts the JavaFX application.
     *
     * @param stationsList The list of stations to be used in the simulation.
     * @param trackSegmentList List of track segments used in the simulation.
     * @param switchesList The list of switches to be used in the simulation.
     */
    public static void launchGUI(List<Station> stationsList,
                                 List<TrackSegment> trackSegmentList,
                                 List<Switch> switchesList) {
        stations = stationsList;
        trackSegments = trackSegmentList;
        switches = switchesList;

        Application.launch();
    }

    // Getters
    public Pane getMainPane() {
        return mainPane;
    }
    public static SimulationGUI getInstance() {
        return instance;
    }
}
