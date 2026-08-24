import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Starts Sumo and stores task text entered by the user.
 */
public class Sumo {
    public static void main(String[] args) {
        String separator = "____________________________________________________________";
        String banner = " ██████  ██    ██ ███    ███  ██████\n"
                + "██       ██    ██ ████  ████ ██    ██\n"
                + " █████   ██    ██ ██ ████ ██ ██    ██\n"
                + "     ██  ██    ██ ██  ██  ██ ██    ██\n"
                + "██████    ██████  ██      ██  ██████";
        List<Task> tasks = new ArrayList<>();

        System.out.println(separator);
        System.out.println(banner);
        System.out.println("Hello! I'm Sumo.");
        System.out.println("What can I do for you?");
        System.out.println(separator);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            System.out.println(separator);

            if (command.equals("bye")) {
                System.out.println("Bye. Hope to see you again soon!");
                System.out.println(separator);
                break;
            }

            try {
                handleCommand(command, tasks);
            } catch (SumoException exception) {
                System.out.println(" I could not complete that command: " + exception.getMessage());
            }

            System.out.println(separator);
        }
    }

    /**
     * Carries out one non-exit command.
     *
     * @param command the command entered by the user
     * @param tasks the task list
     * @throws SumoException if the command is not valid
     */
    private static void handleCommand(String command, List<Task> tasks) throws SumoException {
        if (command.equals("list")) {
            System.out.println(" Here are the tasks in your list:");
            for (int i = 0; i < tasks.size(); i++) {
                System.out.println(" " + (i + 1) + "." + tasks.get(i));
            }
            return;
        }

        if (command.equals("mark") || command.startsWith("mark ")) {
            int taskIndex = getTaskIndex(command.substring(4).trim(), tasks.size());
            tasks.get(taskIndex).markAsDone();
            System.out.println(" Nice! I've marked this task as done:");
            System.out.println("   " + tasks.get(taskIndex));
            return;
        }

        if (command.equals("unmark") || command.startsWith("unmark ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            tasks.get(taskIndex).markAsNotDone();
            System.out.println(" OK, I've marked this task as not done yet:");
            System.out.println("   " + tasks.get(taskIndex));
            return;
        }

        if (command.equals("delete") || command.startsWith("delete ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            Task removedTask = tasks.remove(taskIndex);
            System.out.println(" Noted. I've removed this task:");
            System.out.println("   " + removedTask);
            System.out.println(" Now you have " + tasks.size() + " tasks in the list.");
            return;
        }

        Task addedTask;
        if (command.equals("todo") || command.startsWith("todo ")) {
            String description = command.substring(4).trim();
            ensureNotBlank(description, "Please add a description after 'todo'.");
            addedTask = new Todo(description);
        } else if (command.equals("deadline") || command.startsWith("deadline ")) {
            String[] deadlineParts = splitCommand(command.substring(8).trim(), " /by ",
                    "Use: deadline <description> /by <date>.");
            addedTask = new Deadline(deadlineParts[0], deadlineParts[1]);
        } else if (command.equals("event") || command.startsWith("event ")) {
            String[] eventParts = splitEvent(command.substring(5).trim());
            addedTask = new Event(eventParts[0], eventParts[1], eventParts[2]);
        } else {
            throw new SumoException("I do not recognise that command. Try todo, deadline, event, list, mark, unmark, or delete.");
        }

        tasks.add(addedTask);
        System.out.println(" Got it. I've added this task:");
        System.out.println("   " + addedTask);
        System.out.println(" Now you have " + tasks.size() + " tasks in the list.");
    }

    /**
     * Converts a task number into an array index after checking that it exists.
     *
     * @param taskNumberText the task number supplied by the user
     * @param taskCount the number of stored tasks
     * @return the zero-based task index
     * @throws SumoException if the number is absent, not numeric, or out of range
     */
    private static int getTaskIndex(String taskNumberText, int taskCount) throws SumoException {
        if (taskNumberText.isEmpty()) {
            throw new SumoException("Please specify the number of the task to update.");
        }

        try {
            int taskIndex = Integer.parseInt(taskNumberText) - 1;
            if (taskIndex < 0 || taskIndex >= taskCount) {
                throw new SumoException("That task number is not in your list.");
            }
            return taskIndex;
        } catch (NumberFormatException exception) {
            throw new SumoException("Task numbers must be whole numbers.");
        }
    }

    /**
     * Splits a deadline command into its description and due date.
     *
     * @param taskText the text after the deadline command
     * @param marker the text that separates the two parts
     * @param formatMessage the message to show for an invalid command
     * @return the description and due date
     * @throws SumoException if either part is missing
     */
    private static String[] splitCommand(String taskText, String marker, String formatMessage) throws SumoException {
        int markerIndex = taskText.indexOf(marker);
        if (markerIndex < 0) {
            throw new SumoException(formatMessage);
        }

        String description = taskText.substring(0, markerIndex).trim();
        String date = taskText.substring(markerIndex + marker.length()).trim();
        ensureNotBlank(description, formatMessage);
        ensureNotBlank(date, formatMessage);
        return new String[] {description, date};
    }

    /**
     * Splits an event command into its description, start, and end times.
     *
     * @param taskText the text after the event command
     * @return the description, start time, and end time
     * @throws SumoException if a required event part is missing
     */
    private static String[] splitEvent(String taskText) throws SumoException {
        String formatMessage = "Use: event <description> /from <start> /to <end>.";
        String fromMarker = " /from ";
        String toMarker = " /to ";
        int fromIndex = taskText.indexOf(fromMarker);
        int toIndex = taskText.indexOf(toMarker, fromIndex + fromMarker.length());
        if (fromIndex < 0 || toIndex < 0) {
            throw new SumoException(formatMessage);
        }

        String description = taskText.substring(0, fromIndex).trim();
        String from = taskText.substring(fromIndex + fromMarker.length(), toIndex).trim();
        String to = taskText.substring(toIndex + toMarker.length()).trim();
        ensureNotBlank(description, formatMessage);
        ensureNotBlank(from, formatMessage);
        ensureNotBlank(to, formatMessage);
        return new String[] {description, from, to};
    }

    /**
     * Ensures a required command part contains text.
     *
     * @param text the command part to validate
     * @param message the explanation to show when it is blank
     * @throws SumoException if the text is blank
     */
    private static void ensureNotBlank(String text, String message) throws SumoException {
        if (text.isEmpty()) {
            throw new SumoException(message);
        }
    }

}
