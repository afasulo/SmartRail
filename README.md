# Smart Rail Simulator

A multi-threaded Java application that simulates a dynamic railway system with trains, tracks, stations, and switches. The simulator features a graphical user interface built with JavaFX, allowing users to visualize and interact with the railway network.

<img width="1229" alt="image" src="https://github.com/user-attachments/assets/e36c07b3-05e8-4f2d-9fb7-810ea394c4d7" />


## Features

- **Interactive GUI**: Click-to-operate interface for controlling train movements between stations 
- **Multi-threaded Architecture**: Each component (trains, tracks, stations, switches) runs on its own thread
- **Dynamic Path Finding**: Automatic route calculation for trains between stations
- **Message Passing System**: Components communicate through message passing, similar to real railway networks
- **Real-time Visual Feedback**: 
  - Color-coded track segments showing occupation status
  - Train state visualization (idle, seeking path, moving, etc.)
  - Animated train movements
  - Error notifications for invalid operations

## System Components

### Stations
- Serve as start and end points for trains
- Connect to track segments
- Visual representation with "S" marker
- Click-interactive for selecting journey endpoints

### Track Segments
- Basic building blocks of the railway network
- Can be locked by trains for safe traversal
- Visual representation with realistic railroad ties
- Color-coded status indication (gray for available, red for occupied)
- Communicates with adjacent tracks and switches through message passing

### Switches
- Allow trains to change between track segments
- Manage track branching and merging
- Visual representation with directional indicators
- Coordinates with connected tracks to manage train routing

### Trains
- Autonomous entities that navigate the railway system
- State-based operation (IDLE, SEEKING_PATH, LOCKING_PATH, MOVING)
- Path-finding capability
- Visual representation with state-based color coding
  - Gray: Idle
  - Yellow: Seeking Path
  - Red: Waiting/Locking Path
  - Green: Moving

## Technical Implementation

### Threading Model
- Each component runs on its own thread
- Message-based communication between components
- Synchronized access to shared resources

### Message Types
- ROUTE_REQUEST: Train requesting a path to destination
- ROUTE_RESPONSE: Path availability response
- LOCK_REQUEST: Request to secure a component
- LOCK_RESPONSE: Component lock status
- MOVE_REQUEST: Permission to move to next component
- MOVE_RESPONSE: Movement possibility confirmation

### Safety Features
- Track locking mechanism to prevent collisions
- Deadlock prevention through staged locking
- Visual feedback for blocked paths and errors
- Validation of track configurations

## Getting Started

### Prerequisites
- Java 8 or higher
- JavaFX
- Git (for cloning the repository)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/smart-rail-simulator.git
cd smart-rail-simulator
```

2. Compile the source code:
```bash
javac *.java
```

### Configuration File Format

The simulator uses text-based configuration files to define the railway layout. Format:
```
station x y
track startX startY endX endY [segments]
switch x y
```

Example configuration:
```
station 0 0
station 5 0
track 0 0 5 0 5
switch 2 0
```

### Running the Simulator

1. Navigate to ```src``` directory:
```bash
cd src
```
2. Compile the Java files:
```bash
javac SmartRailSimulator/*.java
```
3. Navigate back to the project root directory and create the JAR file:
```bash
jar cfe SmartRailSimulator.jar SmartRailSimulator.SmartRailSimulator -C src .
```
5. Run the simulator with a configuration file:
```bash
java -jar SmartRailSimulator.jar example_configs/sample.txt
```

3. Using the GUI:
   - Click on a station to select it as the starting point
   - Click on another station to select it as the destination
   - The train will automatically calculate and follow the route

## Error Handling

The simulator includes comprehensive error handling for:
- Invalid track configurations
- Impossible routes
- Occupied/LOCKED tracks
- Invalid station selections

Errors are displayed through an on-screen notification system with fade-out animation.

## Project Structure

```
smart-rail-simulator/
├── src/
│   ├── Location.java
│   ├── Message.java
│   ├── SimulationGUI.java
│   ├── SimulationInitializer.java
│   ├── SmartRailSimulator.java
│   ├── Station.java
│   ├── Switch.java
│   ├── Train.java
│   └── TrackSegment.java
├── example_configs/
│   ├── simple.txt
│   └── simple2.txt, etc...
├── README.md
```

## Troubleshooting

Common issues and solutions:
1. JavaFX not found:
   - Ensure JavaFX is included in your Java installation
   - Add JavaFX to your classpath if needed

2. Configuration file errors:
   - Verify file format matches the specification
   - Check for valid coordinate values
   - Ensure all components are properly connected

3. GUI not responding:
   - Check Java version compatibility
   - Verify system resources availability
   - Review thread status in logs

## Future Enhancements

Potential areas for future development:
- More complex switch configurations
- Traffic optimization algorithms
- Timetable-based scheduling
- Emergency stop functionality

<<<<<<< HEAD
## Warning Acknowledgement

- SimulationGUI: Track visuals used on line 302, fixing warning causes other functionality issues.
- SimulationInitializer: StationMap and SwitchMap queried on lines 160 and 306. End and temp used for temporary variable assignments.
- Station: Parameter StationName necessary to match calling of Station in other classes with a name. Msg necessary for try block of run().
- Switch: Local variable switch causes issues when referencing those variables later. Other fields labeled as "never used" are called in other places of the program, therefore necessary to be there.
- TrackSegment: Getter methods and stop() method left for future implementation possibilities.
- Train: Getter, Setter, and stop() methods left for future implementations. Parameter on line 393 left for the format of calling that method later in the program

## Contributing
=======
## Community Contributing
>>>>>>> origin/main

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is open source and available under the MIT License.

## Acknowledgments

## Contact Contributors

Oscar McCoy - [omccoy1@unm.edu](mailto:omccoy1@unm.edu)

Adam Fasulo - [afasulo3@unm.edu](mailto:afasulo3@unm.edu)

Project PDF: [https://github.com/UNM-CS351/project-4-smartrail-project-4-group-06/blob/main/doc/cs351project4-smartrail.pdf]
