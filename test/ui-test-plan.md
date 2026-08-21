# Console UI test plan

This file is the source of truth for the `test-ui` skill. Keep test cases deterministic and update expected output only when the intended UI behavior changes.

## Execution information

- Working directory: repository root
- Java version: 25
- Setup/compile command: `javac -d out src/main/java/Deadline.java src/main/java/Event.java src/main/java/Sumo.java src/main/java/Task.java src/main/java/Todo.java`
- Program launch command: `java -cp out Sumo`
- Output comparison: exact, after normalizing Windows `CRLF` line endings to `LF`; each expected block contains only the response produced after its listed input
- Test isolation: launch a fresh program process for each test case unless a case explicitly requires one continuous session

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

## Latest test session

Leave the test cases and expected outputs above unchanged when recording a run. Add a dated session below with the actual console transcript, overall result, and—if applicable—the first failure’s actual and expected output.

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
