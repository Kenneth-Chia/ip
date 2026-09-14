# JavaFX GUI test plan

Run `.\gradlew.bat run` from the repository root before each test case unless stated otherwise.

## GUI-001 — Send commands using the keyboard and button

- Confirm the window title is `Sumo` and the initial Sumo dialog says `Hello! I'm Sumo.` followed by
  `What can I do for you?`.
- Enter `todo read book` and press **Enter**. Confirm a user dialog and Sumo's task-added response
  appear.
- Enter `list` and click **Send**. Confirm a user dialog and a response containing the added task
  appear.
- Enter `bye`. Confirm the farewell appears and the input field and button become disabled.

## GUI-002 — Continue without avatar files

Ensure `DaUser.png` and `DaSumo.png` are absent from `src/main/resources/images`, then start the GUI.
Confirm it starts and accepts commands, with blank avatar areas beside the dialogs.

## GUI-003 — Keep the newest dialog visible

Enter enough valid commands to exceed the visible dialog area. Confirm that the scroll pane moves to
show the newest dialog after each response is added.

## GUI-004 — Show saved-data warnings and preserve rejected records

Back up `data/sumo.txt`, then put a valid record followed by an invalid record in the file:
`T | 0 | read book` followed by `invalid record` on its own line. Start Sumo and confirm the chat
shows a warning about line 2 and says saving is disabled. Send `list` and confirm the valid task appears.
Send `delete 1` and confirm a saving error appears; `list` must still show the task and the original
file must remain unchanged. Close Sumo and restore the backup after this check.
