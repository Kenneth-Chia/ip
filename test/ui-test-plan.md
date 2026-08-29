# Console UI test plan

This file is the source of truth for the `test-ui` skill. Keep test cases deterministic and update expected output only when the intended UI behavior changes.

## Execution information

- Working directory: repository root
- Java version: 25
- Setup/compile command: `javac -d out src/main/java/DateTimeDisplay.java src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/SumoException.java src/main/java/Task.java src/main/java/Todo.java`
- Program launch command: `java -cp out Sumo`
- Output comparison: exact, after normalizing Windows `CRLF` line endings to `LF`; each expected block contains only the response produced after its listed input
- Test isolation: before each test case, run `Remove-Item -LiteralPath data/sumo.txt -ErrorAction SilentlyContinue`, then launch a fresh program process unless the case explicitly requires multiple continuous sessions

## Test cases

### UI-001 — Add and list a todo

- Aim: Verify that a todo command creates a typed task and that `list` displays it.
- Inputs, commands, and expected output:

  1. Command/input: `todo read book`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [T][ ] read book
      Now you have 1 tasks in the list.
     ____________________________________________________________
     ```

  2. Command/input: `list`

     Expected output:

     ```text
     ____________________________________________________________
      Here are the tasks in your list:
      1.[T][ ] read book
     ____________________________________________________________
     ```

  3. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

### UI-002 — Mark and unmark a task

- Aim: Verify that a task changes status when marked done and returns to incomplete when unmarked.
- Inputs, commands, and expected output:

  1. Command/input: `todo return book`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [T][ ] return book
      Now you have 1 tasks in the list.
     ____________________________________________________________
     ```

  2. Command/input: `mark 1`

     Expected output:

     ```text
     ____________________________________________________________
      Nice! I've marked this task as done:
        [T][X] return book
     ____________________________________________________________
     ```

  3. Command/input: `unmark 1`

     Expected output:

     ```text
     ____________________________________________________________
      OK, I've marked this task as not done yet:
        [T][ ] return book
     ____________________________________________________________
     ```

  4. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

- Notes: Run all four inputs in one continuous process so the task state is preserved.

### UI-003 — Explain invalid commands

- Aim: Verify that invalid input is handled through a user-friendly error message and does not end the program.
- Inputs, commands, and expected output:

  1. Command/input: `todo`

     Expected output:

     ```text
     ____________________________________________________________
      I could not complete that command: Please add a description after 'todo'.
     ____________________________________________________________
     ```

  2. Command/input: `blah`

     Expected output:

     ```text
     ____________________________________________________________
      I could not complete that command: I do not recognise that command. Try todo, deadline, event, list, mark, unmark, or delete.
     ____________________________________________________________
     ```

  3. Command/input: `deadline submit report`

     Expected output:

     ```text
     ____________________________________________________________
      I could not complete that command: Use: deadline <description> /by <date>.
     ____________________________________________________________
     ```

  4. Command/input: `event meeting /from Monday`

     Expected output:

     ```text
     ____________________________________________________________
      I could not complete that command: Use: event <description> /from <start> /to <end>.
     ____________________________________________________________
     ```

  5. Command/input: `mark one`

     Expected output:

     ```text
     ____________________________________________________________
      I could not complete that command: Task numbers must be whole numbers.
     ____________________________________________________________
     ```

  6. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

### UI-004 — Delete a task

- Aim: Verify that deleting a task removes it from the collection and renumbers the remaining tasks.
- Inputs, commands, and expected output:

  1. Command/input: `todo read book`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [T][ ] read book
      Now you have 1 tasks in the list.
     ____________________________________________________________
     ```

  2. Command/input: `deadline return book /by 2019-06-06`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [D][ ] return book (by: Jun 06 2019)
      Now you have 2 tasks in the list.
     ____________________________________________________________
     ```

  3. Command/input: `delete 1`

     Expected output:

     ```text
     ____________________________________________________________
      Noted. I've removed this task:
        [T][ ] read book
      Now you have 1 tasks in the list.
     ____________________________________________________________
     ```

  4. Command/input: `list`

     Expected output:

     ```text
     ____________________________________________________________
      Here are the tasks in your list:
      1.[D][ ] return book (by: Jun 06 2019)
     ____________________________________________________________
     ```

  5. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

