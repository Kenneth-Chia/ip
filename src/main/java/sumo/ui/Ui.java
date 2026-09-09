package sumo.ui;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import sumo.task.DateTimeDisplay;
import sumo.task.Task;

/**
 * Handles all console input and output for Sumo.
 */
public class Ui {
    private static final String SEPARATOR =
            "____________________________________________________________";
    private static final String BANNER = " ██████  ██    ██ ███    ███  ██████\n"
            + "██       ██    ██ ████  ████ ██    ██\n"
            + " █████   ██    ██ ██ ████ ██ ██    ██\n"
            + "     ██  ██    ██ ██  ██  ██ ██    ██\n"
            + "██████    ██████  ██      ██  ██████";

    private final Scanner scanner;
    private final Consumer<String> output;

    /** Creates a UI that reads commands from standard input. */
    public Ui() {
        this(System.out::println);
    }

    /**
     * Creates a UI that sends each output line to the given destination.
     *
     * @param output destination for response lines
     */
    public Ui(Consumer<String> output) {
        this.scanner = new Scanner(System.in);
        this.output = output;
    }

    /**
     * Returns whether another console command is available.
     *
     * @return whether another console command is available
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Returns the next command with surrounding whitespace removed.
     *
     * @return the next command, with surrounding whitespace removed
     */
    public String readCommand() {
        return scanner.nextLine().trim();
    }

    /** Shows the application greeting. */
    public void showWelcome() {
        output.accept(SEPARATOR);
        output.accept(BANNER);
        output.accept("Hello! I'm Sumo.");
        output.accept("What can I do for you?");
        output.accept(SEPARATOR);
    }

    /** Shows the divider between commands and responses. */
    public void showSeparator() {
        output.accept(SEPARATOR);
    }

    /** Shows the farewell message. */
    public void showGoodbye() {
        output.accept("Bye. Hope to see you again soon!");
    }

    /**
     * Shows a command validation error.
     *
     * @param message description of the validation error
     */
    public void showCommandError(String message) {
        output.accept(" I could not complete that command: " + message);
    }

    /**
     * Shows a file loading error.
     *
     * @param message description of the loading error
     */
    public void showLoadingError(String message) {
        output.accept(" I could not load your saved tasks: " + message);
    }

    /**
     * Shows an invalid saved-task record while allowing other records to load.
     *
     * @param lineNumber one-based line number of the invalid record
     * @param message description of the record error
     */
    public void showInvalidTaskError(int lineNumber, String message) {
        output.accept(" I could not load saved task on line " + lineNumber + ": " + message);
    }

    /**
     * Shows a file saving error.
     *
     * @param message description of the saving error
     */
    public void showSavingError(String message) {
        output.accept(" I could not save your tasks: " + message);
    }

    /**
     * Shows all tasks in their current order.
     *
     * @param tasks tasks to display
     */
    public void showTaskList(List<Task> tasks) {
        output.accept(" Here are the tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            output.accept(" " + (i + 1) + "." + tasks.get(i));
        }
    }

    /**
     * Shows tasks whose descriptions contain the requested keyword.
     *
     * @param tasks matching tasks to display
     */
    public void showMatchingTasks(List<Task> tasks) {
        output.accept(" Here are the matching tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            output.accept(" " + (i + 1) + "." + tasks.get(i));
        }
    }

    /**
     * Shows tasks that occur on the requested date.
     *
     * @param date requested date
     * @param tasks tasks occurring on the requested date
     */
    public void showTasksOnDate(LocalDateTime date, List<Task> tasks) {
        output.accept(" Here are the tasks on " + DateTimeDisplay.format(date, false) + ":");
        for (int i = 0; i < tasks.size(); i++) {
            output.accept(" " + (i + 1) + "." + tasks.get(i));
        }
    }

    /**
     * Shows a task that was added and the new list size.
     *
     * @param task task that was added
     * @param taskCount number of tasks after the addition
     */
    public void showTaskAdded(Task task, int taskCount) {
        output.accept(" Got it. I've added this task:");
        output.accept("   " + task);
        output.accept(" Now you have " + taskCount + " tasks in the list.");
    }

    /**
     * Shows a task that was marked complete.
     *
     * @param task task that was marked complete
     */
    public void showTaskMarked(Task task) {
        output.accept(" Nice! I've marked this task as done:");
        output.accept("   " + task);
    }

    /**
     * Shows a task that was marked incomplete.
     *
     * @param task task that was marked incomplete
     */
    public void showTaskUnmarked(Task task) {
        output.accept(" OK, I've marked this task as not done yet:");
        output.accept("   " + task);
    }

    /**
     * Shows a task that was deleted and the new list size.
     *
     * @param task task that was deleted
     * @param taskCount number of tasks after the deletion
     */
    public void showTaskDeleted(Task task, int taskCount) {
        output.accept(" Noted. I've removed this task:");
        output.accept("   " + task);
        output.accept(" Now you have " + taskCount + " tasks in the list.");
    }
}
