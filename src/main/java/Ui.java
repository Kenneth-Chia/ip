import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

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

    /** Creates a UI that reads commands from standard input. */
    public Ui() {
        this.scanner = new Scanner(System.in);
    }

    /** @return whether another console command is available */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /** @return the next command, with surrounding whitespace removed */
    public String readCommand() {
        return scanner.nextLine().trim();
    }

    /** Shows the application greeting. */
    public void showWelcome() {
        System.out.println(SEPARATOR);
        System.out.println(BANNER);
        System.out.println("Hello! I'm Sumo.");
        System.out.println("What can I do for you?");
        System.out.println(SEPARATOR);
    }

    /** Shows the divider between commands and responses. */
    public void showSeparator() {
        System.out.println(SEPARATOR);
    }

    /** Shows the farewell message. */
    public void showGoodbye() {
        System.out.println("Bye. Hope to see you again soon!");
        showSeparator();
    }

    /** Shows a command validation error. */
    public void showCommandError(String message) {
        System.out.println(" I could not complete that command: " + message);
    }

    /** Shows a file loading error. */
    public void showLoadingError(String message) {
        System.out.println(" I could not load your saved tasks: " + message);
    }

    /** Shows an invalid saved-task record while allowing other records to load. */
    public void showInvalidTaskError(int lineNumber, String message) {
        System.out.println(" I could not load saved task on line " + lineNumber + ": " + message);
    }

    /** Shows a file saving error. */
    public void showSavingError(String message) {
        System.out.println(" I could not save your tasks: " + message);
    }

    /** Shows all tasks in their current order. */
    public void showTaskList(List<Task> tasks) {
        System.out.println(" Here are the tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println(" " + (i + 1) + "." + tasks.get(i));
        }
    }

    /** Shows tasks that occur on the requested date. */
    public void showTasksOnDate(LocalDateTime date, List<Task> tasks) {
        System.out.println(" Here are the tasks on " + DateTimeDisplay.format(date, false) + ":");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println(" " + (i + 1) + "." + tasks.get(i));
        }
    }

    /** Shows a task that was added and the new list size. */
    public void showTaskAdded(Task task, int taskCount) {
        System.out.println(" Got it. I've added this task:");
        System.out.println("   " + task);
        System.out.println(" Now you have " + taskCount + " tasks in the list.");
    }

    /** Shows a task that was marked complete. */
    public void showTaskMarked(Task task) {
        System.out.println(" Nice! I've marked this task as done:");
        System.out.println("   " + task);
    }

    /** Shows a task that was marked incomplete. */
    public void showTaskUnmarked(Task task) {
        System.out.println(" OK, I've marked this task as not done yet:");
        System.out.println("   " + task);
    }

    /** Shows a task that was deleted and the new list size. */
    public void showTaskDeleted(Task task, int taskCount) {
        System.out.println(" Noted. I've removed this task:");
        System.out.println("   " + task);
        System.out.println(" Now you have " + taskCount + " tasks in the list.");
    }
}