- Notes: Run all five inputs in one continuous process so the task collection is preserved.

### UI-005 — Save every task-list change

- Aim: Verify that adding, marking, unmarking, and deleting tasks immediately rewrites `data/sumo.txt`.
- Inputs, commands, and expected output:

  1. Command/input: `todo read book`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [T][ ] read book
      Now you have 1 tasks in the list.
     ____________________________________________________________
     ```

     Expected `data/sumo.txt` content immediately after the command:

     ```text
     T | 0 | read book
     ```

  2. Command/input: `deadline return book /by 2019-06-06`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [D][ ] return book (by: Jun 06 2019)
      Now you have 2 tasks in the list.
     ____________________________________________________________
     ```

     Expected `data/sumo.txt` content immediately after the command:

     ```text
     T | 0 | read book
     D | 0 | return book | 2019-06-06
     ```

  3. Command/input: `event project meeting /from 2019-08-06 1400 /to 2019-08-06 1600`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [E][ ] project meeting (from: Aug 06 2019 2:00 PM to: Aug 06 2019 4:00 PM)
      Now you have 3 tasks in the list.
     ____________________________________________________________
     ```

     Expected `data/sumo.txt` content immediately after the command:

     ```text
     T | 0 | read book
     D | 0 | return book | 2019-06-06
     E | 0 | project meeting | 2019-08-06T14:00 | 2019-08-06T16:00
     ```

  4. Command/input: `mark 1`

     Expected output:

     ```text
     ____________________________________________________________
      Nice! I've marked this task as done:
        [T][X] read book
     ____________________________________________________________
     ```

     Expected `data/sumo.txt` content immediately after the command:

     ```text
     T | 1 | read book
     D | 0 | return book | 2019-06-06
     E | 0 | project meeting | 2019-08-06T14:00 | 2019-08-06T16:00
     ```

  5. Command/input: `unmark 1`

     Expected output:

     ```text
     ____________________________________________________________
      OK, I've marked this task as not done yet:
        [T][ ] read book
     ____________________________________________________________
     ```

     Expected `data/sumo.txt` content immediately after the command:

     ```text
     T | 0 | read book
     D | 0 | return book | 2019-06-06
     E | 0 | project meeting | 2019-08-06T14:00 | 2019-08-06T16:00
     ```

  6. Command/input: `delete 2`

     Expected output:

     ```text
     ____________________________________________________________
      Noted. I've removed this task:
        [D][ ] return book (by: Jun 06 2019)
      Now you have 2 tasks in the list.
     ____________________________________________________________
     ```

     Expected `data/sumo.txt` content immediately after the command:

     ```text
     T | 0 | read book
     E | 0 | project meeting | 2019-08-06T14:00 | 2019-08-06T16:00
     ```

  7. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

- Notes: Run all seven inputs in one continuous process. After each of the first six inputs, inspect the file before sending the next input.

### UI-006 — Load tasks on restart

- Aim: Verify that restarting Sumo reconstructs every task type, its details, and its completion status from `data/sumo.txt`.
- First session inputs, commands, and expected output:

  1. Command/input: `todo read book`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [T][ ] read book
      Now you have 1 tasks in the list.
     ____________________________________________________________
     ```

  2. Command/input: `deadline return book /by 2019-06-06`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [D][ ] return book (by: Jun 06 2019)
      Now you have 2 tasks in the list.
     ____________________________________________________________
     ```

  3. Command/input: `event project meeting /from 2019-08-06 1400 /to 2019-08-06 1600`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [E][ ] project meeting (from: Aug 06 2019 2:00 PM to: Aug 06 2019 4:00 PM)
      Now you have 3 tasks in the list.
     ____________________________________________________________
     ```

  4. Command/input: `mark 2`

     Expected output:

     ```text
     ____________________________________________________________
      Nice! I've marked this task as done:
        [D][X] return book (by: Jun 06 2019)
     ____________________________________________________________
     ```

  5. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

