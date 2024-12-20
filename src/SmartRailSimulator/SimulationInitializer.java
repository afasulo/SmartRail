package SmartRailSimulator;

import java.io.*;
import java.util.*;

/**
 * The SimulationInitializer class is responsible for initializing all parts of
 * the smart rail simulation. We check the validity of the configuration file
 * given by the user. We then create and connect all the components of the
 * simulation.
 */
public class SimulationInitializer {
    private static SimulationInitializer instance;
    private final List<Station> stations = new ArrayList<>();
    private final List<TrackSegment> trackSegments = new ArrayList<>();
    private final List<Switch> switches = new ArrayList<>();
    private final List<Train> trains = new ArrayList<>();
    // Maps to store components by location for easier connection lookup
    private final Map<Location, Station> stationMap = new HashMap<>();
    private final Map<Location, Switch> switchMap = new HashMap<>();
    private final Map<Location, List<TrackSegment>> trackMap = new HashMap<>();

    /**
     * Checks if two line segments intersect.
     *
     * @param a The starting location of the first segment.
     * @param b The ending location of the first segment.
     * @param c The starting location of the second segment.
     * @param d The ending location of the second segment.
     * @return true if the segments intersect, false otherwise.
     */
    private boolean doSegmentsIntersect(Location a, Location b,
                                        Location c, Location d) {
        return intersectionPoint(a, c, d) != intersectionPoint(b, c, d) &&
                intersectionPoint(a, b, c) != intersectionPoint(a, b, d);
    }

