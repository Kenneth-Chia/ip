# Sumo project template

This is a project template for a greenfield Java project. The chatbot is named _Sumo_. Given below are instructions on how to use it.

## Setting up in Intellij

Prerequisites: JDK 25, update Intellij to the most recent version.

1. Open Intellij (if you are not in the welcome screen, click `File` > `Close Project` to close the existing project first)
1. Open the project into Intellij as follows:
   1. Click `Open`.
   1. Select the project directory, and click `OK`.
   1. If there are any further prompts, accept the defaults.
1. Configure the project to use **JDK 25** (not other versions) as explained in [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).<br>
   In the same dialog, set the **Project language level** field to the `SDK default` option.
1. After that, run `./gradlew run` (macOS/Linux) or `.\gradlew.bat run` (Windows) from the
   project root. Alternatively, run `sumo.Launcher.main()` from IntelliJ. The application opens a
   simple chat window with a command field and a **Send** button.

   Add your own avatar images at the following paths. The GUI also works while either image is
   missing.

   - `src/main/resources/images/DaUser.png`
   - `src/main/resources/images/DaSumo.png`

   Enter the same commands supported by the console version, such as `todo read book`, `list`,
   `mark 1`, and `bye`. Press **Enter** or click **Send** to submit a command.

   The original console interface remains available by running `sumo.Sumo.main()`. If the setup is
   correct, its output should look like the following:
   ```
   ____________________________________________________________
     ██████  ██    ██ ███    ███  ██████
    ██       ██    ██ ████  ████ ██    ██
     █████   ██    ██ ██ ████ ██ ██    ██
         ██  ██    ██ ██  ██  ██ ██    ██
    ██████    ██████  ██      ██  ██████
   Hello! I'm Sumo.
   What can I do for you?
   ____________________________________________________________

   read book
   ____________________________________________________________
    added: read book
   ____________________________________________________________

   return book
   ____________________________________________________________
    added: return book
   ____________________________________________________________

   buy bread
   ____________________________________________________________
    added: buy bread
   ____________________________________________________________

   list
   ____________________________________________________________
    Here are the tasks in your list:
    1.[ ] read book
    2.[ ] return book
    3.[ ] buy bread
   ____________________________________________________________

   mark 2
   ____________________________________________________________
    Nice! I've marked this task as done:
      [X] return book
   ____________________________________________________________

   list
   ____________________________________________________________
   Here are the tasks in your list:
    1.[ ] read book
    2.[X] return book
    3.[ ] buy bread
   ____________________________________________________________

   unmark 2
   ____________________________________________________________
    OK, I've marked this task as not done yet:
      [ ] return book
   ____________________________________________________________

   list
   ____________________________________________________________
    Here are the tasks in your list:
    1.[ ] read book
    2.[ ] return book
    3.[ ] buy bread
   ____________________________________________________________

   bye
   ____________________________________________________________
   Bye. Hope to see you again soon!
   ____________________________________________________________
   ```

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.

## Checking the Java coding standard

This project uses Checkstyle to automate part of the SE-EDU Java coding standard. Run the
following command from the repository root before submitting changes:

```powershell
.\gradlew.bat checkstyleMain checkstyleTest
```

The Checkstyle configuration is in `config/checkstyle/checkstyle.xml`, with test-specific
exceptions in `config/checkstyle/suppressions.xml`. To get feedback while coding in IntelliJ,
install the Checkstyle-IDEA plugin and import the local `config/checkstyle/checkstyle.xml` file
using Checkstyle version 10.24.0.
