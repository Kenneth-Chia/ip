package sumo.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Tests the collection operations and date filtering performed by {@link TaskList}. */
public class TaskListTest {
    @Test
    public void constructor_sourceListChanged_taskListUnaffected() {
        List<Task> source = new java.util.ArrayList<>();
        source.add(new Todo("first"));
        TaskList taskList = new TaskList(source);

        source.add(new Todo("second"));

        assertEquals(1, taskList.size());
    }

    @Test
    public void getTasks_returnedSnapshotCannotBeModified() {
        TaskList taskList = new TaskList(List.of(new Todo("first")));

        List<Task> snapshot = taskList.getTasks();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new Todo("second")));
        assertEquals(1, taskList.size());
    }

    @Test
    public void addDeleteAndInsert_tasksRemainInExpectedOrder() {
        Todo first = new Todo("first");
        Todo second = new Todo("second");
        TaskList taskList = new TaskList();
        taskList.add(first);
        taskList.add(second);

        Task removed = taskList.delete(0);
        taskList.insert(0, removed);

        assertSame(first, taskList.get(0));
        assertSame(second, taskList.get(1));
    }

    @Test
    public void setDone_trueThenFalse_completionStateUpdated() {
        Todo todo = new Todo("read");
        TaskList taskList = new TaskList(List.of(todo));

        taskList.setDone(0, true);
        assertTrue(todo.isDone());

        taskList.setDone(0, false);
        assertFalse(todo.isDone());
    }

    @Test
    public void findOn_deadlineOnRequestedDate_deadlineReturned() {
        Deadline matching = new Deadline("submit", LocalDate.of(2026, 2, 3));
        Deadline other = new Deadline("renew", LocalDate.of(2026, 2, 4));
        TaskList taskList = new TaskList(List.of(matching, other));

        assertEquals(List.of(matching), taskList.findOn(LocalDate.of(2026, 2, 3)));
    }

    @Test
    public void findOn_eventRangeBoundaries_eventsReturnedOnBothBoundaries() {
        Event event = new Event("camp", LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 5));
        TaskList taskList = new TaskList(List.of(event));

        assertEquals(List.of(event), taskList.findOn(LocalDate.of(2026, 2, 3)));
        assertEquals(List.of(event), taskList.findOn(LocalDate.of(2026, 2, 5)));
    }

    @Test
    public void findOn_outsideEventRangeAndTodo_noTasksReturned() {
        Event event = new Event("camp", LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 5));
        Todo todo = new Todo("pack");
        TaskList taskList = new TaskList(List.of(event, todo));

        assertTrue(taskList.findOn(LocalDate.of(2026, 2, 2)).isEmpty());
        assertTrue(taskList.findOn(LocalDate.of(2026, 2, 6)).isEmpty());
    }

    @Test
    public void findOn_multipleMatches_originalOrderPreserved() {
        Event first = new Event("camp", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 5));
        Deadline second = new Deadline("submit", LocalDate.of(2026, 2, 3));
        TaskList taskList = new TaskList(List.of(first, new Todo("pack"), second));

        assertEquals(List.of(first, second), taskList.findOn(LocalDate.of(2026, 2, 3)));
    }
}
