import java.io.IOException;

/** Represents one user instruction that can be executed by Sumo. */
public abstract class Command {
    /** Carries out this command using the application's collaborators. */
    public abstract void execute(TaskList tasks, Ui ui, Storage storage) throws IOException;

    /** @return whether this command should end the application */
    public boolean isExit() {
        return false;
    }
}
