package sumo.storage;

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

import sumo.task.Deadline;
import sumo.task.Event;
import sumo.task.Task;
import sumo.task.TaskType;
import sumo.task.Todo;
import sumo.ui.Ui;

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
        TaskType taskType = TaskType.fromStorageCode(taskData[0]);

        if (taskData.length != taskType.getStoredFieldCount()) {
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

        Task task = switch (taskType) {
            case TODO -> new Todo(taskData[2]);
            case DEADLINE -> {
                ParsedDateTime deadline = parseStoredDateTime(taskData[3]);
                yield new Deadline(taskData[2], deadline.value, deadline.hasTime);
            }
            case EVENT -> {
                ParsedDateTime from = parseStoredDateTime(taskData[3]);
                ParsedDateTime to = parseStoredDateTime(taskData[4]);
                yield new Event(taskData[2], from.value, to.value,
                        from.hasTime, to.hasTime);
            }
        };

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
        private final boolean hasTime;

        private ParsedDateTime(LocalDateTime value, boolean hasTime) {
            this.value = value;
            this.hasTime = hasTime;
        }
    }
}
