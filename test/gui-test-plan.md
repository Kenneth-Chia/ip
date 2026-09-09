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
