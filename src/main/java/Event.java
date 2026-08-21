/**
 * A task that takes place between a specified start and end date or time.
 */
public class Event extends Task {
    private final String from;
    private final String to;

    /**
     * Creates a new incomplete event.
     *
     * @param description the event text
     * @param from the event start date or time
     * @param to the event end date or time
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the event type icon.
     *
     * @return the event type icon
     */
    @Override
    public String getTypeIcon() {
        return "E";
    }

    /**
     * Returns the event start date or time.
     *
     * @return the event start date or time
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the event end date or time.
     *
     * @return the event end date or time
     */
    public String getTo() {
        return to;
    }

    /**
     * Returns this event in the format used by Sumo's task list.
     *
     * @return the formatted event
     */
    @Override
    public String toString() {
        return "[" + getTypeIcon() + "][" + getStatusIcon() + "] "
                + getDescription() + " (from: " + from + " to: " + to + ")";
    }
}
