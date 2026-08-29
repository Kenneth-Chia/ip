import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Starts Sumo and stores task text entered by the user.
 */
public class Sumo {
    /** Date format accepted in commands. */
    private static final DateTimeFormatter ISO_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.STRICT);

    /** Day/month/year format accepted in commands. */
    private static final DateTimeFormatter DAY_MONTH_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.STRICT);

    /** 24-hour time format accepted in commands. */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmm", Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.STRICT);

    public static void main(String[] args) {
        Ui ui = new Ui();
        Storage storage = new Storage(Path.of("data", "sumo.txt"));
        List<Task> tasks;
        try {
            tasks = storage.load(ui);
        } catch (IOException exception) {
            tasks = new ArrayList<>();
            ui.showLoadingError(getErrorMessage(exception));
        }

        ui.showWelcome();

        while (ui.hasNextCommand()) {
            String command = ui.readCommand();
            ui.showSeparator();

            if (command.equals("bye")) {
                ui.showGoodbye();
                break;
            }

            try {
                handleCommand(command, tasks, ui, storage);
            } catch (SumoException exception) {
                ui.showCommandError(exception.getMessage());
            } catch (IOException exception) {
                ui.showSavingError(getErrorMessage(exception));
            }

            ui.showSeparator();
        }
    }

    /**
     * Carries out one non-exit command.
     *
     * @param command the command entered by the user
     * @param tasks the task list
     * @param ui the console interface used to display command results
     * @param storage the data-file manager used after task-list changes
     * @throws SumoException if the command is not valid
     */
    private static void handleCommand(String command, List<Task> tasks, Ui ui, Storage storage)
            throws SumoException, IOException {
        if (command.equals("list")) {
            ui.showTaskList(tasks);
            return;
        }

        if (command.equals("on") || command.startsWith("on ")) {
            printTasksOnDate(command.substring(2).trim(), tasks, ui);
            return;
        }

        if (command.equals("mark") || command.startsWith("mark ")) {
            int taskIndex = getTaskIndex(command.substring(4).trim(), tasks.size());
            Task task = tasks.get(taskIndex);
            boolean wasDone = task.isDone;
            task.markAsDone();
            try {
                storage.save(tasks);
            } catch (IOException exception) {
                task.isDone = wasDone;
                throw exception;
            }
            ui.showTaskMarked(task);
            return;
        }

        if (command.equals("unmark") || command.startsWith("unmark ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            Task task = tasks.get(taskIndex);
            boolean wasDone = task.isDone;
            task.markAsNotDone();
            try {
                storage.save(tasks);
            } catch (IOException exception) {
                task.isDone = wasDone;
                throw exception;
            }
            ui.showTaskUnmarked(task);
            return;
        }

        if (command.equals("delete") || command.startsWith("delete ")) {
            int taskIndex = getTaskIndex(command.substring(6).trim(), tasks.size());
            Task removedTask = tasks.remove(taskIndex);
            try {
                storage.save(tasks);
            } catch (IOException exception) {
                tasks.add(taskIndex, removedTask);
                throw exception;
            }
            ui.showTaskDeleted(removedTask, tasks.size());
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
            ParsedDateTime deadline = parseDateTime(deadlineParts[1],
                    "Use: deadline <description> /by <date> [HHmm].");
            addedTask = new Deadline(deadlineParts[0], deadline.value, deadline.includesTime);
        } else if (command.equals("event") || command.startsWith("event ")) {
            String[] eventParts = splitEvent(command.substring(5).trim());
            ParsedDateTime from = parseDateTime(eventParts[1],
                    "Use: event <description> /from <date> [HHmm] /to <date> [HHmm].");
            ParsedDateTime to = parseDateTime(eventParts[2],
                    "Use: event <description> /from <date> [HHmm] /to <date> [HHmm].");
            addedTask = new Event(eventParts[0], from.value, to.value,
                    from.includesTime, to.includesTime);
        } else {
            throw new SumoException("I do not recognise that command. Try todo, deadline, event, list, on, mark, unmark, or delete.");
        }

        tasks.add(addedTask);
        try {
            storage.save(tasks);
        } catch (IOException exception) {
            tasks.remove(tasks.size() - 1);
            throw exception;
        }
        ui.showTaskAdded(addedTask, tasks.size());
    }

    /**
     * Prints deadlines and events that occur on a requested date.
     * Multi-day events match every date from their start through their end.
     *
     * @param dateText the date supplied after the {@code on} command
     * @param tasks the task list to search
     * @param ui the console interface used to display matching tasks
     * @throws SumoException if the date is missing or invalid
     */
    private static void printTasksOnDate(String dateText, List<Task> tasks, Ui ui) throws SumoException {
        if (dateText.isBlank()) {
            throw new SumoException("Use: on <date>.");
        }

        LocalDate date;
        try {
            date = parseDate(dateText);
        } catch (DateTimeParseException exception) {
            throw new SumoException("Use: on <date>. Dates must use yyyy-MM-dd or d/M/yyyy.");
        }

        List<Task> matchingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (occursOn(task, date)) {
                matchingTasks.add(task);
            }
        }
        ui.showTasksOnDate(date.atStartOfDay(), matchingTasks);
    }

    /**
     * Checks whether a deadline or event occurs on a date.
     *
     * @param task the task to check
     * @param date the requested date
     * @return true when the task occurs on the requested date
     */
    private static boolean occursOn(Task task, LocalDate date) {
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
     * Parses a date or date-time supplied in a command.
     *
     * @param text the date or date-time text
     * @param errorMessage the message to show when parsing fails
     * @return the typed value and whether a time was supplied
     * @throws SumoException if the value does not use a supported format
     */
    private static ParsedDateTime parseDateTime(String text, String errorMessage) throws SumoException {
        try {
            String[] parts = text.trim().split("\\s+");
            if (parts.length == 1) {
                return new ParsedDateTime(parseDate(parts[0]).atStartOfDay(), false);
            }
            if (parts.length == 2) {
                LocalDate date = parseDate(parts[0]);
                LocalTime time = LocalTime.parse(parts[1], TIME_FORMATTER);
                return new ParsedDateTime(LocalDateTime.of(date, time), true);
            }
        } catch (DateTimeParseException exception) {
            // Fall through to the user-friendly command error below.
        }
        throw new SumoException(errorMessage
                + " Dates must use yyyy-MM-dd or d/M/yyyy, optionally followed by HHmm.");
    }

    /**
     * Parses one supported date format.
     *
     * @param text the date text
     * @return the parsed date
     * @throws DateTimeParseException if the date is invalid
     */
    private static LocalDate parseDate(String text) throws DateTimeParseException {
        try {
            return LocalDate.parse(text, ISO_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return LocalDate.parse(text, DAY_MONTH_DATE_FORMATTER);
        }
    }

    /** Holds a parsed date/time and whether the original value included a time. */
    private static final class ParsedDateTime {
        private final LocalDateTime value;
        private final boolean includesTime;

        private ParsedDateTime(LocalDateTime value, boolean includesTime) {
            this.value = value;
            this.includesTime = includesTime;
        }
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
