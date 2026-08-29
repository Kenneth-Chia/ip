package sumo;

import java.io.IOException;
import java.nio.file.Path;

import sumo.command.Command;
import sumo.exception.SumoException;
import sumo.parser.Parser;
import sumo.storage.Storage;
import sumo.task.TaskList;
import sumo.ui.Ui;

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
        boolean isExit = false;
        while (!isExit && ui.hasNextCommand()) {
            String input = ui.readCommand();
            ui.showSeparator();
            try {
                Command command = parser.parse(input, tasks.size());
                command.execute(tasks, ui, storage);
                isExit = command.isExit();
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

    private String getErrorMessage(IOException exception) {
        return exception.getMessage() == null
                ? "The data file could not be accessed."
                : exception.getMessage();
    }
}
