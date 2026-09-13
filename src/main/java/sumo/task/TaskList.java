package sumo.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Owns Sumo's ordered collection of tasks and its task-list operations.
 */
public class TaskList {
    private final List<Task> tasks;

    /** Creates an empty task list. */
    public TaskList() {
        this.tasks = new ArrayList<>();
    }

    /**
     * Creates a task list containing the loaded tasks.
     *
     * @param tasks tasks loaded from storage
     */
    public TaskList(List<Task> tasks) {
        assert tasks != null && tasks.stream().allMatch(task -> task != null)
                : "A task list must contain only non-null tasks.";
        this.tasks = new ArrayList<>(tasks);
    }

    /**
     * Returns the number of tasks in the list.
     *
     * @return the number of tasks in the list
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns a read-only snapshot of the tasks in their current order.
     *
     * @return a read-only snapshot of the tasks in their current order
     */
    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    /**
     * Returns the task at the given zero-based index.
     *
     * @param index zero-based position of the task
     * @return the task at the given index
     */
    public Task get(int index) {
        return tasks.get(index);
    }

    /**
     * Adds a task to the end of the list.
     *
     * @param task task to add
     */
    public void add(Task task) {
        assert task != null : "A task list cannot contain a null task.";
        tasks.add(task);
    }

    /**
     * Removes and returns the task at the given zero-based index.
     *
     * @param index zero-based position of the task
     * @return the removed task
     */
    public Task delete(int index) {
        return tasks.remove(index);
    }

    /**
     * Reinserts a task at a specific position when an operation is rolled back.
     *
     * @param index zero-based position at which to insert the task
     * @param task task to insert
     */
    public void insert(int index, Task task) {
        assert index >= 0 && index <= tasks.size() : "Rollback index must be within the list bounds.";
        assert task != null : "A task list cannot contain a null task.";
        tasks.add(index, task);
    }

    /**
     * Updates the completion status of one task.
     *
     * @param index zero-based position of the task
     * @param isDone whether the task should be marked complete
     */
    public void setDone(int index, boolean isDone) {
        assert index >= 0 && index < tasks.size() : "Task status updates require a valid task index.";
        if (isDone) {
            tasks.get(index).markAsDone();
        } else {
            tasks.get(index).markAsNotDone();
        }
    }

    /**
     * Finds deadlines and events that occur on the requested date.
     *
     * @param date date to search for
     * @return matching tasks in their original order
     */
    public List<Task> findOn(LocalDate date) {
        assert date != null : "A date is required when filtering tasks.";
        return tasks.stream()
                .filter(task -> occursOn(task, date))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Finds tasks whose descriptions contain the requested keyword.
     *
     * @param keyword text to search for
     * @return matching tasks in their original order
     */
    public List<Task> find(String keyword) {
        assert keyword != null : "A keyword is required when searching tasks.";
        return tasks.stream()
                .filter(task -> task.getDescription().contains(keyword))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Determines whether a deadline or event covers the requested date. */
    private boolean occursOn(Task task, LocalDate date) {
        if (task instanceof Deadline deadline) {
            return deadline.getBy().toLocalDate().equals(date);
        }
        if (task instanceof Event event) {
            LocalDate from = event.getFrom().toLocalDate();
            LocalDate to = event.getTo().toLocalDate();
            return !date.isBefore(from) && !date.isAfter(to);
        }
        return false;
    }
}
