package sumo.task;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A task that should be completed by a specified date or time.
 */
public class Deadline extends Task {
    private final LocalDateTime by;
    private final boolean includesTime;

    /**
     * Creates a new incomplete deadline.
     *
     * @param description the task text
     * @param by the date and time by which the task should be completed
     */
    public Deadline(String description, LocalDateTime by) {
        this(description, by, true);
    }

    /**
     * Creates a new incomplete deadline for a date without a specified time.
     *
     * @param description the task text
     * @param by the date by which the task should be completed
     */
    public Deadline(String description, LocalDate by) {
        this(description, by.atStartOfDay(), false);
    }

    /**
     * Creates a deadline while retaining whether its input included a time.
     *
     * @param description the task text
     * @param by the deadline date and time
     * @param includesTime whether the user supplied a time
     */
    public Deadline(String description, LocalDateTime by, boolean includesTime) {
        super(description);
        this.by = by;
        this.includesTime = includesTime;
    }

    /**
     * Returns the deadline type icon.
     *
     * @return the deadline type icon
     */
    @Override
    public String getTypeIcon() {
        return "D";
    }

    /**
     * Returns the date or time by which this task should be completed.
     *
     * @return the deadline date or time
     */
    public LocalDateTime getBy() {
        return by;
    }

    /**
     * Returns this deadline in the line-based format used for persistent storage.
     *
     * @return the task type, completion status, description, and deadline
     */
    @Override
    public String toDataString() {
        return super.toDataString() + " | "
                + (includesTime ? by.toString() : by.toLocalDate().toString());
    }

    /**
     * Returns this deadline in the format used by Sumo's task list.
     *
     * @return the formatted deadline
     */
    @Override
    public String toString() {
        return "[" + getTypeIcon() + "][" + getStatusIcon() + "] "
                + getDescription() + " (by: " + DateTimeDisplay.format(by, includesTime) + ")";
    }
}