- Second session inputs, commands, and expected output:

  1. Command/input: `list`

     Expected output:

     ```text
     ____________________________________________________________
      Here are the tasks in your list:
      1.[T][ ] read book
      2.[D][X] return book (by: Jun 06 2019)
      3.[E][ ] project meeting (from: Aug 06 2019 2:00 PM to: Aug 06 2019 4:00 PM)
     ____________________________________________________________
     ```

  2. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

- Notes: End the first process after `bye`, then launch a second process without deleting or changing `data/sumo.txt` between the two sessions.

### UI-007 — Start without an existing data folder or file

- Aim: Verify that Sumo starts with an empty task list and creates the relative `data` folder when neither the folder nor `data/sumo.txt` exists.
- Setup: Before launching Sumo, run `Remove-Item -LiteralPath data -Recurse -Force -ErrorAction SilentlyContinue`.
- Inputs, commands, and expected output:

  1. Command/input: `list`

     Expected output:

     ```text
     ____________________________________________________________
      Here are the tasks in your list:
     ____________________________________________________________
     ```

  2. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

- Notes: Confirm that `data` exists after startup. The `sumo.txt` file is not required until a task-list change is saved.

### UI-008 — Parse and format dates and times

- Aim: Verify that day/month/year and ISO date inputs become typed values and are displayed in the requested output formats.
- Inputs, commands, and expected output:

  1. Command/input: `deadline return book /by 2/12/2019 1800`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [D][ ] return book (by: Dec 02 2019 6:00 PM)
      Now you have 1 tasks in the list.
     ____________________________________________________________
     ```

  2. Command/input: `deadline submit report /by 2019-10-15`

     Expected output:

     ```text
     ____________________________________________________________
      Got it. I've added this task:
        [D][ ] submit report (by: Oct 15 2019)
      Now you have 2 tasks in the list.
     ____________________________________________________________
     ```

  3. Command/input: `list`

     Expected output:

     ```text
     ____________________________________________________________
      Here are the tasks in your list:
      1.[D][ ] return book (by: Dec 02 2019 6:00 PM)
      2.[D][ ] submit report (by: Oct 15 2019)
     ____________________________________________________________
     ```

  4. Command/input: `bye`

     Expected output:

     ```text
     ____________________________________________________________
     Bye. Hope to see you again soon!
     ____________________________________________________________
     ```

- Notes: Run all four inputs in one continuous process so the task state is preserved.

## Latest test session

Leave the test cases and expected outputs above unchanged when recording a run. Add a dated session below with the actual console transcript, overall result, and—if applicable—the first failure’s actual and expected output.

### 2026-08-29 — PASS (typed date/time parsing and formatting)

Transcript:

```text
$ java -version
java version "25.0.4" 2026-07-21 LTS

$ javac -d out src/main/java/DateTimeDisplay.java src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/SumoException.java src/main/java/Task.java src/main/java/Todo.java

### UI-001 — Add and list a todo
> todo read book — exact expected response matched
> list — exact expected response matched
> bye — exact expected response matched

### UI-002 — Mark and unmark a task
> todo return book — exact expected response matched
> mark 1 — exact expected response matched
> unmark 1 — exact expected response matched
> bye — exact expected response matched

### UI-003 — Explain invalid commands
> all six inputs — exact expected responses matched

### UI-004 — Delete a task
> all five inputs — exact expected responses matched

### UI-005 — Save every task-list change
> all seven inputs — exact responses and immediate file contents matched

### UI-006 — Load tasks on restart
> First session — exact expected responses matched
> Second session — exact expected responses matched

### UI-007 — Start without an existing data folder or file
> list, bye — exact expected responses matched; data directory created

