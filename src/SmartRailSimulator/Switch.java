package SmartRailSimulator;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Switch class that is responsible for the initialization of all necessary
 * information for the Switches in the smart rail simulation. The switches
 * in the simulation allow for trains to transfer from one rail line to
 * another. Switches are typically allowed to be used in only one direction
 * to prevent trains merging onto the track with oncoming traffic. There are
 * some configuration cases where they are usable both directions.
 */

public class Switch implements Runnable {
    // Location and track properties
    private final Location switchLocation;
    private List<TrackSegment> connectedSegments;
    private TrackSegment currentSegment;

    // Switch state
    private enum SwitchPosition {
        STRAIGHT,    // Uses primary track
        DIVERGENT    // Uses alternate track
    }
    private final SwitchPosition currentPosition = SwitchPosition.STRAIGHT;
    private boolean secured = false;

    // Thread management
    private Thread thread;
    private boolean running = true;
    public BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    // Train management
    private Train securingTrain = null;
    private Train currentTrain = null;

    // Constructor
    public Switch(Location switchLocation,
                  List<TrackSegment> connectedSegments) {
        this.switchLocation = switchLocation;
        this.connectedSegments = connectedSegments;

        if (!connectedSegments.isEmpty()) {
            this.currentSegment = connectedSegments.get(0);
        }
    }

    // Thread management methods
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Run method for the Switches that allows them to be run as threads and
     * join/sleep when necessary. Takes in messages and either allows or
     * blocks the trains from using.
     */

    @Override
    public void run() {
        while (running) {
            try {
                Message msg = messageQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Switch interrupted");
                running = false;
            }
        }
    }

    // Enacted once route request has been secured and the path is reserved
    public void trainLeaving() {
        currentTrain = null;
        secured = false;
        securingTrain = null;
    }

    // Displays where switches are being drawn at their x and y values
    @Override
    public String toString() {
        return "Switch at " + switchLocation.toString();
    }

    // Getters and setters
    public List<TrackSegment> getConnectedSegments() {return connectedSegments;}
    public Location getSwitchLocation() { return switchLocation; }
    public void setConnectedSegments(List<TrackSegment> connectedSegments) {
        this.connectedSegments = connectedSegments;
    }
}