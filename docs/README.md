# Sumo User Guide

Sumo keeps tasks in the order in which they were added. Use `list` to display that order.

## Adding tasks

Add a todo with:

```text
todo read a book
```

Add a deadline with a date or date and time:

```text
deadline submit report /by 2026-02-03
deadline attend review /by 2026-02-03 0915
```

Add an event with a start and end:

```text
event project meeting /from 2026-02-03 1400 /to 2026-02-03 1600
```

## Sorting tasks temporarily

Use `sort` to display the full task list in ascending chronological order. Incomplete tasks are
shown before completed tasks. Within each group, deadlines are ordered by `by`, events by `to`,
and events with the same `to` are ordered by `from`. Undated todos appear last, and equal values
retain their existing order.

The sorted view is temporary: it does not rewrite `data/sumo.txt` or change the order used by
`list`, `find`, `on`, `mark`, or `delete`.

```text
sort
```

The display is grouped into `Incomplete tasks` and `Completed tasks` and is labelled as a sorted
view. If the list is empty, Sumo replies:

```text
No tasks to be sorted.
```

`sort` takes no arguments. For example, `sort descending` is rejected as an unrecognised command.
