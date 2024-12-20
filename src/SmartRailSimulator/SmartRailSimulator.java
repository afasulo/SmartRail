package SmartRailSimulator;

/**
 * SmartRailSimulator class that is the main organizer of the whole simulation.
 * Can be thought of as the "Game Manager" class. This class prompts the user
 * for a configuration file as an argument. If the file is in the valid format,
 * the simulation is started.
 */

public class SmartRailSimulator {
    public static void main(String[] args) {
        // Config file
        if (args.length == 0) {
            System.out.println("Please provide a file as a command line arg.");
        }

        String file = args[0];
        SimulationInitializer initializer = SimulationInitializer.getInstance();

        System.out.println("Starting smart rail simulation from file: " + file);
        initializer.initializeSim(file);

        //Each component running on a thread.

        initializer.getStations().forEach(Station::start);
        initializer.getTrackSegments().forEach(TrackSegment::start);
        initializer.getSwitches().forEach(Switch::start);
        initializer.getTrains().forEach(Train::start);

        // Display all components once initialized from the config file

        System.out.println("Stations: " + initializer.getStations());
        System.out.println("Track Segments: " + initializer.getTrackSegments());
        System.out.println("Switches: " + initializer.getSwitches());

        // Run the simulation

        SimulationGUI.launchGUI(initializer.getStations(),
                                initializer.getTrackSegments(),
                                initializer.getSwitches());
    }
}