    /**
     * Determines if the point c is on one side of the line segment from a to b.
     *
     * @param a The starting location of the line segment.
     * @param b The ending location of the line segment.
     * @param c The location to check against the line segment.
     * @return true if point c is on one side of the line segment from a to b,
     * false otherwise.
     */
    private boolean intersectionPoint(Location a, Location b, Location c) {
        return (c.getY() - a.getY()) * (b.getX() - a.getX()) >
                (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    /**
     * Validates the configuration of track segments by checking for
     * intersections.
     *
     * @param trackSegments The list of track segments to validate.
     * @return true if the configuration is valid (no intersections),
     * false otherwise.
     */
    private boolean validateConfig(List<TrackSegment> trackSegments) {
        for (int i = 0; i < trackSegments.size(); i++) {
            TrackSegment segment1 = trackSegments.get(i);
            for (int j = i + 1; j < trackSegments.size(); j++) {
                TrackSegment segment2 = trackSegments.get(j);

                if (doSegmentsIntersect(
                        segment1.getStartingLocation(),
                        segment1.getEndingLocation(),
                        segment2.getStartingLocation(),
                        segment2.getEndingLocation()
                )) {

                    if (segmentsShareEndPoint(segment1, segment2)) {
                        continue;
                    }

                    System.out.println("ERROR! TRACKS INTERSECTING OR " +
                                                            "TRACKS FLOATING.");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if two track segments share an endpoint.
     *
     * @param segment1 The first track segment.
     * @param segment2 The second track segment.
     * @return true if the segments share an endpoint, false otherwise.
     */
    private boolean segmentsShareEndPoint(TrackSegment segment1,
                                          TrackSegment segment2) {
        return segment1.getStartingLocation()
                .equals(segment2.getStartingLocation()) ||
                segment1.getStartingLocation()
                        .equals(segment2.getEndingLocation()) ||
                segment1.getEndingLocation()
                        .equals(segment2.getStartingLocation()) ||
                segment1.getEndingLocation()
                        .equals(segment2.getEndingLocation());
    }

    /**
     * Initializes the simulation by reading the configuration from a file.
     *
     * @param file The path to the configuration file.
     */
    public void initializeSim(String file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            // First pass: Create all components
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] coordinates = line.split(" ");
                switch (coordinates[0]) {
                    case "station" -> createStation(coordinates);
                    case "track"   -> createTrackSegment(coordinates);
                    case "switch"  -> createSwitch(coordinates);
                }
            }

            if (!validateConfig(trackSegments)) {
                throw new IllegalArgumentException("Config file rejected.");
            }

            // Second pass: Establish all connections
            establishConnections();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a station from the given coordinates and adds it to the list of
     * stations.
     *
     * @param coordinates An array containing the coordinates of the station.
     *                    The array should have exactly 3 elements:
     *                    the first element is ignored, the second element is
     *                    the x-coordinate, and the third element is the
     *                    y-coordinate.
     */
    private void createStation(String[] coordinates) {
        if (coordinates.length != 3) {
            System.out.println("Invalid line format for station");
            return;
        }

        double x = Double.parseDouble(coordinates[1]);
        double y = Double.parseDouble(coordinates[2]);
        Location location = new Location(x, y);

        Station station = new Station
                            ("Station " + stations.size(), location);
        stations.add(station);
        stationMap.put(location, station);
    }



    /**
     * Creates a track segment from the given coordinates and adds it to the
     * list of track segments.
     *
     * @param coordinates An array containing the coordinates of the track
     *                    segment.
     *                    The array should have either 5 or 6 elements:
     *                    the first element is ignored, the second and third
     *                    elements are the x and y coordinates of the start
     *                    location, the fourth and fifth elements are the x and
     *                    y coordinates of the end location, and the optional
     *                    sixth element is the number of segments to create.
     */
    private void createTrackSegment(String[] coordinates) {
        if (coordinates.length != 5 && coordinates.length != 6) {
            System.out.println("Invalid line format for track segment");
            return;
        }

        double startX = Double.parseDouble(coordinates[1]);
        double startY = Double.parseDouble(coordinates[2]);
        double endX = Double.parseDouble(coordinates[3]);
        double endY = Double.parseDouble(coordinates[4]);

        Location startLoc = new Location(startX, startY);
        Location endLoc = new Location(endX, endY);

        if (coordinates.length == 5) {
            TrackSegment segment = new TrackSegment
                                              (startLoc, endLoc, 1);
            addTrackSegmentToMap(segment);
            trackSegments.add(segment);
        } else {
            int segments = Integer.parseInt(coordinates[5]);
            createMultipleTrackSegments(startLoc, endLoc, segments);
        }

        normalizeTrackOrder(startLoc, endLoc);
    }

    /**
     * Connects track segments by setting their neighbors based on matching
     * endpoints.
     *
     * @param trackSegments The list of track segments to connect.
     */
    private void connectTrackSegments(List<TrackSegment> trackSegments) {
        System.out.println("Connecting track segments...");
        for (TrackSegment current : trackSegments) {
            for (TrackSegment other : trackSegments) {
                if (current == other) continue;

                // Connect matching endpoints
                if (current.getEndingLocation()
                        .equals(other.getStartingLocation())) {
                    current.setRightNeighbor(other);
                    other.setLeftNeighbor(current);
                }
                else if (current.getStartingLocation()
                        .equals(other.getEndingLocation())) {
                    current.setLeftNeighbor(other);
                    other.setRightNeighbor(current);
                }
            }
        }
    }

    /**
     * Creates multiple track segments between the given start and end locations.
     *
     * @param start The starting location of the track.
     * @param end The ending location of the track.
     * @param segments The number of segments to create.
     */
    private void createMultipleTrackSegments(Location start,
                                             Location end, int segments) {
        System.out.println("\nCreating " + segments + " track segments from " +
                                                          start + " to " + end);

        // Store previous segment to connect them
        TrackSegment previousSegment = null;

        // Important: Determine if this is a reverse-defined track
        boolean isReversed = end.getX() < start.getX() ||
                (end.getX() == start.getX() && end.getY() < start.getY());

        // If track is reversed, swap start & end for consistent l-to-r segments
        if (isReversed) {
            Location temp = start;
            start = end;
            end = temp;
            System.out.println("Track was defined in reverse - " +
                                                       "normalizing direction");
        }

        for (int i = 0; i < segments; i++) {
            // Calculate exact positions for smoother transitions
            double t = (double) i / segments;
            double nextT = (double) (i + 1) / segments;

            Location currentStart = new Location(
                    start.getX() + (end.getX() - start.getX()) * t,
                    start.getY() + (end.getY() - start.getY()) * t
            );

            Location currentEnd = new Location(
                    start.getX() + (end.getX() - start.getX()) * nextT,
                    start.getY() + (end.getY() - start.getY()) * nextT
            );

            TrackSegment segment = new TrackSegment
                                      (currentStart, currentEnd, 1);
            System.out.println("Created segment: " + segment);

            // Connect to previous segment if it exists
            if (previousSegment != null) {
                previousSegment.setRightNeighbor(segment);
                segment.setLeftNeighbor(previousSegment);
                System.out.println("Connected: " + previousSegment +
                                    " -> " + segment);
            }

            trackSegments.add(segment);
            addTrackSegmentToMap(segment);
            previousSegment = segment;
        }
    }

    /**
     * Creates a switch from the given coordinates and adds it to the list of
     * switches.
     *
     * @param coordinates An array containing the coordinates of the switch.
     *                    The array should have exactly 3 elements:
     *                    the first element is ignored, the second element is
     *                    the x-coordinate, and the third element is the
     *                    y-coordinate.
     */
    private void createSwitch(String[] coordinates) {
        if (coordinates.length != 3) {
            System.out.println("Invalid line format for switch");
            return;
        }

        double x = Double.parseDouble(coordinates[1]);
        double y = Double.parseDouble(coordinates[2]);
        Location location = new Location(x, y);

        Switch trackSwitch = new Switch(location, new ArrayList<>());
        switches.add(trackSwitch);
        switchMap.put(location, trackSwitch);
    }

    /**
     * Adds a track segment to the track map, associating it with its start and
     * end locations.
     *
     * @param segment The track segment to add to the map.
     */
    private void addTrackSegmentToMap(TrackSegment segment) {
        // Add to start location
        trackMap.computeIfAbsent(segment.getStartingLocation(),
                k -> new ArrayList<>()).add(segment);
        // Add to end location
        trackMap.computeIfAbsent(segment.getEndingLocation(),
                k -> new ArrayList<>()).add(segment);
    }

    /**
     * Normalizes the order of the track by ensuring the start location is
     * always less than or equal to the end location.
     *
     * @param start The starting location of the track.
     * @param end The ending location of the track.
     */
    private void normalizeTrackOrder(Location start, Location end) {
        if (start.getX() > end.getX() ||
                (start.getX() == end.getX()) && start.getY() > end.getY()) {
            Location temp = start;
            start = end;
            end = temp;
        }
    }


    /**
     * Establishes connections between track segments, switches, and stations.
     * This method first connects track segments to each other, then connects
     * switches to tracks, and finally connects stations to tracks.
     */
    private void establishConnections() {
        // First connect track segments to each other
        connectTrackSegments(trackSegments);

        // Then connect switches to tracks
        connectSwitches();

        // Finally connect stations to tracks
        connectStations();
    }

    /**
     * Connects stations to track segments by matching their locations.
     * This method iterates through all stations and finds the corresponding
     * track segment that starts or ends at the station's location, then sets
     * the connection.
     */
    private void connectStations() {
        System.out.println("Connecting stations...");
        for (Station station : stations) {
            Location stationLoc = station.getStationLocation();

            for (TrackSegment track : trackSegments) {
                if (track.getStartingLocation().equals(stationLoc)) {
                    station.setConnectedTrack(track);
                    track.setLeftNeighbor(null); // Station is at start
                    break;
                }
                else if (track.getEndingLocation().equals(stationLoc)) {
                    station.setConnectedTrack(track);
                    track.setRightNeighbor(null); // Station is at end
                    break;
                }
            }
        }
    }

    /**
     * Connects switches to track segments by matching their locations.
     * This method iterates through all switches and finds the corresponding
     * track segments that start or end at the switch's location, then sets the
     * connection.
     */
    private void connectSwitches() {
        System.out.println("Connecting switches...");
        for (Switch sw : switches) {
            Location switchLoc = sw.getSwitchLocation();
            List<TrackSegment> connectedTracks = new ArrayList<>();

            for (TrackSegment track : trackSegments) {
                if (track.getStartingLocation().equals(switchLoc) ||
                        track.getEndingLocation().equals(switchLoc)) {
                    connectedTracks.add(track);

                    // Update track's connection
                    if (track.getStartingLocation().equals(switchLoc)) {
                        track.setLeftNeighbor(null); // Switch is at start
                    } else {
                        track.setRightNeighbor(null); // Switch is at end
                    }
                    System.out.println("Connected switch at " +
                            switchLoc + " to " + track);
                }
            }
            sw.setConnectedSegments(connectedTracks);
        }
    }


    /**
     * Returns the singleton instance of the SimulationInitializer.
     * If the instance is null, it initializes a new SimulationInitializer.
     *
     * @return The singleton instance of SimulationInitializer.
     */
    public static SimulationInitializer getInstance() {
        if (instance == null) {
            instance = new SimulationInitializer();
        }
        return instance;
    }

    //Getters.

    public List<Station> getStations() {
        return stations;
    }

    public List<TrackSegment> getTrackSegments() {
        return trackSegments;
    }

    public List<Switch> getSwitches() {
        return switches;
    }
    public List<Train> getTrains() {
        return trains;
    }
}
