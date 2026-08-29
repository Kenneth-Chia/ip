import java.io.IOException;

/** Ends the current Sumo session. */
public class ExitCommand extends Command {
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws IOException {
        ui.showGoodbye();
    }

    @Override
    public boolean isExit() {
        return true;
    }
}
