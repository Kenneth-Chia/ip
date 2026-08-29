import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and saves Sumo tasks using a line-based data file.
 */
public class Storage {
    private final Path dataFile;

    /**
     * Creates storage backed by the given file.
     *
     * @param dataFile path to the task data file
     */
    public Storage(Path dataFile) {
        this.dataFile = dataFile;
    }

    /**
     * Loads tasks from the data file, or returns an empty list on the first run.
     * Invalid records are reported and skipped so that valid records still load.
     *
     * @param ui console interface used to report invalid saved records
     * @return tasks stored during the previous run
     * @throws IOException if the data directory or existing file cannot be read
     */
    public List<Task> load(Ui ui) throws IOException {
        List<Task> tasks = new ArrayList<>();
        Path dataDirectory = getDataDirectory();
        Files.createDirectories(dataDirectory);
        if (Files.notExists(dataFile)) {
            return tasks;
        }

        List<String> taskLines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
        for (int lineNumber = 0; lineNumber < taskLines.size(); lineNumber++) {
            String taskLine = taskLines.get(lineNumber);
            if (taskLine.isBlank()) {
                continue;
            }
            try {
                tasks.add(parseTask(taskLine));
            } catch (IllegalArgumentException exception) {
                ui.showInvalidTaskError(lineNumber + 1, exception.getMessage());
            }
        }
        return tasks;
    }

    /**
     * Atomically rewrites the data file to represent the current task list when
     * the file system supports atomic moves.
     *
     * @param tasks current task list
     * @throws IOException if the data directory or file cannot be written
     */
    public void save(List<Task> tasks) throws IOException {
        Path dataDirectory = getDataDirectory();
        Files.createDirectories(dataDirectory);
        List<String> taskLines = tasks.stream()
                .map(Task::toDataString)
                .toList();
        Path temporaryFile = Files.createTempFile(dataDirectory, "sumo-", ".tmp");
        try {
            Files.write(temporaryFile, taskLines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, dataFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** @return the directory containing the configured data file */
    private Path getDataDirectory() {
        Path parent = dataFile.getParent();
        return parent == null ? Path.of(".") : parent;
    }

    /** Reconstructs one task from its stored fields. */
    private Task parseTask(String taskLine) {
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
            ParsedDateTime deadline = parseStoredDateTime(taskData[3]);
            task = new Deadline(taskData[2], deadline.value, deadline.includesTime);
            break;
        case "E":
            ParsedDateTime from = parseStoredDateTime(taskData[3]);
            ParsedDateTime to = parseStoredDateTime(taskData[4]);
            task = new Event(taskData[2], from.value, to.value,
                    from.includesTime, to.includesTime);
            break;
        default:
            throw new IllegalArgumentException("Unknown task type in data file: " + taskData[0]);
        }

        if (taskData[1].equals("1")) {
            task.markAsDone();
        }
        return task;
    }

    /** Parses a canonical date or date-time stored in the data file. */
    private ParsedDateTime parseStoredDateTime(String text) {
        try {
            if (text.contains("T")) {
                return new ParsedDateTime(LocalDateTime.parse(text), true);
            }
            return new ParsedDateTime(LocalDate.parse(text).atStartOfDay(), false);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid date or time in data file: " + text);
        }
    }

    /** Holds a stored date/time and whether the record includes a time. */
    private static final class ParsedDateTime {
        private final LocalDateTime value;
        private final boolean includesTime;

        private ParsedDateTime(LocalDateTime value, boolean includesTime) {
            this.value = value;
            this.includesTime = includesTime;
        }
    }
}
