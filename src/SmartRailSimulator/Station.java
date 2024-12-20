package SmartRailSimulator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Station class that is responsible for all the initialization of the stations
 * for the smart rail simulation. Stations are created with an x and y location,
 * as well as a name to identify. Stations are run on their own threads
 * concurrently.
 */

public class Station implements Runnable {
    private final Location stationLocation;
    private Thread thread;
    private boolean running = true;
    private final BlockingQueue<Message> messageQueue =
                                                    new LinkedBlockingQueue<>();
    private TrackSegment connectedTrack;

    // Constructor
    public Station(String stationName, Location stationLocation) {
        this.stationLocation = stationLocation;
    }

    // Thread management methods
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Run method for the Stations that allows them to be run as threads and
     * join/sleep when necessary. Takes in messages and either allows or
     * blocks the trains from departing/docking.
     */
    @Override
    public void run() {
        while (running) {
            try {
                // Blocks until message available
                Message msg = messageQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Train interrupted");
                running = false;
            }
        }
    }

    // Prints x, y location of the stations
    @Override
    public String toString() {
        return "Station " + stationLocation;
    }

    //Getter and setters

    public Location getStationLocation() {
        return stationLocation;
    }

    public void setConnectedTrack(TrackSegment track) {
        this.connectedTrack = track;
        System.out.println("Station " + stationLocation +
                                               " connected to track: " + track);

        // Update track's connection to station
        if (track.getStartingLocation().equals(stationLocation)) {
            track.setLeftNeighbor(null);  // Station is at start
        } else if (track.getEndingLocation().equals(stationLocation)) {
            track.setRightNeighbor(null);  // Station is at end
        }
    }

    public TrackSegment getConnectedTrack() {
        return connectedTrack;
    }

}
