package SmartRailSimulator;

import java.util.Objects;

/**
 * Location class that is used by all components of the smart rail simulation.
 * Provides an x and y value for communication and connectivity purposes of
 * all components. Also aids in the GUI and visual aspects to streamline the
 * drawing of the components on the screen.
 */

public class Location {
    private final double x;
    private final double y;

    // Constructor

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Overridden equals method that determines if two components of the
     * simulation have the same location (x and y value).
     * @param o Object
     * @return Location X, Y
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        // Use exact integer comparison instead of floating point
        return x == location.x && y == location.y;
    }

    // Overridden hashCode to format in the correct x, y format we are using

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    // Overridden toString, displays: " (x,y) "

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    // Getters for X and Y

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}