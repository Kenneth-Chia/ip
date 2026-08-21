/**
 * A task that should be completed by a specified date or time.
 */
public class Deadline extends Task {
    private final String by;

    /**
     * Creates a new incomplete deadline.
     *
     * @param description the task text
     * @param by the date or time by which the task should be completed
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
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
    public String getBy() {
        return by;
    }

    /**
     * Returns this deadline in the format used by Sumo's task list.
     *
     * @return the formatted deadline
     */
    @Override
    public String toString() {
        return "[" + getTypeIcon() + "][" + getStatusIcon() + "] "
                + getDescription() + " (by: " + by + ")";
    }
}
