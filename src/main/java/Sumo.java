import java.util.Scanner;

/**
 * Starts Sumo and stores task text entered by the user.
 */
public class Sumo {
    public static void main(String[] args) {
        String separator = "____________________________________________________________";
        String banner = " ██████  ██    ██ ███    ███  ██████\n"
                + "██       ██    ██ ████  ████ ██    ██\n"
                + " █████   ██    ██ ██ ████ ██ ██    ██\n"
                + "     ██  ██    ██ ██  ██  ██ ██    ██\n"
                + "██████    ██████  ██      ██  ██████";
        Task[] tasks = new Task[100];
        int taskCount = 0;

        System.out.println(separator);
        System.out.println(banner);
        System.out.println("Hello! I'm Sumo.");
        System.out.println("What can I do for you?");
        System.out.println(separator);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine();
            System.out.println(separator);

            if (command.equals("bye")) {
                System.out.println("Bye. Hope to see you again soon!");
                System.out.println(separator);
                break;
            }

            if (command.equals("list")) {
                System.out.println(" Here are the tasks in your list:");
                for (int i = 0; i < taskCount; i++) {
                    System.out.println(" " + (i + 1) + "." + tasks[i]);
                }
            } else if (command.startsWith("mark ")) {
                int taskNumber = Integer.parseInt(command.substring(5));
                int taskIndex = taskNumber - 1;
                tasks[taskIndex].markAsDone();
                System.out.println(" Nice! I've marked this task as done:");
                System.out.println("   " + tasks[taskIndex]);
            } else if (command.startsWith("unmark ")) {
                int taskNumber = Integer.parseInt(command.substring(7));
                int taskIndex = taskNumber - 1;
                tasks[taskIndex].markAsNotDone();
                System.out.println(" OK, I've marked this task as not done yet:");
                System.out.println("   " + tasks[taskIndex]);
            } else if (command.startsWith("todo ")) {
                String description = command.substring(5);
                tasks[taskCount] = new Todo(description);
                taskCount++;
                System.out.println(" Got it. I've added this task:");
                System.out.println("   " + tasks[taskCount - 1]);
                System.out.println(" Now you have " + taskCount + " tasks in the list.");
            } else if (command.startsWith("deadline ")) {
                String taskText = command.substring(9);
                String marker = " /by ";
                int byIndex = taskText.indexOf(marker);
                if (byIndex >= 0) {
                    String description = taskText.substring(0, byIndex);
                    String by = taskText.substring(byIndex + marker.length());
                    tasks[taskCount] = new Deadline(description, by);
                    taskCount++;
                    System.out.println(" Got it. I've added this task:");
                    System.out.println("   " + tasks[taskCount - 1]);
                    System.out.println(" Now you have " + taskCount + " tasks in the list.");
                } else {
                    tasks[taskCount] = new Task(command);
                    taskCount++;
                    System.out.println(" added: " + command);
                }
            } else if (command.startsWith("event ")) {
                String taskText = command.substring(6);
                String fromMarker = " /from ";
                String toMarker = " /to ";
                int fromIndex = taskText.indexOf(fromMarker);
                int toIndex = taskText.indexOf(toMarker, fromIndex + fromMarker.length());
                if (fromIndex >= 0 && toIndex >= 0) {
                    String description = taskText.substring(0, fromIndex);
                    String from = taskText.substring(fromIndex + fromMarker.length(), toIndex);
                    String to = taskText.substring(toIndex + toMarker.length());
                    tasks[taskCount] = new Event(description, from, to);
                    taskCount++;
                    System.out.println(" Got it. I've added this task:");
                    System.out.println("   " + tasks[taskCount - 1]);
                    System.out.println(" Now you have " + taskCount + " tasks in the list.");
                } else {
                    tasks[taskCount] = new Task(command);
                    taskCount++;
                    System.out.println(" added: " + command);
                }
            } else {
                tasks[taskCount] = new Task(command);
                taskCount++;
                System.out.println(" added: " + command);
            }

            System.out.println(separator);
        }
    }
}
