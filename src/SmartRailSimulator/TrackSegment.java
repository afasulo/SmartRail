package SmartRailSimulator;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.Group;
import javafx.scene.shape.Circle;


/**
 * Represents a segment of railroad track in the train simulation.
 * A track segment is a basic unit of the rail system that connects different
 * points and can be traversed by trains. It maintains its own state and
 * processes messages for routing, locking, and movement requests.
 */

public class TrackSegment implements Runnable {
    private final Location startingLocation;
    private final Location endingLocation;
    private final int segmentCount;
    // Made public for Train class access
    public final BlockingQueue<Message> messageQueue =
                                                    new LinkedBlockingQueue<>();

    private volatile boolean running = true;
    private volatile boolean secured = false;
    private volatile Train currentTrain = null;

    private Thread thread;
    private TrackSegment leftNeighbor;
    private TrackSegment rightNeighbor;

    /**
     * Constructs a new track segment with specified start and end locations.
     *
     * @param startingLocation The starting point of the track segment
     * @param endingLocation The ending point of the track segment
     * @param segmentCount The number of visual segments this track should be
     *                     divided into
     */
    public TrackSegment(Location startingLocation,
                        Location endingLocation, int segmentCount) {
        this.startingLocation = startingLocation;
        this.endingLocation = endingLocation;
        this.segmentCount = segmentCount;
    }

    /**
     * Starts the track segment's message processing thread if it hasn't been
     * started already.
     * The method initializes the segment's ability to handle incoming messages.
     */
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Stops the track segment's message processing thread and marks it for
     * termination.
     */
    public void stop() {
        running = false;
        thread.interrupt();
    }

