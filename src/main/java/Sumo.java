import java.io.IOException;
import java.nio.file.Path;

/** Coordinates Sumo's user interface, parsing, task operations, and storage. */
public class Sumo {
    public static void main(String[] args) {
        Ui ui = new Ui();
        Storage storage = new Storage(Path.of("data", "sumo.txt"));
        Parser parser = new Parser();
        TaskList tasks;
        try {
            tasks = new TaskList(storage.load(ui));
        } catch (IOException exception) {
            tasks = new TaskList();
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
    private static void handleCommand(Parser.ParsedCommand command, TaskList tasks,
            Ui ui, Storage storage) throws IOException {
        switch (command.getType()) {
        case LIST:
            ui.showTaskList(tasks.getTasks());
            break;
        case ON:
            ui.showTasksOnDate(command.getDate().atStartOfDay(), tasks.findOn(command.getDate()));
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
    private static void updateTaskStatus(int taskIndex, boolean markDone, TaskList tasks,
            Ui ui, Storage storage) throws IOException {
        Task task = tasks.get(taskIndex);
        boolean wasDone = task.isDone;
        tasks.setDone(taskIndex, markDone);
        try {
            storage.save(tasks.getTasks());
        } catch (IOException exception) {
            tasks.setDone(taskIndex, wasDone);
            throw exception;
        }
        if (markDone) {
            ui.showTaskMarked(task);
        } else {
            ui.showTaskUnmarked(task);
        }
    }

    /** Deletes one task and restores it if saving fails. */
    private static void deleteTask(int taskIndex, TaskList tasks, Ui ui, Storage storage)
            throws IOException {
        Task removedTask = tasks.delete(taskIndex);
        try {
            storage.save(tasks.getTasks());
        } catch (IOException exception) {
            tasks.insert(taskIndex, removedTask);
            throw exception;
        }
        ui.showTaskDeleted(removedTask, tasks.size());
    }

    /** Adds one task and removes it again if saving fails. */
    private static void addTask(Task task, TaskList tasks, Ui ui, Storage storage)
            throws IOException {
        tasks.add(task);
        try {
            storage.save(tasks.getTasks());
        } catch (IOException exception) {
            tasks.delete(tasks.size() - 1);
            throw exception;
        }
        ui.showTaskAdded(task, tasks.size());
    }

    private static String getErrorMessage(IOException exception) {
        return exception.getMessage() == null
                ? "The data file could not be accessed."
                : exception.getMessage();
    }
}
