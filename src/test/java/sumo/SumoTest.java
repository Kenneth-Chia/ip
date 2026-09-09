package sumo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                + "Try todo, deadline, event, list, find, on, mark, unmark, or delete.", response);
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