### UI-008 — Parse and format dates and times
> deadline return book /by 2/12/2019 1800 — exact expected response matched
> deadline submit report /by 2019-10-15 — exact expected response matched
> list, bye — exact expected responses matched
```

Result: All eight listed test cases passed under Java 25.0.4. UI-005 confirmed immediate persistence, UI-006 confirmed typed dates/times survive restart, and UI-008 confirmed both requested input styles and display formats.

### 2026-08-28 — PASS (relative-path and clean-start verification)

Transcript:

```text
$ java -version
java version "25.0.4" 2026-07-21 LTS

$ javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/SumoException.java src/main/java/Task.java src/main/java/Todo.java

### UI-001 — Add and list a todo
> todo read book — exact expected response matched
> list — exact expected response matched
> bye — exact expected response matched

### UI-002 — Mark and unmark a task
> todo return book — exact expected response matched
> mark 1 — exact expected response matched
> unmark 1 — exact expected response matched
> bye — exact expected response matched

### UI-003 — Explain invalid commands
> todo — exact expected response matched
> blah — exact expected response matched
> deadline submit report — exact expected response matched
> event meeting /from Monday — exact expected response matched
> mark one — exact expected response matched
> bye — exact expected response matched

### UI-004 — Delete a task
> todo read book — exact expected response matched
> deadline return book /by June 6th — exact expected response matched
> delete 1 — exact expected response matched
> list — exact expected response matched
> bye — exact expected response matched

### UI-005 — Save every task-list change
> todo read book — exact response and file content matched
> deadline return book /by June 6th — exact response and file content matched
> event project meeting /from Aug 6th 2pm /to 4pm — exact response and file content matched
> mark 1 — exact response and file content matched
> unmark 1 — exact response and file content matched
> delete 2 — exact response and file content matched
> bye — exact expected response matched

### UI-006 — Load tasks on restart
> First session: todo, deadline, event, mark 2, bye — all exact responses matched
> Second session: list, bye — all exact responses matched

### UI-007 — Start without an existing data folder or file
$ Remove-Item -LiteralPath data -Recurse -Force -ErrorAction SilentlyContinue
> list — exact expected response matched; data directory created
> bye — exact expected response matched
```

Result: All seven listed test cases passed under Java 25.0.4. UI-005 confirmed immediate persistence after each task-list mutation, and UI-007 confirmed startup succeeds when both the data folder and file are absent.

### 2026-08-21 — PASS

Transcript:

```text
$ javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/Task.java src/main/java/Todo.java

### UI-001 — Add and list a todo
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________

> list
____________________________________________________________
 Here are the tasks in your list:
 1.[T][ ] read book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-002 — Mark and unmark a task
> todo return book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] return book
 Now you have 1 tasks in the list.
____________________________________________________________

> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] return book
____________________________________________________________

> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] return book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

Result: All listed test cases passed under Java 25.0.4.

### 2026-08-27 — PASS (read-on-startup verification)

Transcript:

