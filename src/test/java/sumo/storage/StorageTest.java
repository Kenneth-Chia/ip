package sumo.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sumo.task.Deadline;
import sumo.task.Event;
import sumo.task.Task;
import sumo.task.Todo;
import sumo.ui.Ui;

/** Tests durable task saving and tolerant loading in {@link Storage}. */
public class StorageTest {
    @TempDir
    private Path temporaryDirectory;

    /** Verifies first-run loading and data-directory creation. */
    @Test
    public void load_fileDoesNotExist_emptyListReturnedAndDirectoryCreated() throws IOException {
        Path dataFile = temporaryDirectory.resolve("nested").resolve("sumo.txt");
        Storage storage = new Storage(dataFile);

        List<Task> tasks = storage.load(new RecordingUi());

        assertTrue(tasks.isEmpty());
        assertTrue(Files.isDirectory(dataFile.getParent()));
        assertFalse(Files.exists(dataFile));
    }

    /** Verifies that every task type and status survives a save-load round trip. */
    @Test
    public void saveAndLoad_allTaskTypesAndStatuses_roundTripPreserved() throws IOException {
        Path dataFile = temporaryDirectory.resolve("data").resolve("sumo.txt");
        Storage storage = new Storage(dataFile);
        Todo todo = new Todo("read");
        todo.markAsDone();
        Deadline deadline = new Deadline("submit", LocalDate.of(2026, 2, 3));
        Event event = new Event("camp",
                LocalDateTime.of(2026, 2, 3, 9, 0),
                LocalDateTime.of(2026, 2, 3, 17, 30));

        storage.save(List.of(todo, deadline, event));
        List<Task> loaded = storage.load(new RecordingUi());

        assertEquals(3, loaded.size());
        assertInstanceOf(Todo.class, loaded.get(0));
        assertInstanceOf(Deadline.class, loaded.get(1));
        assertInstanceOf(Event.class, loaded.get(2));
        assertEquals(todo.toDataString(), loaded.get(0).toDataString());
        assertEquals(deadline.toDataString(), loaded.get(1).toDataString());
        assertEquals(event.toDataString(), loaded.get(2).toDataString());
        assertTrue(loaded.get(0).isDone());
    }

    /** Verifies that saving replaces obsolete file contents. */
    @Test
    public void save_existingFileReplacedWithCurrentTasks() throws IOException {
        Path dataFile = temporaryDirectory.resolve("sumo.txt");
        Storage storage = new Storage(dataFile);
        storage.save(List.of(new Todo("old")));

        storage.save(List.of(new Todo("new")));

        assertEquals(List.of("T | 0 | new"),
                Files.readAllLines(dataFile, StandardCharsets.UTF_8));
    }

    /** Verifies that invalid records are reported without hiding valid records. */
    @Test
    public void load_blankAndInvalidRecords_validRecordsLoadedAndErrorsReported() throws IOException {
        Path dataFile = temporaryDirectory.resolve("sumo.txt");
        Files.write(dataFile, List.of(
                "T | 0 | first",
                "",
                "X | 0 | unknown",
                "D | done | report | 2026-02-03",
                "D | 0 | report | invalid-date",
                "E | 1 | camp | 2026-02-03 | 2026-02-04"), StandardCharsets.UTF_8);
        RecordingUi ui = new RecordingUi();

        List<Task> loaded = new Storage(dataFile).load(ui);

        assertEquals(List.of("first", "camp"),
                loaded.stream().map(Task::getDescription).toList());
        assertTrue(loaded.get(1).isDone());
        assertEquals(List.of(3, 4, 5), ui.invalidLineNumbers);
    }

    /** Records invalid line numbers without coupling storage tests to console output. */
    private static class RecordingUi extends Ui {
        private final List<Integer> invalidLineNumbers = new ArrayList<>();

        /** Records the line number of an invalid stored task. */
        @Override
        public void showInvalidTaskError(int lineNumber, String message) {
            invalidLineNumbers.add(lineNumber);
        }
    }
}
