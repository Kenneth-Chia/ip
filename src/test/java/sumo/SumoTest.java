package sumo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SumoTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void getResponse_addAndListCommands_returnsExistingResponses() {
        Sumo sumo = createSumo();

        String addResponse = sumo.getResponse("todo read book");
        String listResponse = sumo.getResponse("list");

        assertEquals("Got it. I've added this task:" + System.lineSeparator()
                + "   [T][ ] read book" + System.lineSeparator()
                + " Now you have 1 tasks in the list.", addResponse);
        assertEquals("Here are the tasks in your list:" + System.lineSeparator()
                + " 1.[T][ ] read book", listResponse);
    }

    @Test
    void getResponse_invalidCommand_returnsExistingErrorResponse() {
        Sumo sumo = createSumo();

        String response = sumo.getResponse("blah");

        assertEquals("I could not complete that command: I do not recognise that command. "
                + "Try todo, deadline, event, list, sort, find, on, mark, unmark, or delete.", response);
        assertEquals(response, sumo.getResponse("sort descending"));
    }

    @Test
    void getResponse_sortCommand_displaysTemporarySortedView() throws Exception {
        Sumo sumo = createSumo();
        Path dataFile = temporaryDirectory.resolve("sumo.txt");

        sumo.getResponse("todo buy groceries");
        sumo.getResponse("deadline submit report /by 2026-02-09");
        sumo.getResponse("event meeting /from 2026-02-03 /to 2026-02-10");
        sumo.getResponse("mark 2");
        String storedTasksBeforeSort = Files.readString(dataFile);

        assertEquals("Here are your tasks sorted chronologically (ascending):" + System.lineSeparator()
                + " Incomplete tasks:" + System.lineSeparator()
                + " 1.[E][ ] meeting (from: Feb 03 2026 to: Feb 10 2026)" + System.lineSeparator()
                + " 2.[T][ ] buy groceries" + System.lineSeparator()
                + " Completed tasks:" + System.lineSeparator()
                + " 3.[D][X] submit report (by: Feb 09 2026)" + System.lineSeparator()
                + " Sorted view only; the normal task order is unchanged.", sumo.getResponse("sort"));
        assertEquals(storedTasksBeforeSort, Files.readString(dataFile));
        assertEquals("Here are the tasks in your list:" + System.lineSeparator()
                + " 1.[T][ ] buy groceries" + System.lineSeparator()
                + " 2.[D][X] submit report (by: Feb 09 2026)" + System.lineSeparator()
                + " 3.[E][ ] meeting (from: Feb 03 2026 to: Feb 10 2026)", sumo.getResponse("list"));
    }

    @Test
    void getResponse_sort_emptyState() {
        Sumo sumo = createSumo();

        assertEquals("No tasks to be sorted.", sumo.getResponse("sort"));
    }

    @Test
    void getResponse_exitCommand_returnsGoodbyeAndEndsSession() {
        Sumo sumo = createSumo();

        assertFalse(sumo.isExit());
        assertEquals("Bye. Hope to see you again soon!", sumo.getResponse("bye"));
        assertTrue(sumo.isExit());
    }

    private Sumo createSumo() {
        return new Sumo(temporaryDirectory.resolve("sumo.txt").toString());
    }
}
