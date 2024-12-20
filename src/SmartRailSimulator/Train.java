package SmartRailSimulator;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a train in the railway simulation system.
 * Manages train movement, path finding, and track locking to ensure safe
 * transit between stations. Implements visual feedback of train state and
 * animated movement.
 * -
 * The train operates as an independent entity that communicates with track
 * segments and switches via a message-based system. It can find paths to
 * destinations and navigate through the railway network while avoiding
 * collisions.
 *
 * @author Oscar McCoy and Adam Fasulo
 * @version 1.0
 */
public class Train implements Runnable {
    public enum TrainState {
        IDLE, SEEKING_PATH, LOCKING_PATH, WAITING_FOR_PATH, MOVING
    }

    // Core properties
    private final BlockingQueue<Message> messageQueue =
                                                    new LinkedBlockingQueue<>();
    private Location currentLocation;
    private Station desiredDestination;
    private Station finalDestination;
    private String direction;
    private Rectangle visualComponent;

    // State management
    private TrainState currentState = TrainState.IDLE;
    private boolean running = true;
    private Thread thread;
    private boolean isIntermediateJourney = false;

    // Path management
    private ArrayList<Object> plannedRoute;
    private ArrayList<Location> intermediatePoints;
    private final List<Object> lockedComponents = new ArrayList<>();
    private int currentRouteIndex;
    private int currentPointIndex;

    /**
     * Constructs a new train with specified starting parameters.
     *
     * @param startingLocation Initial location of the train
     * @param desiredDestination Target station for the journey
     * @param direction Initial direction of travel
     */
    public Train(Location startingLocation,
                 Station desiredDestination, String direction) {
        this.currentLocation = startingLocation;
        this.desiredDestination = desiredDestination;
        this.direction = direction;
    }

