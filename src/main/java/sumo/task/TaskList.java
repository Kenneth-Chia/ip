package sumo.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        this.tasks = new ArrayList<>(tasks);
    }

    /** @return the number of tasks in the list */
    public int size() {
        return tasks.size();
    }

    /** @return a read-only snapshot of the tasks in their current order */
    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    /** @return the task at the given zero-based index */
    public Task get(int index) {
        return tasks.get(index);
    }

    /** Adds a task to the end of the list. */
    public void add(Task task) {
        tasks.add(task);
    }

    /** Removes and returns the task at the given zero-based index. */
    public Task delete(int index) {
        return tasks.remove(index);
    }

    /** Reinserts a task at a specific position when an operation is rolled back. */
    public void insert(int index, Task task) {
        tasks.add(index, task);
    }

    /** Updates the completion status of one task. */
    public void setDone(int index, boolean isDone) {
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
        List<Task> matchingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (occursOn(task, date)) {
                matchingTasks.add(task);
            }
        }
        return matchingTasks;
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
