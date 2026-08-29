package sumo.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import sumo.command.ExitCommand;
import sumo.command.ListCommand;
import sumo.command.OnCommand;
import sumo.exception.SumoException;
import sumo.parser.Parser.CommandType;
import sumo.parser.Parser.ParsedCommand;
import sumo.task.Deadline;
import sumo.task.Event;
import sumo.task.Todo;

/** Tests command recognition, validation, and task construction in {@link Parser}. */
public class ParserTest {
    private final Parser parser = new Parser();

    @Test
    public void parse_commandsWithoutArguments_correctCommandTypesReturned() throws SumoException {
        assertInstanceOf(ExitCommand.class, parser.parse("bye", 0));
        assertInstanceOf(ListCommand.class, parser.parse("list", 0));
    }

    @Test
    public void parse_todoCommand_addCommandContainsTodo() throws SumoException {
        ParsedCommand command = assertInstanceOf(ParsedCommand.class,
                parser.parse("todo read a book", 0));

        assertEquals(CommandType.ADD, command.getType());
        assertEquals(-1, command.getTaskIndex());
        Todo todo = assertInstanceOf(Todo.class, command.getTask());
        assertEquals("read a book", todo.getDescription());
    }

    @Test
    public void parse_deadlineWithDate_dateOnlyDeadlineReturned() throws SumoException {
        ParsedCommand command = assertInstanceOf(ParsedCommand.class,
                parser.parse("deadline submit report /by 3/2/2026", 0));

        Deadline deadline = assertInstanceOf(Deadline.class, command.getTask());
        assertEquals(LocalDateTime.of(2026, 2, 3, 0, 0), deadline.getBy());
        assertEquals("D | 0 | submit report | 2026-02-03", deadline.toDataString());
    }

    @Test
    public void parse_deadlineWithTime_dateTimeDeadlineReturned() throws SumoException {
        ParsedCommand command = assertInstanceOf(ParsedCommand.class,
                parser.parse("deadline submit report /by 2026-02-03 0915", 0));

        Deadline deadline = assertInstanceOf(Deadline.class, command.getTask());
        assertEquals(LocalDateTime.of(2026, 2, 3, 9, 15), deadline.getBy());
        assertEquals("D | 0 | submit report | 2026-02-03T09:15", deadline.toDataString());
    }

    @Test
    public void parse_eventWithMixedDateTimes_eventRetainsEachInputFormat() throws SumoException {
        ParsedCommand command = assertInstanceOf(ParsedCommand.class,
                parser.parse("event camp /from 2026-02-03 /to 4/2/2026 1730", 0));

        Event event = assertInstanceOf(Event.class, command.getTask());
        assertEquals(LocalDateTime.of(2026, 2, 3, 0, 0), event.getFrom());
        assertEquals(LocalDateTime.of(2026, 2, 4, 17, 30), event.getTo());
        assertEquals("E | 0 | camp | 2026-02-03 | 2026-02-04T17:30", event.toDataString());
    }

    @Test
    public void parse_indexedCommands_validTaskNumberConvertedToZeroBasedIndex() throws SumoException {
        assertIndexedCommand("mark 2", CommandType.MARK);
        assertIndexedCommand("unmark 2", CommandType.UNMARK);
        assertIndexedCommand("delete 2", CommandType.DELETE);
    }

    @Test
    public void parse_onWithSupportedDateFormats_onCommandsReturned() throws SumoException {
        assertInstanceOf(OnCommand.class, parser.parse("on 2026-02-03", 0));
        assertInstanceOf(OnCommand.class, parser.parse("on 3/2/2026", 0));
    }

    @Test
    public void parse_blankRequiredArguments_exceptionThrown() {
        assertThrows(SumoException.class, () -> parser.parse("todo", 0));
        assertThrows(SumoException.class, () -> parser.parse("deadline /by 2026-02-03", 0));
        assertThrows(SumoException.class,
                () -> parser.parse("event camp /from 2026-02-03 /to", 0));
        assertThrows(SumoException.class, () -> parser.parse("mark", 1));
        assertThrows(SumoException.class, () -> parser.parse("on", 0));
    }

    @Test
    public void parse_invalidDatesAndTimes_exceptionThrown() {
        assertThrows(SumoException.class,
                () -> parser.parse("deadline report /by 2025-02-29", 0));
        assertThrows(SumoException.class,
                () -> parser.parse("deadline report /by 2026-02-03 2400", 0));
        assertThrows(SumoException.class,
                () -> parser.parse("event camp /from tomorrow /to 2026-02-03", 0));
    }

    @Test
    public void parse_invalidTaskNumbers_exceptionThrown() {
        assertThrows(SumoException.class, () -> parser.parse("mark zero", 2));
        assertThrows(SumoException.class, () -> parser.parse("mark 0", 2));
        assertThrows(SumoException.class, () -> parser.parse("mark 3", 2));
    }

    @Test
    public void parse_persistenceDelimiterInTaskData_exceptionThrown() {
        assertThrows(SumoException.class, () -> parser.parse("todo first | second", 0));
        assertThrows(SumoException.class,
                () -> parser.parse("deadline report /by 2026-02-03 | extra", 0));
    }

    @Test
    public void parse_unknownCommand_exceptionThrown() {
        assertThrows(SumoException.class, () -> parser.parse("remind me", 0));
    }

    private void assertIndexedCommand(String input, CommandType expectedType) throws SumoException {
        ParsedCommand command = assertInstanceOf(ParsedCommand.class, parser.parse(input, 3));
        assertEquals(expectedType, command.getType());
        assertEquals(1, command.getTaskIndex());
    }
}
