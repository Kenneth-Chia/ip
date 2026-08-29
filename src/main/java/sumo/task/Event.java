package sumo.task;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A task that takes place between a specified start and end date or time.
 */
public class Event extends Task {
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final boolean fromIncludesTime;
    private final boolean toIncludesTime;

    /**
     * Creates a new incomplete event.
     *
     * @param description the event text
     * @param from the event start date and time
     * @param to the event end date and time
     */
    public Event(String description, LocalDateTime from, LocalDateTime to) {
        this(description, from, to, true, true);
    }

    /**
     * Creates a new incomplete event for date-only values.
     *
     * @param description the event text
     * @param from the event start date
     * @param to the event end date
     */
    public Event(String description, LocalDate from, LocalDate to) {
        this(description, from.atStartOfDay(), to.atStartOfDay(), false, false);
    }

    /**
     * Creates an event while retaining whether each input included a time.
     *
     * @param description the event text
     * @param from the event start date and time
     * @param to the event end date and time
     * @param fromIncludesTime whether the start input included a time
     * @param toIncludesTime whether the end input included a time
     */
    public Event(String description, LocalDateTime from, LocalDateTime to,
            boolean fromIncludesTime, boolean toIncludesTime) {
        super(description);
        this.from = from;
        this.to = to;
        this.fromIncludesTime = fromIncludesTime;
        this.toIncludesTime = toIncludesTime;
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
    public LocalDateTime getFrom() {
        return from;
    }

    /**
     * Returns the event end date or time.
     *
     * @return the event end date or time
     */
    public LocalDateTime getTo() {
        return to;
    }

    /**
     * Returns this event in the line-based format used for persistent storage.
     *
     * @return the task type, completion status, description, start, and end
     */
    @Override
    public String toDataString() {
        String storedFrom = fromIncludesTime ? from.toString() : from.toLocalDate().toString();
        String storedTo = toIncludesTime ? to.toString() : to.toLocalDate().toString();
        return super.toDataString() + " | " + storedFrom + " | " + storedTo;
    }

    /**
     * Returns this event in the format used by Sumo's task list.
     *
     * @return the formatted event
     */
    @Override
    public String toString() {
        return "[" + getTypeIcon() + "][" + getStatusIcon() + "] "
                + getDescription() + " (from: " + DateTimeDisplay.format(from, fromIncludesTime)
                + " to: " + DateTimeDisplay.format(to, toIncludesTime) + ")";
    }
}
