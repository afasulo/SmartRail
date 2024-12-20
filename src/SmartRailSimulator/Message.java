package SmartRailSimulator;

/**
 * Message class that is responsible for all components of the smart rail
 * system communicating to each other. Train components request a route from
 * the track, station, and switch components. The secondary components respond
 * to the route request. After a route is requested and responded to, there is
 * then a lock request and response to reserve the track segments for that
 * specific train. After these two steps, the move is requested and responded
 * to.
 */

public class Message {
    private final String message;
    private final String recipient;
    private final MessageType type;
    private final Object sender;
    private final Object additionalData;

    public enum MessageType {
        ROUTE_REQUEST, // Train requesting a route to destination
        ROUTE_RESPONSE, // response about route availability
        LOCK_REQUEST, // Request to lock a component
        LOCK_RESPONSE, // RESPONSE about lock status
        MOVE_REQUEST, // REQUEST to move to next component
        MOVE_RESPONSE // Response about movement possibility
    }

    // Constructor

    public Message(String message, String recipient, MessageType type,
                   Object sender, Object additionalData) {
        this.message = message;
        this.recipient = recipient;
        this.type = type;
        this.sender = sender;
        this.additionalData = additionalData;
    }

    // Type, sender, and additional data getters

    public MessageType getType() {
        return type;
    }

    public Object getSender () {
        return sender;
    }

    public Object getAdditionalData(){
        return additionalData;
    }

    /**
     * Overridden toString method to correctly print out the information
     * of the messages. This information includes the message itself, the
     * sender, recipient, and any additional data included.
     * @return Message toString
     */

    @Override
    public String toString() {
        if (type != null) {
            return String.format("Message{type=%s, message='%s', " +
                                "recipient='%s', sender=%s, additionalData=%s}",
                    type, message, recipient, sender, additionalData);
        }
        return String.format("Message{message='%s', recipient='%s'}",
                                                            message, recipient);
    }
}
