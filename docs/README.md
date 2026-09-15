# Sumo User Guide

Sumo is a task manager you chat with. Track todos, deadlines, and events with short commands,
and pick up where you left off with automatically saved tasks.

## Getting started

1. Install **JDK 25** and follow the [project setup instructions](../README.md#setting-up-in-intellij).
2. Run `sumo.Launcher.main()` in IntelliJ to open the chat window.
3. Type `todo read a book`, then press **Enter** or click **Send**.
4. Try `list` to see your tasks, then `mark 1` to complete your first task.

Prefer the console? Run `sumo.Sumo.main()` in IntelliJ. The commands below work in both interfaces.

## Command basics

- Commands are **case-sensitive**: use `list`, not `List`. Enter one command at a time.
- Replace uppercase placeholders such as `DESCRIPTION` with your own text.
- Dates use `yyyy-MM-dd` or `d/M/yyyy`, for example `2026-09-20` or `20/9/2026`.
- Optional times use four-digit, 24-hour `HHmm`: `0915` means 9:15 am. An omitted time means midnight.
  Square brackets in command formats indicate optional input; do not type the brackets.
- Descriptions cannot be blank or contain `|`, line breaks, or control characters.
  Extra spaces are reduced to single spaces.

## Features

### Add a todo: `todo`

Use a todo for something without a date.

**Format:** `todo DESCRIPTION`

```text
todo read a book
```

Sumo adds an incomplete task and shows the new task count.

### Add a deadline: `deadline`

Use a deadline for something that must be done by a date, optionally with a time.

**Format:** `deadline DESCRIPTION /by DATE [HHmm]`

```text
deadline submit report /by 2026-09-20
deadline pay fees /by 21/9/2026 1700
```

### Add an event: `event`

Use an event for something with a start and end.

**Format:** `event DESCRIPTION /from DATE [HHmm] /to DATE [HHmm]`

```text
event project meeting /from 2026-09-20 1400 /to 2026-09-20 1600
```

The end must be after the start. For a same-day event, include an end time later than the start.
Use each parameter (`/by`, or `/from` and `/to`) exactly once in the order shown.
Other words starting with `/` are reserved in deadline and event commands.

**Duplicate tasks:** Sumo rejects tasks with the same type, description, and dates/times, even if
capitalization, spacing, or completion status differs. A date without a time equals that date at
`0000`. Give recurring tasks different dates or descriptions.

### View all tasks: `list`

Enter `list` to see all tasks in the order you added them, including completed tasks. For example:

```text
Here are the tasks in your list:
 1.[T][ ] read a book
 2.[D][X] submit report (by: Sep 20 2026)
```

`[T]` means todo, `[D]` deadline, and `[E]` event. `[ ]` means incomplete; `[X]` means completed.

### Complete or reopen a task: `mark` / `unmark`

**Formats:** `mark NUMBER` and `unmark NUMBER`

Use `mark 1` to complete the first task, or `unmark 1` to make it incomplete again.
Sumo shows the task with its updated status.

> **Always use the task number from `list`** for `mark`, `unmark`, and `delete`.
> Results from `find`, `on`, and `sort` have their own display numbering. Run `list` again before
> changing a task so you select the right one.

### Delete a task: `delete`

**Format:** `delete NUMBER`

Use `delete 1` to remove the first task. Sumo shows the removed task and remaining count.
Deletion has no undo command, and later task numbers shift up. Run `list` before your next change.

### Find tasks by description: `find`

**Format:** `find TEXT`

Use `find book` to show descriptions containing `book`, including `read a book` and `buy books`.
Matching is case-sensitive: `Book` does not match `book`. Multiple words are matched as one phrase.
If nothing matches, only the results heading appears.

### View tasks on a date: `on`

**Format:** `on DATE`

Use `on 2026-09-20` to see deadlines due that day and events spanning that day, including their
start and end dates. Todos are excluded; completed tasks are included. If nothing matches, only
the results heading appears.

### View tasks chronologically: `sort`

Enter `sort` for a temporary view grouped into incomplete tasks first, then completed tasks.
Within each group:

- Deadlines are ordered by due date/time and events by end date/time, earliest first.
- Events with the same end are ordered by start; other ties keep their existing order.
- Undated todos appear last.

Sorting does not change the saved order or the numbers used to update tasks. Use `list` to return
to the normal view. An empty list produces `No tasks to be sorted.`

### End your session: `bye`

Enter `bye` to end the session. In the chat window, input is disabled and you can close the window;
reopen Sumo to start another session. The console version exits.

`list`, `sort`, and `bye` take no extra arguments.

## Saving and troubleshooting

Tasks are saved automatically after each successful addition, deletion, or status change to
`data/sumo.txt`, relative to the folder Sumo was launched from. Launch from the same folder each time
to load the same tasks. You do not need a save command.

- **Command rejected?** Check the spelling, required description, date/time format, and parameter order.
  Task numbers must be positive whole numbers from `list`.
- **Saving failed?** The attempted change is rolled back. Check the error and that the data folder is
  writable. If the file was changed outside Sumo, restart to load it before trying again.
- **Saved-data warning on startup?** Sumo preserves the original file and disables saving; valid records
  remain viewable when possible. Back up `data/sumo.txt`, repair the reported lines or file permissions,
  and restart Sumo.