    /**
     * Sends a message to this train's message queue for processing.
     * Messages are used for communication between the train & track components.
     *
     * @param message The message to be processed by the train
     */
    public void sendMessage(Message message) {
        if (message != null) {
            messageQueue.add(message);
        }
    }
    /**
     * Main execution loop for the train thread. Continuously processes incoming
     * messages from the message queue until stopped.
     */
    @Override
    public void run() {
        while (running) {
            try {
                processMessage(messageQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Train interrupted");
                break;
            }
        }
    }

    /**
     * Starts the train's execution thread if it hasn't been started already.
     */
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Stops the train's execution thread and interrupts any ongoing operations.
     */
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Processes incoming messages based on their type and initiates appropriate
     * actions.
     *
     * @param msg The message to process
     */
    private void processMessage(Message msg) {
        if (msg == null || msg.getType() == null) return;
        switch (msg.getType()) {
            case ROUTE_RESPONSE -> handleRouteResponse(msg);
            case LOCK_RESPONSE -> handleLockResponse(msg);
            case MOVE_RESPONSE -> handleMoveResponse(msg);
        }
    }

    /**
     * Handles route response messages containing path information.
     * Validates the response and initiates path following if valid.
     *
     * @param msg The route response message
     */
    @SuppressWarnings("unchecked")
    private void handleRouteResponse(Message msg) {
        System.out.println("Received route response");

        if (!(msg.getAdditionalData() instanceof ArrayList)) {
            System.out.println("Invalid route response data");
            currentState = TrainState.IDLE;
            updateTrainVisuals();
            return;
        }

        plannedRoute = (ArrayList<Object>) msg.getAdditionalData();
        if (plannedRoute.isEmpty()) {
            System.out.println("Received empty path");
            currentState = TrainState.IDLE;
            updateTrainVisuals();
            onNoPathFound();
            return;
        }

        initializePath();
        lockEntirePath();
    }

    /**
     * Initializes the path following sequence after receiving a valid route.
     * Sets up intermediate points and prepares for path locking.
     */
    private void initializePath() {
        System.out.println("Found valid route with " + plannedRoute.size() +
                                                                " components:");
        plannedRoute.forEach(component ->
                                        System.out.println("  - " + component));

        updateDirectionFromPath();
        intermediatePoints = generateIntermediatePoints();
        currentPointIndex = 0;
        currentRouteIndex = 0;

        System.out.println("Generated path points: " + intermediatePoints);
        currentState = TrainState.LOCKING_PATH;
        updateTrainVisuals();
    }

    /**
     * Updates the train's direction based on the first two components of the
     * planned route.
     */
    private void updateDirectionFromPath() {
        if (plannedRoute.size() >= 2) {
            Object first = plannedRoute.get(0);
            Object second = plannedRoute.get(1);
            if (first instanceof TrackSegment &&
                    second instanceof TrackSegment) {
                TrackSegment firstTrack = (TrackSegment) first;
                TrackSegment secondTrack = (TrackSegment) second;
                direction = determineDirection(firstTrack, secondTrack);
                System.out.println("Determined direction: " + direction);
            }
        }
    }

    /**
     * Handles responses to lock requests. Updates train state and initiates
     * movement if lock was successful.
     *
     * @param msg The lock response message
     */
    private void handleLockResponse(Message msg) {
        if ((boolean) msg.getAdditionalData()) {
            System.out.println("Lock acquired successfully");
            currentState = TrainState.MOVING;
            updateTrainVisuals();
            requestNextMovement();
        } else {
            System.out.println("Failed to acquire lock");
            currentState = TrainState.IDLE;
            updateTrainVisuals();
        }
    }

    /**
     * Handles responses to movement requests. Initiates movement if approved.
     *
     * @param msg The move response message
     */
    private void handleMoveResponse(Message msg) {
        if (!(boolean) msg.getAdditionalData()) {
            System.out.println("Move request denied");
            currentState = TrainState.IDLE;
            updateTrainVisuals();
            return;
        }

        currentState = TrainState.MOVING;
        updateTrainVisuals();
        handleMovement();
    }

    /**
     * Manages the train's movement between points on its path.
     * Initiates animation and tracks progress.
     */
    private void handleMovement() {
        if (currentPointIndex >= intermediatePoints.size() - 1) {
            return;
        }

        Location nextLocation = intermediatePoints.get(currentPointIndex + 1);
        System.out.println("Moving from " + currentLocation + " to " +
                                                                  nextLocation);

        animateMovement(currentLocation, nextLocation, () -> {
            try {
                processMovementCompletion(nextLocation);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Movement interrupted");
            }
        });
    }

    /**
     * Processes completion of a movement step, updating position and
     * determining next actions.
     *
     * @param nextLocation The location reached after movement
     * @throws InterruptedException If thread is interrupted during processing
     */
    private void processMovementCompletion(Location nextLocation) throws
                                                          InterruptedException {
        Object currentComponent = plannedRoute.get(currentRouteIndex);
        currentLocation = nextLocation;
        currentPointIndex++;

        if (currentLocation.equals(desiredDestination.getStationLocation())) {
            handleDestinationArrival();
            return;
        }

        boolean needNextComponent = checkNeedNextComponent(currentComponent);
        if (needNextComponent) {
            currentRouteIndex++;
            if (currentRouteIndex < plannedRoute.size()) {
                requestNextMovement();
            }
        } else {
            requestNextMovement();
        }
    }

    /**
     * Handles arrival at a destination station, managing both intermediate
     * and final destinations.
     */
    private void handleDestinationArrival() {
        if (isIntermediateJourney) {
            System.out.println("Reached intermediate station, continuing to " +
                                                           "final destination");
            isIntermediateJourney = false;
            desiredDestination = finalDestination;
            releaseAllLocks();
            beginJourney();
        } else {
            System.out.println("Reached final destination");
            releaseAllLocks();
            currentState = TrainState.IDLE;
            updateTrainVisuals();
        }
    }

    /**
     * Generates a list of intermediate points along the planned route.
     *
     * @return ArrayList of locations representing points along the route
     */
    private ArrayList<Location> generateIntermediatePoints() {
        ArrayList<Location> points = new ArrayList<>();
        points.add(currentLocation);

        for (Object component : plannedRoute) {
            if (component instanceof TrackSegment track) {
                points.add(direction.equals("Right") ?
                        track.getEndingLocation() :
                        track.getStartingLocation());
            } else if (component instanceof Switch sw) {
                points.add(sw.getSwitchLocation());
            }
        }

        Location destLoc = desiredDestination.getStationLocation();
        if (points.isEmpty() || !points.get(points.size() - 1)
                                                             .equals(destLoc)) {
            points.add(destLoc);
        }

        System.out.println("Generated path points: " + points);
        return points;
    }

    /**
     * Attempts to lock all components along the planned route.
     * Implements backoff and retry logic for failed lock attempts.
     */
    private void lockEntirePath() {
        System.out.println("Attempting to lock entire path...");
        List<Object> tempLocks = new ArrayList<>();

        for (Object component : plannedRoute) {
            if (!attemptLockComponent(component, tempLocks)) {
                // If we fail to get a lock, release temp locks & retry
                for (Object locked : tempLocks) {
                    releaseComponent(locked);
                    if (locked instanceof TrackSegment track) {
                        // Reset visual state when releasing
                        track.updateVisualState();
                    }
                }
                tempLocks.clear();

                // Random backoff prevents trains from retrying simultaneously
                try {
                    Thread.sleep((long)(Math.random() * 1000) + 500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Start over from beginning of path
                lockEntirePath();
                return;
            }
            tempLocks.add(component);
            // Update visual state when locking
            if (component instanceof TrackSegment track) {
                track.updateVisualState();
            }
        }

        // Successfully locked entire path
        lockedComponents.addAll(tempLocks);
        System.out.println("All path components locked, starting movement");
        currentState = TrainState.MOVING;
        updateTrainVisuals();
        requestNextMovement();
    }

    /**
     * Attempts to lock a single component along the route.
     *
     * @param component The component to lock
     * @param tempLocks List of temporarily locked components
     * @return boolean indicating if lock was successful
     */
    private boolean attemptLockComponent(Object component,
                                         List<Object> tempLocks) {
        Message lockRequest = createLockRequest(component);
        int maxRetries = 3; // Limit retries before giving up and backing off
        int retryCount = 0;

        try {
            while (retryCount < maxRetries) {
                sendLockRequest(component, lockRequest);
                Message response = messageQueue.take();

                if (response.getType() == Message.MessageType.LOCK_RESPONSE) {
                    if ((Boolean)response.getAdditionalData()) {
                        return true;
                    } else {
                        currentState = TrainState.WAITING_FOR_PATH;
                        updateTrainVisuals();
                        System.out.println("Path blocked, retry " +
                                        (retryCount + 1) + " of " + maxRetries);
                        retryCount++;
                        Thread.sleep(500);
                    }
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Creates a lock request message for a component.
     *
     * @param component The component to create a lock request for
     * @return Message containing the lock request
     */
    private Message createLockRequest(Object component) {
        return new Message(
                "Lock request",
                component.toString(),
                Message.MessageType.LOCK_REQUEST,
                this,
                true
        );
    }

    /**
     * Sends a lock request to a component.
     *
     * @param component The component to send the request to
     * @param request The lock request message
     */
    private void sendLockRequest(Object component, Message request) {
        if (component instanceof TrackSegment track) {
            track.messageQueue.add(request);
        } else if (component instanceof Switch sw) {
            sw.messageQueue.add(request);
        }
    }

    /**
     * Releases all locked components along the route.
     */
    private void releaseAllLocks() {
        for (Object component : lockedComponents) {
            if (component instanceof TrackSegment track) {
                track.trainLeaving();
            } else if (component instanceof Switch sw) {
                sw.trainLeaving();
            }
        }
        lockedComponents.clear();
    }

    /**
     * Releases a single component's lock.
     *
     * @param component The component to release
     */
    private void releaseComponent(Object component) {
        if (component instanceof TrackSegment track) {
            track.trainLeaving();
        } else if (component instanceof Switch sw) {
            sw.trainLeaving();
        }
    }

    // Journey management methods

    /**
     * Initiates the train's journey to its destination.
     * Sets up initial direction and finds starting track.
     */
    public void beginJourney() {
        System.out.println("Starting journey to " +
                                       desiredDestination.getStationLocation());
        finalDestination = desiredDestination;
        currentState = TrainState.SEEKING_PATH;
        updateTrainVisuals();

        // Set initial direction based on destination
        direction = (desiredDestination.getStationLocation().getX() >
                                                       currentLocation.getX()) ?
                "Right" : "Left";

        TrackSegment startTrack = findValidStartingTrack();
        if (startTrack != null) {
            Message routeRequest = new Message(
                    "Request route",
                    startTrack.toString(),
                    Message.MessageType.ROUTE_REQUEST,
                    this,
                    new ArrayList<>()
            );
            startTrack.messageQueue.add(routeRequest);
        } else {
            currentState = TrainState.IDLE;
            updateTrainVisuals();
            onNoPathFound();
        }
    }

    // Helper methods

    /**
     * Determines the direction of travel based on track connections.
     *
     * @param current The current track segment
     * @param next The next track segment
     * @return String indicating direction ("Right" or "Left")
     */
    private String determineDirection(TrackSegment current, TrackSegment next) {
        if (current.getEndingLocation().equals(next.getStartingLocation())) {
            return "Right";
        } else if (current.getStartingLocation()
                                            .equals(next.getEndingLocation())) {
            return "Left";
        }
        return current.getStartingLocation().getX() <
                                             next.getStartingLocation().getX() ?
                "Right" : "Left";
    }

    /**
     * Checks if the train needs to move to the next component in its route.
     *
     * @param currentComponent The current component being traversed
     * @return boolean indicating if next component is needed
     */

    private boolean checkNeedNextComponent(Object currentComponent) {
        if (currentComponent instanceof TrackSegment track) {
            return (direction.equals("Right") &&
                    currentLocation.equals(track.getEndingLocation())) ||
                    (direction.equals("Left") & currentLocation.equals
                                                 (track.getStartingLocation()));
        }
        return currentComponent instanceof Switch;
    }

    /**
     * Finds valid track segment to start the journey from the current location.
     *
     * @return TrackSegment that can be used to start the journey, or null if
     * none found.
     */
    private TrackSegment findValidStartingTrack() {
        System.out.println("\nDebug: Finding starting track");
        System.out.println("Current location: " + currentLocation);
        System.out.println("Desired destination: " +
                                       desiredDestination.getStationLocation());
        System.out.println("Looking for tracks in direction: " + direction);

        for (Station station : SimulationInitializer.getInstance()
                                                               .getStations()) {
            Location stationLoc = station.getStationLocation();
            if (stationLoc.getX() == currentLocation.getX() &&
                    stationLoc.getY() == currentLocation.getY()) {

                TrackSegment track = station.getConnectedTrack();
                if (track != null) {
                    System.out.println("Found connected track: " + track);
                    System.out.println("Track starts at: " +
                                                   track.getStartingLocation());
                    System.out.println("Track ends at: " +
                                                     track.getEndingLocation());

                    // If we want to go right
                    if (direction.equals("Right")) {
                        // Track goes right from our location
                        if (track.getStartingLocation()
                                                     .equals(currentLocation)) {
                            System.out.println("Using track starting at our " +
                                                        "location going right");
                            return track;
                        }
                        // Track comes in from the right, we can use it in reverse
                        else if (track.getEndingLocation()
                                                     .equals(currentLocation)) {
                            System.out.println("Using incoming track to go " +
                                                                       "right");
                            return track;
                        }
                    }
                    // If we want to go left
                    else if (direction.equals("Left")) {
                        // Track goes left from our location
                        if (track.getEndingLocation().equals(currentLocation)) {
                            System.out.println("Using track ending at our " +
                                                         "location going left");
                            return track;
                        }
                        // Track comes in from the left, we can use it in reverse
                        else if (track.getStartingLocation()
                                                     .equals(currentLocation)) {
                            System.out.println("Using incoming track to go " +
                                                                        "left");
                            return track;
                        }
                    }
                }
            }
        }
        System.out.println("No valid starting track found");
        return null;
    }

    // Visual management methods
    /**
     * Sets the visual representation of the train and updates its appearance.
     *
     * @param visualComponent The Rectangle object representing the train
     */
    public void setVisualComponent(Rectangle visualComponent) {
        this.visualComponent = visualComponent;
        updateTrainVisuals();
    }

    /**
     * Updates the train's visual appearance based on its current state.
     */
    private void updateTrainVisuals() {
        if (visualComponent == null) return;

        Platform.runLater(() -> {
            Color color = switch (currentState) {
                case IDLE -> Color.GREY;
                case SEEKING_PATH -> Color.YELLOW;
                case LOCKING_PATH, WAITING_FOR_PATH -> Color.RED; // Orange
                case MOVING -> Color.GREEN;
            };
            visualComponent.setFill(color);
        });
    }

    /**
     * Animates the train's movement between two locations.
     *
     * @param start Starting location
     * @param end Ending location
     * @param onComplete Callback to execute when animation completes
     */
    // In Train.java - modify the animateMovement method
    private void animateMovement(Location start,
                                 Location end, Runnable onComplete) {
        if (visualComponent == null) {
            System.out.println("No visual component to animate!");
            onComplete.run();
            return;
        }

        Platform.runLater(() -> {
            try {
                // Calculate screen coordinates
                double startX = start.getX() * 100 + 50;
                double startY = start.getY() * 100 + 50;
                double endX = end.getX() * 100 + 50;
                double endY = end.getY() * 100 + 50;

                // Calculate the distance
                double distance = Math.sqrt(
                        Math.pow(endX - startX, 2) +
                                Math.pow(endY - startY, 2)
                );

                // Set a constant speed (pixels per second)
                double speed = 269; // Adjust this value to change train speed

                // Calculate duration based on distance and speed
                double duration = distance / speed;

                // Create and configure animation
                Timeline timeline = new Timeline();
                KeyValue kvX = new KeyValue
                                      (visualComponent.layoutXProperty(), endX);
                KeyValue kvY = new KeyValue
                                      (visualComponent.layoutYProperty(), endY);
                KeyFrame kf = new KeyFrame
                                         (Duration.seconds(duration), kvX, kvY);

                timeline.getKeyFrames().add(kf);
                timeline.setOnFinished(event -> onComplete.run());
                timeline.play();
            } catch (Exception e) {
                System.out.println("Animation error: " + e.getMessage());
                e.printStackTrace();
                onComplete.run();
            }
        });
    }
    /**
     * Requests permission to move to the next component in the route.
     */
    private void requestNextMovement() {
        if (currentRouteIndex >= plannedRoute.size()) {
            return;
        }
        Object currentComponent = plannedRoute.get(currentRouteIndex);
        Message moveRequest = new Message(
                "Move request",
                currentComponent.toString(),
                Message.MessageType.MOVE_REQUEST,
                this,
                true
        );

        sendMoveRequest(currentComponent, moveRequest);
    }

    /**
     * Sends a movement request to a component.
     *
     * @param component The component to send the request to
     * @param moveRequest The movement request message
     */
    private void sendMoveRequest(Object component, Message moveRequest) {
        if (component instanceof TrackSegment track) {
            track.messageQueue.add(moveRequest);
        } else if (component instanceof Switch sw) {
            sw.messageQueue.add(moveRequest);
        }
    }
    protected void onNoPathFound() {
        // Default implementation - can be overridden
        System.out.println("No valid path found to destination");
    }

    // Getters and setters
    public Station getDesiredDestination() {return desiredDestination;}
    public void setDesiredDestination(Station destination) {
        this.desiredDestination = destination;
    }
    public String getDirection() {return direction;}
    public TrainState getCurrentState() {return currentState;}
}