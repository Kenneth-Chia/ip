/**
 * Represents a task entered by the user.
 */
public class Task {
    protected String description;
    protected boolean isDone;

    /**
     * Creates a new incomplete task with the given description.
     *
     * @param description the task text
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Returns the icon used to show whether this task is done.
     *
     * @return "X" for a completed task or a space otherwise
     */
    public String getStatusIcon() {
        return (isDone ? "X" : " ");
    }

    /**
     * Returns the icon used to show the type of this task.
     *
     * <p>The base task keeps the original display format for backwards
     * compatibility. Concrete task types override this method when they have
     * a type icon.</p>
     *
     * @return an empty string for a general task
     */
    public String getTypeIcon() {
        return "";
    }

    /**
     * Returns this task in the line-based format used for persistent storage.
     *
     * @return the task type, completion status, and description
     */
    public String toDataString() {
        return getTypeIcon() + " | " + (isDone ? "1" : "0") + " | " + getDescription();
    }

    /**
     * Returns this task in the original Sumo display format.
     *
     * @return the formatted task
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + getDescription();
    }

    /**
     * Marks this task as done.
     */
    public void markAsDone() {
        isDone = true;
    }

    /**
     * Marks this task as not done.
     */
    public void markAsNotDone() {
        isDone = false;
    }

    /**
     * Returns the task description.
     *
     * @return the task description
     */
    public String getDescription() {
        return description;
    }
}