    /**
     * Main processing loop for the track segment.
     * Continuously processes incoming messages until stopped or interrupted.
     */
    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                processMessage(messageQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Processes incoming messages based on their type.
     * Handles route requests, lock requests, and move requests.
     *
     * @param msg The message to process
     */
    private void processMessage(Message msg) {
        if (msg == null || msg.getType() == null) return;

        switch (msg.getType()) {
            case ROUTE_REQUEST -> handleRouteRequest((Train) msg.getSender());
            case LOCK_REQUEST -> handleLockRequest((Train) msg.getSender());
            case MOVE_REQUEST -> handleMoveRequest((Train) msg.getSender());
        }
    }

    /**
     * Handles a route request from a train.
     * Attempts to find a valid path to the train's destination and sends back
     * a response.
     *
     * @param requestingTrain The train requesting a route
     */
    private void handleRouteRequest(Train requestingTrain) {
        Location destinationLoc =
                   requestingTrain.getDesiredDestination().getStationLocation();
        List<Object> path = findPathTo(destinationLoc,
                               requestingTrain.getDirection(), new HashSet<>());

        requestingTrain.sendMessage(new Message(
                path != null ? "Route found" : "No route found",
                requestingTrain.toString(),
                Message.MessageType.ROUTE_RESPONSE,
                this,
                path != null ? path : new ArrayList<>()
        ));
    }

    /**
     * Handles a lock request from a train.
     * Attempts to secure the track segment for exclusive use by the requesting
     * train.
     *
     * @param requestingTrain The train requesting the lock
     */
    private void handleLockRequest(Train requestingTrain) {
        boolean success = !secured && currentTrain == null;
        if (success) {
            secured = true;
            currentTrain = requestingTrain;
        }

        sendResponse(requestingTrain,
                     Message.MessageType.LOCK_RESPONSE, success);
        updateVisualState();
    }

    /**
     * Handles a move request from a train.
     * Verifies if the train can move onto this track segment.
     *
     * @param requestingTrain The train requesting to move
     */
    private void handleMoveRequest(Train requestingTrain) {
        boolean success = currentTrain == null ||
                          currentTrain == requestingTrain;
        if (success) {
            currentTrain = requestingTrain;
        }

        sendResponse(requestingTrain,
                     Message.MessageType.MOVE_RESPONSE, success);
    }

    /**
     * Sends a response message back to a train.
     *
     * @param train The train to send the response to
     * @param type The type of response message
     * @param success Whether the request was successful
     */
    private void sendResponse(Train train,
                              Message.MessageType type, boolean success) {
        train.sendMessage(new Message(
                success ? "Request approved" : "Request denied",
                train.toString(),
                type,
                this,
                success
        ));
    }

    /**
     * Attempts to find path from this track segment to a specified destination.
     * Uses depth-first search to explore possible routes.
     *
     * @param destination The target location
     * @param direction The direction of travel ("Right" or "Left")
     * @param visited Set of already visited components to prevent cycles
     * @return List of track components forming a path to the destination, or
     *         null if no path exists
     */
    public List<Object> findPathTo(Location destination,
                                   String direction, Set<Object> visited) {
        if (visited.contains(this)) {
            System.out.println("Already visited this track, backtracking");
            return null;
        }
        visited.add(this);

        // First check if this track connects to destination
        boolean reachedDestination = false;
        if (direction.equals("Right")) {
            // When going right, we can reach destination if:
            // 1. Our end point is the destination (normal case)
            // 2. Our start point is the destination (reverse track case)
            if (endingLocation.equals(destination) ||
                                         startingLocation.equals(destination)) {
                System.out.println("Found destination at " + destination);
                reachedDestination = true;
            }
        } else { // Going Left
            // When going left, we can reach destination if:
            // 1. Our start point is the destination (normal case)
            // 2. Our end point is the destination (reverse track case)
            if (startingLocation.equals(destination) ||
                                           endingLocation.equals(destination)) {
                System.out.println("Found destination at " + destination);
                reachedDestination = true;
            }
        }

        if (reachedDestination) {
            List<Object> path = new ArrayList<>();
            path.add(this);
            return path;
        }

        // If not at destination, explore neighbors
        List<Object> neighbors = findNeighbors(direction);
        System.out.println("Exploring neighbors from " + this + ": " +
                                                                     neighbors);

        for (Object neighbor : neighbors) {
            if (neighbor instanceof TrackSegment) {
                TrackSegment track = (TrackSegment) neighbor;
                List<Object> path = track.findPathTo(destination,
                                             direction, new HashSet<>(visited));
                if (path != null) {
                    path.add(0, this);
                    return path;
                }
            }
        }

        System.out.println("No path found from this track");
        return null;
    }

    /**
     * Finds all neighboring track components in the specified direction.
     * Includes both track segments and switches.
     *
     * @param direction The direction to search ("Right" or "Left")
     * @return List of neighboring track components
     */

    public List<Object> findNeighbors(String direction) {
        List<Object> neighbors = new ArrayList<>();
        SimulationInitializer initializer = SimulationInitializer.getInstance();

        // Find connected tracks
        for (TrackSegment track : initializer.getTrackSegments()) {
            if (track != this && isNeighboringTrack(track, direction)) {
                neighbors.add(track);
            }
        }

        // Find connected switches
        Location connectionPoint =
                  direction.equals("Right") ? endingLocation : startingLocation;
        initializer.getSwitches().stream()
                .filter(sw -> sw.getSwitchLocation().equals(connectionPoint))
                .forEach(neighbors::add);

        return neighbors;
    }

    /**
     * Checks if another track segment is a neighbor of this one in the
     * specified direction.
     *
     * @param track The track segment to check
     * @param direction The direction to check ("Right" or "Left")
     * @return true if the tracks are neighbors
     */

    private boolean isNeighboringTrack(TrackSegment track, String direction) {
        return direction.equals("Right") ?
                (track.getStartingLocation().equals(this.endingLocation) ||
                        track.getEndingLocation().equals(this.endingLocation)) :
                (track.getEndingLocation().equals(this.startingLocation) ||
                        track.getStartingLocation()
                                                .equals(this.startingLocation));
    }

    /**
     * Releases all locks & clears the current train when it leaves the segment.
     */

    public void trainLeaving() {
        currentTrain = null;
        secured = false;
        updateVisualState();
    }

    /**
     * Updates the visual representation of the track segment in the GUI.
     * Changes color based on whether the segment is secured or not.
     */

    public void updateVisualState() {
        Platform.runLater(() -> {
            SimulationGUI gui = SimulationGUI.getInstance();
            if (gui == null) return;

            for (Node node : gui.getMainPane().getChildren()) {
                if (node instanceof Group group &&
                                                  group.getUserData() == this) {
                    Color trackColor = secured ? Color.RED : Color.DARKGRAY;

                    // Update the color of all lines in the group
                    for (Node child : group.getChildren()) {
                        if (child instanceof Line line) {
                            if (line.getStroke() == Color.SADDLEBROWN) {
                                // Don't change the color of the ties
                                continue;
                            }
                            line.setStroke(trackColor);
                        } else if (child instanceof Circle circle) {
                            circle.setFill(trackColor);
                        }
                    }
                    break;
                }
            }
        });
    }

    // Setters

    public void setLeftNeighbor(TrackSegment neighbor) {
        if (neighbor != null &&
                !neighbor.getEndingLocation().equals(startingLocation)) {
            System.out.println("WARNING: Invalid left connection attempt");
            return;
        }
        leftNeighbor = neighbor;
    }

    public void setRightNeighbor(TrackSegment neighbor) {
        if (neighbor != null &&
                !neighbor.getStartingLocation().equals(endingLocation)) {
            System.out.println("WARNING: Invalid right connection attempt");
            return;
        }
        rightNeighbor = neighbor;
    }

    // Getters

    public Location getStartingLocation() { return startingLocation; }
    public Location getEndingLocation() { return endingLocation; }
    public int getSegmentCount() { return segmentCount; }
    public TrackSegment getLeftNeighbor() { return leftNeighbor; }
    public TrackSegment getRightNeighbor() { return rightNeighbor; }

    // Print format of the track segments

    @Override
    public String toString() {
        return String.format("Track from %s to %s",
                                              startingLocation, endingLocation);
    }
}