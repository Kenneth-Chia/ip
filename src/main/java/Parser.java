import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Interprets user input and converts it into structured command data. */
public class Parser {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DAY_MONTH_DATE = DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);

    /** Identifies the action requested by a parsed command. */
    public enum CommandType { MARK, UNMARK, DELETE, ADD }

    /** Holds the values Sumo needs to carry out one parsed command. */
    public static class ParsedCommand extends Command {
        private final CommandType type;
        private final Task task;
        private final int taskIndex;

        private ParsedCommand(CommandType type, Task task, int taskIndex) {
            this.type = type;
            this.task = task;
            this.taskIndex = taskIndex;
        }

        public CommandType getType() { return type; }
        public Task getTask() { return task; }
        public int getTaskIndex() { return taskIndex; }

        /** Executes a mutation that has not yet been extracted into its own command class. */
        @Override
        public void execute(TaskList tasks, Ui ui, Storage storage) throws IOException {
            switch (type) {
            case MARK:
                updateTaskStatus(tasks, ui, storage, true);
                break;
            case UNMARK:
                updateTaskStatus(tasks, ui, storage, false);
                break;
            case DELETE:
                deleteTask(tasks, ui, storage);
                break;
            case ADD:
                addTask(tasks, ui, storage);
                break;
            default:
                throw new IllegalStateException("Unsupported task command.");
            }
        }

        private void updateTaskStatus(TaskList tasks, Ui ui, Storage storage, boolean markDone)
                throws IOException {
            Task selectedTask = tasks.get(taskIndex);
            boolean wasDone = selectedTask.isDone;
            tasks.setDone(taskIndex, markDone);
            try {
                storage.save(tasks.getTasks());
            } catch (IOException exception) {
                tasks.setDone(taskIndex, wasDone);
                throw exception;
            }
            if (markDone) {
                ui.showTaskMarked(selectedTask);
            } else {
                ui.showTaskUnmarked(selectedTask);
            }
        }

        private void deleteTask(TaskList tasks, Ui ui, Storage storage) throws IOException {
            Task removedTask = tasks.delete(taskIndex);
            try {
                storage.save(tasks.getTasks());
            } catch (IOException exception) {
                tasks.insert(taskIndex, removedTask);
                throw exception;
            }
            ui.showTaskDeleted(removedTask, tasks.size());
        }

        private void addTask(TaskList tasks, Ui ui, Storage storage) throws IOException {
            tasks.add(task);
            try {
                storage.save(tasks.getTasks());
            } catch (IOException exception) {
                tasks.delete(tasks.size() - 1);
                throw exception;
            }
            ui.showTaskAdded(task, tasks.size());
        }
    }

    /**
     * Parses and validates one user command.
     *
     * @param command the user's input
     * @param taskCount the current number of tasks, used to validate task numbers
     * @return structured command data
     * @throws SumoException if the command or any argument is invalid
     */
    public Command parse(String command, int taskCount) throws SumoException {
        if (command.equals("bye")) { return new ExitCommand(); }
        if (command.equals("list")) { return new ListCommand(); }
        if (command.equals("on") || command.startsWith("on ")) {
            return parseOn(command.substring(2).trim());
        }
        if (command.equals("mark") || command.startsWith("mark ")) {
            return indexedCommand(CommandType.MARK, command.substring(4).trim(), taskCount);
        }
        if (command.equals("unmark") || command.startsWith("unmark ")) {
            return indexedCommand(CommandType.UNMARK, command.substring(6).trim(), taskCount);
        }
        if (command.equals("delete") || command.startsWith("delete ")) {
            return indexedCommand(CommandType.DELETE, command.substring(6).trim(), taskCount);
        }
        if (command.equals("todo") || command.startsWith("todo ")) {
            String description = command.substring(4).trim();
            ensureNotBlank(description, "Please add a description after 'todo'.");
            ensurePersistable(description);
            return addCommand(new Todo(description));
        }
        if (command.equals("deadline") || command.startsWith("deadline ")) {
            return parseDeadline(command.substring(8).trim());
        }
        if (command.equals("event") || command.startsWith("event ")) {
            return parseEvent(command.substring(5).trim());
        }
        throw new SumoException("I do not recognise that command. Try todo, deadline, event, list, on, mark, unmark, or delete.");
    }

    private Command parseDeadline(String taskText) throws SumoException {
        String[] parts = splitCommand(taskText, " /by ", "Use: deadline <description> /by <date>.");
        ParsedDateTime deadline = parseDateTime(parts[1], "Use: deadline <description> /by <date> [HHmm].");
        return addCommand(new Deadline(parts[0], deadline.value, deadline.includesTime));
    }

    private Command parseEvent(String taskText) throws SumoException {
        String[] parts = splitEvent(taskText);
        String message = "Use: event <description> /from <date> [HHmm] /to <date> [HHmm].";
        ParsedDateTime from = parseDateTime(parts[1], message);
        ParsedDateTime to = parseDateTime(parts[2], message);
        return addCommand(new Event(parts[0], from.value, to.value, from.includesTime, to.includesTime));
    }

    private Command parseOn(String dateText) throws SumoException {
        if (dateText.isBlank()) { throw new SumoException("Use: on <date>."); }
        try {
            return new OnCommand(parseDate(dateText));
        } catch (DateTimeParseException exception) {
            throw new SumoException("Use: on <date>. Dates must use yyyy-MM-dd or d/M/yyyy.");
        }
    }

    private ParsedCommand indexedCommand(CommandType type, String text, int taskCount) throws SumoException {
        if (text.isBlank()) { throw new SumoException("Please specify the number of the task to update."); }
        try {
            int index = Integer.parseInt(text) - 1;
            if (index < 0 || index >= taskCount) { throw new SumoException("That task number is not in your list."); }
            return new ParsedCommand(type, null, index);
        } catch (NumberFormatException exception) {
            throw new SumoException("Task numbers must be whole numbers.");
        }
    }

    private ParsedCommand addCommand(Task task) { return new ParsedCommand(CommandType.ADD, task, -1); }

    private String[] splitCommand(String text, String marker, String message) throws SumoException {
        int markerIndex = text.indexOf(marker);
        if (markerIndex < 0) { throw new SumoException(message); }
        String first = text.substring(0, markerIndex).trim();
        String second = text.substring(markerIndex + marker.length()).trim();
        ensureNotBlank(first, message);
        ensureNotBlank(second, message);
        ensurePersistable(first);
        ensurePersistable(second);
        return new String[] {first, second};
    }

    private String[] splitEvent(String text) throws SumoException {
        String message = "Use: event <description> /from <start> /to <end>.";
        String fromMarker = " /from ";
        String toMarker = " /to ";
        int fromIndex = text.indexOf(fromMarker);
        int toIndex = text.indexOf(toMarker, fromIndex + fromMarker.length());
        if (fromIndex < 0 || toIndex < 0) { throw new SumoException(message); }
        String description = text.substring(0, fromIndex).trim();
        String from = text.substring(fromIndex + fromMarker.length(), toIndex).trim();
        String to = text.substring(toIndex + toMarker.length()).trim();
        ensureNotBlank(description, message);
        ensureNotBlank(from, message);
        ensureNotBlank(to, message);
        ensurePersistable(description);
        ensurePersistable(from);
        ensurePersistable(to);
        return new String[] {description, from, to};
    }

    private ParsedDateTime parseDateTime(String text, String message) throws SumoException {
        try {
            String[] parts = text.trim().split("\\s+");
            if (parts.length == 1) { return new ParsedDateTime(parseDate(parts[0]).atStartOfDay(), false); }
            if (parts.length == 2) {
                return new ParsedDateTime(LocalDateTime.of(parseDate(parts[0]), LocalTime.parse(parts[1], TIME)), true);
            }
        } catch (DateTimeParseException exception) {
            // Use the consistent, user-friendly error below.
        }
        throw new SumoException(message + " Dates must use yyyy-MM-dd or d/M/yyyy, optionally followed by HHmm.");
    }

    private LocalDate parseDate(String text) throws DateTimeParseException {
        try { return LocalDate.parse(text, ISO_DATE); }
        catch (DateTimeParseException exception) { return LocalDate.parse(text, DAY_MONTH_DATE); }
    }

    private void ensureNotBlank(String text, String message) throws SumoException {
        if (text == null || text.isBlank()) { throw new SumoException(message); }
    }

    private void ensurePersistable(String text) throws SumoException {
        if (text.contains(" | ")) { throw new SumoException("Task text cannot contain ' | '."); }
    }

    private static class ParsedDateTime {
        private final LocalDateTime value;
        private final boolean includesTime;

        private ParsedDateTime(LocalDateTime value, boolean includesTime) {
            this.value = value;
            this.includesTime = includesTime;
        }
    }
}