```text
$ java -version
java version "25.0.4" 2026-07-21 LTS

$ javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/SumoException.java src/main/java/Task.java src/main/java/Todo.java

### UI-001 — Add and list a todo
$ Remove-Item -LiteralPath data/sumo.txt -ErrorAction SilentlyContinue
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
> list
____________________________________________________________
 Here are the tasks in your list:
 1.[T][ ] read book
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-002 — Mark and unmark a task
$ Remove-Item -LiteralPath data/sumo.txt -ErrorAction SilentlyContinue
> todo return book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] return book
 Now you have 1 tasks in the list.
____________________________________________________________
> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] return book
____________________________________________________________
> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] return book
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-003 — Explain invalid commands
$ Remove-Item -LiteralPath data/sumo.txt -ErrorAction SilentlyContinue
> todo
____________________________________________________________
 I could not complete that command: Please add a description after 'todo'.
____________________________________________________________
> blah
____________________________________________________________
 I could not complete that command: I do not recognise that command. Try todo, deadline, event, list, mark, unmark, or delete.
____________________________________________________________
> deadline submit report
____________________________________________________________
 I could not complete that command: Use: deadline <description> /by <date>.
____________________________________________________________
> event meeting /from Monday
____________________________________________________________
 I could not complete that command: Use: event <description> /from <start> /to <end>.
____________________________________________________________
> mark one
____________________________________________________________
 I could not complete that command: Task numbers must be whole numbers.
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-004 — Delete a task
$ Remove-Item -LiteralPath data/sumo.txt -ErrorAction SilentlyContinue
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
> deadline return book /by June 6th
____________________________________________________________
 Got it. I've added this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________
> delete 1
____________________________________________________________
 Noted. I've removed this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
> list
____________________________________________________________
 Here are the tasks in your list:
 1.[D][ ] return book (by: June 6th)
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-005 — Save every task-list change
$ Remove-Item -LiteralPath data/sumo.txt -ErrorAction SilentlyContinue
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
> deadline return book /by June 6th
____________________________________________________________
 Got it. I've added this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
D | 0 | return book | June 6th
> event project meeting /from Aug 6th 2pm /to 4pm
____________________________________________________________
 Got it. I've added this task:
   [E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
 Now you have 3 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] read book
____________________________________________________________
$ Get-Content data/sumo.txt
T | 1 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] read book
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
> delete 2
____________________________________________________________
 Noted. I've removed this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
E | 0 | project meeting | Aug 6th 2pm | 4pm
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-006 — Load tasks on restart
$ Remove-Item -LiteralPath data/sumo.txt -ErrorAction SilentlyContinue

First session:
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
> deadline return book /by June 6th
____________________________________________________________
 Got it. I've added this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________
> event project meeting /from Aug 6th 2pm /to 4pm
____________________________________________________________
 Got it. I've added this task:
   [E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
 Now you have 3 tasks in the list.
____________________________________________________________
> mark 2
____________________________________________________________
 Nice! I've marked this task as done:
   [D][X] return book (by: June 6th)
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

Second session:
> list
____________________________________________________________
 Here are the tasks in your list:
 1.[T][ ] read book
 2.[D][X] return book (by: June 6th)
 3.[E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

Result: All six listed test cases passed under Java 25.0.4. UI-006 confirmed that tasks and completion state were reconstructed after restarting Sumo.

### 2026-08-27 — PASS (write-only persistence verification)

Transcript:

```text
$ javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/SumoException.java src/main/java/Task.java src/main/java/Todo.java

### UI-001 — Add and list a todo
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
> list
____________________________________________________________
 Here are the tasks in your list:
 1.[T][ ] read book
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-002 — Mark and unmark a task
> todo return book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] return book
 Now you have 1 tasks in the list.
____________________________________________________________
> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] return book
____________________________________________________________
> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] return book
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-003 — Explain invalid commands
> todo
____________________________________________________________
 I could not complete that command: Please add a description after 'todo'.
____________________________________________________________
> blah
____________________________________________________________
 I could not complete that command: I do not recognise that command. Try todo, deadline, event, list, mark, unmark, or delete.
____________________________________________________________
> deadline submit report
____________________________________________________________
 I could not complete that command: Use: deadline <description> /by <date>.
____________________________________________________________
> event meeting /from Monday
____________________________________________________________
 I could not complete that command: Use: event <description> /from <start> /to <end>.
____________________________________________________________
> mark one
____________________________________________________________
 I could not complete that command: Task numbers must be whole numbers.
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-004 — Delete a task
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
> deadline return book /by June 6th
____________________________________________________________
 Got it. I've added this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________
> delete 1
____________________________________________________________
 Noted. I've removed this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
> list
____________________________________________________________
 Here are the tasks in your list:
 1.[D][ ] return book (by: June 6th)
____________________________________________________________
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-005 — Save every task-list change
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
> deadline return book /by June 6th
____________________________________________________________
 Got it. I've added this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
D | 0 | return book | June 6th
> event project meeting /from Aug 6th 2pm /to 4pm
____________________________________________________________
 Got it. I've added this task:
   [E][ ] project meeting (from: Aug 6th 2pm to: 4pm)
 Now you have 3 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] read book
____________________________________________________________
$ Get-Content data/sumo.txt
T | 1 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] read book
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
D | 0 | return book | June 6th
E | 0 | project meeting | Aug 6th 2pm | 4pm
> delete 2
____________________________________________________________
 Noted. I've removed this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________
