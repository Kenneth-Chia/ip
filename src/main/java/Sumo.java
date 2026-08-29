import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Coordinates Sumo's user interface, parsing, task operations, and storage. */
public class Sumo {
    public static void main(String[] args) {
        Ui ui = new Ui();
        Storage storage = new Storage(Path.of("data", "sumo.txt"));
        Parser parser = new Parser();
        List<Task> tasks;
        try {
            tasks = storage.load(ui);
        } catch (IOException exception) {
            tasks = new ArrayList<>();
            ui.showLoadingError(getErrorMessage(exception));
        }

        ui.showWelcome();
        while (ui.hasNextCommand()) {
            String input = ui.readCommand();
            ui.showSeparator();
            try {
                Parser.ParsedCommand command = parser.parse(input, tasks.size());
                if (command.getType() == Parser.CommandType.BYE) {
                    ui.showGoodbye();
                    break;
                }
                handleCommand(command, tasks, ui, storage);
            } catch (SumoException exception) {
                ui.showCommandError(exception.getMessage());
            } catch (IOException exception) {
                ui.showSavingError(getErrorMessage(exception));
            }
            ui.showSeparator();
        }
    }

    /** Carries out one already-parsed, non-exit command. */
    private static void handleCommand(Parser.ParsedCommand command, List<Task> tasks,
            Ui ui, Storage storage) throws IOException {
        switch (command.getType()) {
        case LIST:
            ui.showTaskList(tasks);
            break;
        case ON:
            printTasksOnDate(command.getDate(), tasks, ui);
            break;
        case MARK:
            updateTaskStatus(command.getTaskIndex(), true, tasks, ui, storage);
            break;
        case UNMARK:
            updateTaskStatus(command.getTaskIndex(), false, tasks, ui, storage);
            break;
        case DELETE:
            deleteTask(command.getTaskIndex(), tasks, ui, storage);
            break;
        case ADD:
            addTask(command.getTask(), tasks, ui, storage);
            break;
        default:
            throw new IllegalStateException("Exit commands are handled by the main loop.");
        }
    }

    /** Updates one task and restores its old state if saving fails. */
    private static void updateTaskStatus(int taskIndex, boolean markDone, List<Task> tasks,
            Ui ui, Storage storage) throws IOException {
        Task task = tasks.get(taskIndex);
        boolean wasDone = task.isDone;
        if (markDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }
        try {
            storage.save(tasks);
        } catch (IOException exception) {
            task.isDone = wasDone;
            throw exception;
        }
        if (markDone) {
            ui.showTaskMarked(task);
        } else {
            ui.showTaskUnmarked(task);
        }
    }

    /** Deletes one task and restores it if saving fails. */
    private static void deleteTask(int taskIndex, List<Task> tasks, Ui ui, Storage storage)
            throws IOException {
        Task removedTask = tasks.remove(taskIndex);
        try {
            storage.save(tasks);
        } catch (IOException exception) {
            tasks.add(taskIndex, removedTask);
            throw exception;
        }
        ui.showTaskDeleted(removedTask, tasks.size());
    }

    /** Adds one task and removes it again if saving fails. */
    private static void addTask(Task task, List<Task> tasks, Ui ui, Storage storage)
            throws IOException {
        tasks.add(task);
        try {
            storage.save(tasks);
        } catch (IOException exception) {
            tasks.remove(tasks.size() - 1);
            throw exception;
        }
        ui.showTaskAdded(task, tasks.size());
    }

    /** Finds deadlines and events that occur on the requested date. */
    private static void printTasksOnDate(LocalDate date, List<Task> tasks, Ui ui) {
        List<Task> matchingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (occursOn(task, date)) {
                matchingTasks.add(task);
            }
        }
        ui.showTasksOnDate(date.atStartOfDay(), matchingTasks);
    }

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

    private static String getErrorMessage(IOException exception) {
        return exception.getMessage() == null
                ? "The data file could not be accessed."
                : exception.getMessage();
    }
}
