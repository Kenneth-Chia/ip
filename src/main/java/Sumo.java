import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Starts Sumo and stores task text entered by the user.
 */
public class Sumo {
    /** Relative directory where Sumo stores its task data. */
    private static final Path DATA_DIRECTORY = Path.of("data");

    /** Relative, platform-independent path to Sumo's task data file. */
    private static final Path DATA_FILE = DATA_DIRECTORY.resolve("sumo.txt");

    public static void main(String[] args) {
        String separator = "____________________________________________________________";
        String banner = " ██████  ██    ██ ███    ███  ██████\n"
                + "██       ██    ██ ████  ████ ██    ██\n"
                + " █████   ██    ██ ██ ████ ██ ██    ██\n"
                + "     ██  ██    ██ ██  ██  ██ ██    ██\n"
                + "██████    ██████  ██      ██  ██████";
        List<Task> tasks;
        try {
            tasks = loadTasks();
        } catch (IOException exception) {
            tasks = new ArrayList<>();
            System.out.println(" I could not load your saved tasks: " + getErrorMessage(exception));
        }

        System.out.println(separator);
        System.out.println(banner);
        System.out.println("Hello! I'm Sumo.");
        System.out.println("What can I do for you?");
        System.out.println(separator);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();
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
            } catch (IOException exception) {
                System.out.println(" I could not save your tasks: " + getErrorMessage(exception));
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
            Task task = tasks.get(taskIndex);
            boolean wasDone = task.isDone;
            task.markAsDone();
            try {
                saveTasks(tasks);
            } catch (IOException exception) {
                task.isDone = wasDone;
                throw exception;
            }
            System.out.println(" Nice! I've marked this task as done:");
            System.out.println("   " + task);
            return;
        }

        if (command.equals("unmark") || command.startsWith("unmark ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            Task task = tasks.get(taskIndex);
            boolean wasDone = task.isDone;
            task.markAsNotDone();
            try {
                saveTasks(tasks);
            } catch (IOException exception) {
                task.isDone = wasDone;
                throw exception;
            }
            System.out.println(" OK, I've marked this task as not done yet:");
            System.out.println("   " + task);
            return;
        }

        if (command.equals("delete") || command.startsWith("delete ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            Task removedTask = tasks.remove(taskIndex);
            try {
                saveTasks(tasks);
            } catch (IOException exception) {
                tasks.add(taskIndex, removedTask);
                throw exception;
            }
            System.out.println(" Noted. I've removed this task:");
            System.out.println("   " + removedTask);
            System.out.println(" Now you have " + tasks.size() + " tasks in the list.");
            return;
        }

        Task addedTask;
        if (command.equals("todo") || command.startsWith("todo ")) {
            String description = command.substring(4).trim();
            ensureNotBlank(description, "Please add a description after 'todo'.");
            ensurePersistable(description);
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
        try {
            saveTasks(tasks);
        } catch (IOException exception) {
            tasks.remove(tasks.size() - 1);
            throw exception;
        }
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
        Files.createDirectories(DATA_DIRECTORY);
        List<String> taskLines = tasks.stream()
                .map(Task::toDataString)
                .toList();
        Path temporaryFile = Files.createTempFile(DATA_DIRECTORY, "sumo-", ".tmp");
        try {
            Files.write(temporaryFile, taskLines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, DATA_FILE, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Loads tasks from the data file, or returns an empty list on the first run.
     *
     * @return the tasks stored during the previous run
     * @throws IOException if the existing data file cannot be read
     */
    private static List<Task> loadTasks() throws IOException {
        List<Task> tasks = new ArrayList<>();
        Files.createDirectories(DATA_DIRECTORY);
        if (Files.notExists(DATA_FILE)) {
            return tasks;
        }

        List<String> taskLines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
        for (int lineNumber = 0; lineNumber < taskLines.size(); lineNumber++) {
            String taskLine = taskLines.get(lineNumber);
            if (taskLine.isBlank()) {
                continue;
            }
            try {
                tasks.add(parseTask(taskLine));
            } catch (IllegalArgumentException exception) {
                System.out.println(" I could not load saved task on line " + (lineNumber + 1)
                        + ": " + exception.getMessage());
            }
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
        if (taskLine == null || taskLine.isBlank()) {
            throw new IllegalArgumentException("the task record is blank.");
        }

        String[] taskData = taskLine.split(" \\| ", -1);
        int expectedFieldCount;
        switch (taskData[0]) {
        case "T":
            expectedFieldCount = 3;
            break;
        case "D":
            expectedFieldCount = 4;
            break;
        case "E":
            expectedFieldCount = 5;
            break;
        default:
            throw new IllegalArgumentException("Unknown task type in data file: " + taskData[0]);
        }

        if (taskData.length != expectedFieldCount) {
            throw new IllegalArgumentException("Invalid number of fields in data file.");
        }
        if (!taskData[1].equals("0") && !taskData[1].equals("1")) {
            throw new IllegalArgumentException("Invalid completion status in data file: " + taskData[1]);
        }
        for (int i = 2; i < taskData.length; i++) {
            if (taskData[i].isBlank()) {
                throw new IllegalArgumentException("Task fields in data file cannot be blank.");
            }
        }

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
        if (taskNumberText == null || taskNumberText.isBlank()) {
            throw new SumoException("Please specify the number of the task to update.");
        }

        try {
            int taskIndex = Integer.parseInt(taskNumberText.trim()) - 1;
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
        ensurePersistable(description);
        ensurePersistable(date);
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
        ensurePersistable(description);
        ensurePersistable(from);
        ensurePersistable(to);
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
        if (text == null || text.isBlank()) {
            throw new SumoException(message);
        }
    }

    /**
     * Rejects the separator used by the line-based data format so that a task
     * can be loaded exactly as it was entered after a restart.
     *
     * @param text the task field to validate
     * @throws SumoException if the field contains the storage separator
     */
    private static void ensurePersistable(String text) throws SumoException {
        if (text.contains(" | ")) {
            throw new SumoException("Task text cannot contain ' | '.");
        }
    }

    /**
     * Returns a useful message even when an I/O exception has no detail.
     *
     * @param exception the I/O exception
     * @return the exception detail or a general fallback message
     */
    private static String getErrorMessage(IOException exception) {
        return exception.getMessage() == null
                ? "The data file could not be accessed."
                : exception.getMessage();
    }

}