$ Get-Content data/sumo.txt
T | 0 | read book
E | 0 | project meeting | Aug 6th 2pm | 4pm
> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

Result: All five listed test cases passed under Java 25.0.4. UI-005 confirmed that every task-list mutation immediately rewrote `data/sumo.txt` with the expected content.

### 2026-08-21 — PASS (post-agent-rule verification)

Transcript:

```text
$ javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/Task.java src/main/java/Todo.java

> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________

> list
____________________________________________________________
 Here are the tasks in your list:
 1.[T][ ] read book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

> todo return book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] return book
 Now you have 1 tasks in the list.
____________________________________________________________

> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] return book
____________________________________________________________

> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] return book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

Result: All listed test cases passed under Java 25.0.4 after updating `AGENTS.md`.

### 2026-08-24 — PASS (error-handling verification)

Transcript:

```text
$ javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/SumoException.java src/main/java/Task.java src/main/java/Todo.java

### UI-001 — Add and list a todo
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________

> list
____________________________________________________________
 Here are the tasks in your list:
 1.[T][ ] read book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-002 — Mark and unmark a task
> todo return book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] return book
 Now you have 1 tasks in the list.
____________________________________________________________

> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] return book
____________________________________________________________

> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] return book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-003 — Explain invalid commands
> todo
____________________________________________________________
 I could not complete that command: Please add a description after 'todo'.
____________________________________________________________

> blah
____________________________________________________________
 I could not complete that command: I do not recognise that command. Try todo, deadline, event, list, mark, or unmark.
____________________________________________________________

> deadline submit report
____________________________________________________________
 I could not complete that command: Use: deadline <description> /by <date>.
____________________________________________________________

> event meeting /from Monday
____________________________________________________________
 I could not complete that command: Use: event <description> /from <start> /to <end>.
____________________________________________________________

> mark one
____________________________________________________________
 I could not complete that command: Task numbers must be whole numbers.
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

Result: All listed test cases passed under Java 25.0.4.

### 2026-08-24 — PASS (delete-task verification)

Transcript:

```text
$ javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/SumoException.java src/main/java/Task.java src/main/java/Todo.java

### UI-001 — Add and list a todo
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________

> list
____________________________________________________________
 Here are the tasks in your list:
 1.[T][ ] read book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-002 — Mark and unmark a task
> todo return book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] return book
 Now you have 1 tasks in the list.
____________________________________________________________

> mark 1
____________________________________________________________
 Nice! I've marked this task as done:
   [T][X] return book
____________________________________________________________

> unmark 1
____________________________________________________________
 OK, I've marked this task as not done yet:
   [T][ ] return book
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-003 — Explain invalid commands
> todo
____________________________________________________________
 I could not complete that command: Please add a description after 'todo'.
____________________________________________________________

> blah
____________________________________________________________
 I could not complete that command: I do not recognise that command. Try todo, deadline, event, list, mark, unmark, or delete.
____________________________________________________________

> deadline submit report
____________________________________________________________
 I could not complete that command: Use: deadline <description> /by <date>.
____________________________________________________________

> event meeting /from Monday
____________________________________________________________
 I could not complete that command: Use: event <description> /from <start> /to <end>.
____________________________________________________________

> mark one
____________________________________________________________
 I could not complete that command: Task numbers must be whole numbers.
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________

### UI-004 — Delete a task
> todo read book
____________________________________________________________
 Got it. I've added this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________

> deadline return book /by June 6th
____________________________________________________________
 Got it. I've added this task:
   [D][ ] return book (by: June 6th)
 Now you have 2 tasks in the list.
____________________________________________________________

> delete 1
____________________________________________________________
 Noted. I've removed this task:
   [T][ ] read book
 Now you have 1 tasks in the list.
____________________________________________________________

> list
____________________________________________________________
 Here are the tasks in your list:
 1.[D][ ] return book (by: June 6th)
____________________________________________________________

> bye
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

Result: All listed test cases passed under Java 25.0.4.
