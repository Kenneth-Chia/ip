import java.io.IOException;
import java.time.LocalDate;

/** Displays tasks that occur on a particular date. */
public class OnCommand extends Command {
    private final LocalDate date;

    /** Creates a date-filtering command. */
    public OnCommand(LocalDate date) {
        this.date = date;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws IOException {
        ui.showTasksOnDate(date.atStartOfDay(), tasks.findOn(date));
    }
}
