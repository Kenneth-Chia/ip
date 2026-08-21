---
name: test-ui
description: Run the project’s documented console UI test cases, comparing every command’s actual output with its expected output and stopping at the first failure.
---

# Test UI

Use this project-specific skill when the user asks to test the interactive console behavior of the Java program.

## Source of truth

Read `test/ui-test-plan.md` before running anything. The plan is the source of truth for:

- the project working directory and setup command;
- the command used to launch the program;
- the ordered test cases;
- each test case’s aim and ordered console command inputs; and
- the expected output for every command in each case.

Do not invent test cases or silently change expected output. If the plan is incomplete or ambiguous, report the missing detail before testing.

## Execution

1. Use Java 25 for compilation and execution. Confirm the selected Java version before running the plan; stop and report the problem if Java 25 is unavailable.
2. Run any setup or compilation command from the plan, recording its console input and output.
3. Execute the test cases in the order listed. For each case, launch the program using its documented launch command and provide the documented inputs in order.
4. Treat each command/input and expected-output pair as one assertion. Capture the program’s response to that input, excluding output already recorded for an earlier input in the same session. Normalize only `CRLF` to `LF`, then compare the result exactly. Preserve spaces, blank lines, punctuation, and Unicode characters.
5. As soon as an assertion fails, terminate the running program, stop the entire test session, and do not run later test cases. Report the test case, command/input, actual output, and expected output.
6. On success, report that all listed test cases passed.

If a test case needs state from earlier inputs, keep those inputs in the same case and preserve their order. Do not combine separate test cases into one process unless the plan explicitly says to do so.

## Test-session record

Always include a concise transcript in the response after testing. Show each command or console input with a `$ ` or `> ` marker and its resulting output in a fenced `text` block. Include setup/compilation output when it is relevant. On failure, end the transcript at the first failing assertion and clearly label both `Actual output` and `Expected output`.

Do not edit the implementation to make a test pass. If the plan itself needs correction, explain the issue separately and wait for the user’s direction.
