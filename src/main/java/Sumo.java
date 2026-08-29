import java.io.IOException;
import java.nio.file.Path;

/** Coordinates Sumo's user interface, parsing, task operations, and storage. */
public class Sumo {
    private final Ui ui;
    private final Storage storage;
    private final Parser parser;
    private final TaskList tasks;

    /**
     * Creates a Sumo application backed by the given task file.
     *
     * @param filePath path used to load and save tasks
     */
    public Sumo(String filePath) {
        this.ui = new Ui();
        this.storage = new Storage(Path.of(filePath));
        this.parser = new Parser();
        TaskList loadedTasks;
        try {
            loadedTasks = new TaskList(storage.load(ui));
        } catch (IOException exception) {
            loadedTasks = new TaskList();
            ui.showLoadingError(getErrorMessage(exception));
        }
        this.tasks = loadedTasks;
    }

    /** Starts the command-reading loop. */
    public void run() {
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
                handleCommand(command);
            } catch (SumoException exception) {
                ui.showCommandError(exception.getMessage());
            } catch (IOException exception) {
                ui.showSavingError(getErrorMessage(exception));
            }
            ui.showSeparator();
        }
    }

    /** Starts Sumo using its default task file. */
    public static void main(String[] args) {
        new Sumo(Path.of("data", "sumo.txt").toString()).run();
    }

    /** Carries out one already-parsed, non-exit command. */
    private void handleCommand(Parser.ParsedCommand command) throws IOException {
        switch (command.getType()) {
        case LIST:
            ui.showTaskList(tasks.getTasks());
            break;
        case ON:
            ui.showTasksOnDate(command.getDate().atStartOfDay(), tasks.findOn(command.getDate()));
            break;
        case MARK:
            updateTaskStatus(command.getTaskIndex(), true);
            break;
        case UNMARK:
            updateTaskStatus(command.getTaskIndex(), false);
            break;
        case DELETE:
            deleteTask(command.getTaskIndex());
            break;
        case ADD:
            addTask(command.getTask());
            break;
        default:
            throw new IllegalStateException("Exit commands are handled by the main loop.");
        }
    }

    /** Updates one task and restores its old state if saving fails. */
    private void updateTaskStatus(int taskIndex, boolean markDone) throws IOException {
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
    private void deleteTask(int taskIndex) throws IOException {
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
    private void addTask(Task task) throws IOException {
        tasks.add(task);
        try {
            storage.save(tasks.getTasks());
        } catch (IOException exception) {
            tasks.delete(tasks.size() - 1);
            throw exception;
        }
        ui.showTaskAdded(task, tasks.size());
    }

    private String getErrorMessage(IOException exception) {
        return exception.getMessage() == null
                ? "The data file could not be accessed."
                : exception.getMessage();
    }
}
