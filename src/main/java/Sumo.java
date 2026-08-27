import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Starts Sumo and stores task text entered by the user.
 */
public class Sumo {
    private static final Path DATA_FILE = Path.of("data", "sumo.txt");

    public static void main(String[] args) throws IOException {
        String separator = "____________________________________________________________";
        String banner = " ██████  ██    ██ ███    ███  ██████\n"
                + "██       ██    ██ ████  ████ ██    ██\n"
                + " █████   ██    ██ ██ ████ ██ ██    ██\n"
                + "     ██  ██    ██ ██  ██  ██ ██    ██\n"
                + "██████    ██████  ██      ██  ██████";
        List<Task> tasks = loadTasks();

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
    private static void handleCommand(String command, List<Task> tasks) throws SumoException, IOException {
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
            saveTasks(tasks);
            System.out.println(" Nice! I've marked this task as done:");
            System.out.println("   " + tasks.get(taskIndex));
            return;
        }

        if (command.equals("unmark") || command.startsWith("unmark ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            tasks.get(taskIndex).markAsNotDone();
            saveTasks(tasks);
            System.out.println(" OK, I've marked this task as not done yet:");
            System.out.println("   " + tasks.get(taskIndex));
            return;
        }

        if (command.equals("delete") || command.startsWith("delete ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            Task removedTask = tasks.remove(taskIndex);
            saveTasks(tasks);
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
        saveTasks(tasks);
        System.out.println(" Got it. I've added this task:");
        System.out.println("   " + addedTask);
        System.out.println(" Now you have " + tasks.size() + " tasks in the list.");
    }

    /**
     * Rewrites the data file so that it represents the current task list.
     *
     * @param tasks the current task list
     * @throws IOException if the data directory or file cannot be written
     */
    private static void saveTasks(List<Task> tasks) throws IOException {
        Files.createDirectories(DATA_FILE.getParent());
        List<String> taskLines = tasks.stream()
                .map(Task::toDataString)
                .toList();
        Files.write(DATA_FILE, taskLines, StandardCharsets.UTF_8);
    }

    /**
     * Loads tasks from the data file, or returns an empty list on the first run.
     *
     * @return the tasks stored during the previous run
     * @throws IOException if the existing data file cannot be read
     */
    private static List<Task> loadTasks() throws IOException {
        List<Task> tasks = new ArrayList<>();
        if (!Files.exists(DATA_FILE)) {
            return tasks;
        }

        for (String taskLine : Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8)) {
            tasks.add(parseTask(taskLine));
        }
        return tasks;
    }

    /**
     * Reconstructs one task from its stored type, status, and task-specific fields.
     *
     * @param taskLine one line from the data file
     * @return the reconstructed task
     */
    private static Task parseTask(String taskLine) {
        String[] taskData = taskLine.split(" \\| ", -1);
        Task task;

        switch (taskData[0]) {
        case "T":
            task = new Todo(taskData[2]);
            break;
        case "D":
            task = new Deadline(taskData[2], taskData[3]);
            break;
        case "E":
            task = new Event(taskData[2], taskData[3], taskData[4]);
            break;
        default:
            throw new IllegalArgumentException("Unknown task type in data file: " + taskData[0]);
        }

        if (taskData[1].equals("1")) {
            task.markAsDone();
        }
        return task;
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
