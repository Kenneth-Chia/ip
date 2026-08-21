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
        String[] tasks = new String[100];
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
                for (int i = 0; i < taskCount; i++) {
                    System.out.println(" " + (i + 1) + ". " + tasks[i]);
                }
            } else {
                tasks[taskCount] = command;
                taskCount++;
                System.out.println(" added: " + command);
            }

            System.out.println(separator);
        }
    }
}
