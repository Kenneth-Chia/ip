package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the console UI test cases documented in {@code test/ui-test-plan.md}.
 */
public class UiTestRunner {
    private static final String SEPARATOR = "____________________________________________________________";
    private static final Pattern CASE_PATTERN = Pattern.compile(
            "(?ms)^### (?<id>UI-\\d+) \\S+ (?<name>[^\\r\\n]+)\\r?\\n(?<body>.*?)(?=^### UI-|\\z)");
    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "(?m)^\\s+\\d+\\. Command/input: `(?<command>[^`]+)`\\s*$");
    private static final Pattern OUTPUT_PATTERN = Pattern.compile(
            "(?ms)Expected output:\\s*\\r?\\n\\s*```text\\r?\\n(?<content>.*?)^\\s*```\\s*$");
    private static final Pattern FILE_PATTERN = Pattern.compile(
            "(?ms)Expected `(?<path>[^`]+)` content immediately after the command:"
                    + "\\s*\\r?\\n\\s*```text\\r?\\n(?<content>.*?)^\\s*```\\s*$");

    private final Path repositoryRoot;
    private final boolean shouldShowOutput;

    /**
     * Creates a runner rooted at the current repository.
     *
     * @param repositoryRoot root directory containing {@code test/ui-test-plan.md}.
     * @param shouldShowOutput whether successful application output should be printed.
     */
    public UiTestRunner(Path repositoryRoot, boolean shouldShowOutput) {
        this.repositoryRoot = repositoryRoot;
        this.shouldShowOutput = shouldShowOutput;
    }

    /**
     * Runs every documented UI test case.
     *
     * @param args accepts the optional {@code --show-output} flag.
     * @throws Exception if the plan is invalid, a command fails, or an assertion fails.
     */
    public static void main(String[] args) throws Exception {
        boolean shouldShowOutput = false;
        for (String argument : args) {
            if (argument.equals("--show-output")) {
                shouldShowOutput = true;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }

        Path repositoryRoot = Paths.get("").toAbsolutePath().normalize();
        new UiTestRunner(repositoryRoot, shouldShowOutput).run();
    }

    private void run() throws Exception {
        String plan = normalizeLineEndings(Files.readString(resolvePath("test/ui-test-plan.md")));
        int recordsHeading = plan.indexOf("## Test-session records");
        if (recordsHeading >= 0) {
            plan = plan.substring(0, recordsHeading);
        }

        String requiredJavaVersion = getRequiredJavaVersion(plan);
        String compileCommand = getInlineCommand(plan, "Setup/compile command");
        String launchCommand = getInlineCommand(plan, "Program launch command");
        List<UiTestCase> testCases = getTestCases(plan);
        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("No UI test cases were parsed from the plan.");
        }
        System.out.printf("Parsed %d UI test cases from the plan.%n", testCases.size());

        confirmJavaVersion(requiredJavaVersion);
        long compilationStart = System.nanoTime();
        runDocumentedCommand(compileCommand);
        System.out.println("$ " + compileCommand);
        System.out.printf("Compilation passed in %.2f s.%n", elapsedSeconds(compilationStart));

        long suiteStart = System.nanoTime();
        for (UiTestCase testCase : testCases) {
            runTestCase(testCase, launchCommand);
        }
        System.out.printf("All %d UI test cases passed in %.2f s, excluding compilation.%n",
                testCases.size(), elapsedSeconds(suiteStart));
    }

    private void confirmJavaVersion(String requiredVersion) throws Exception {
        Process process = new ProcessBuilder("java", "-version")
                .redirectErrorStream(true)
                .start();
        String versionOutput = readAllOutput(process);
        int exitCode = process.waitFor();
        if (exitCode != 0 || !versionOutput.contains("version \"" + requiredVersion + ".")) {
            throw new IllegalStateException(
                    "Java " + requiredVersion + " is required.\nSelected Java:\n" + versionOutput.strip());
        }
        System.out.printf("$ java -version -- Java %s confirmed%n", requiredVersion);
    }

    private void runTestCase(UiTestCase testCase, String launchCommand) throws Exception {
        System.out.printf("Starting %s -- %s%n", testCase.id(), testCase.name());
        if (testCase.shouldRemoveDataDirectory()) {
            deleteRecursively(resolvePath("data"));
        } else {
            Files.deleteIfExists(resolvePath("data/sumo.txt"));
        }

        long caseStart = System.nanoTime();
        int currentSession = 0;
        RunningProgram program = null;
        try {
            for (TestStep step : testCase.steps()) {
                if (step.session() != currentSession) {
                    stopProgram(program);
                    program = startProgram(launchCommand);
                    String startupOutput = readOutputBlock(program);
                    if (shouldShowOutput) {
                        System.out.println(startupOutput);
                    }
                    currentSession = step.session();
                }

                if (step.command().isEmpty()) {
                    throw new IllegalArgumentException(testCase.id() + " contains an empty command.");
                }
                program.input().write(step.command());
                program.input().newLine();
                program.input().flush();
                String actualOutput = readOutputBlock(program);
                if (!actualOutput.equals(step.expectedOutput())) {
                    throw new AssertionError(testCase.id() + " failed at > " + step.command()
                            + "\nActual output:\n" + actualOutput
                            + "\nExpected output:\n" + step.expectedOutput());
                }

                System.out.printf("%s > %s -- PASS%n", testCase.id(), step.command());
                if (shouldShowOutput) {
                    System.out.println(actualOutput);
                }
                assertFileIfRequired(testCase, step);
            }

            if (testCase.shouldRequireDataDirectory() && !Files.isDirectory(resolvePath("data"))) {
                throw new AssertionError(testCase.id() + " expected the data directory to exist after startup.");
            }
        } finally {
            stopProgram(program);
        }
        System.out.printf("%s passed in %.2f s.%n", testCase.id(), elapsedSeconds(caseStart));
    }

    private void assertFileIfRequired(UiTestCase testCase, TestStep step) throws IOException {
        if (step.filePath() == null) {
            return;
        }

        Path assertedPath = resolvePath(step.filePath());
        if (!Files.isRegularFile(assertedPath)) {
            throw new AssertionError(testCase.id() + " expected '" + step.filePath()
                    + "' after > " + step.command() + ", but the file does not exist.");
        }
        String actualFile = trimTrailingNewlines(normalizeLineEndings(Files.readString(assertedPath)));
        if (!actualFile.equals(step.expectedFile())) {
            throw new AssertionError(testCase.id() + " file assertion failed after > " + step.command()
                    + "\nActual file content:\n" + actualFile
                    + "\nExpected file content:\n" + step.expectedFile());
        }
        System.out.printf("%s $ read %s -- PASS%n", testCase.id(), step.filePath());
    }

    private RunningProgram startProgram(String launchCommand) throws IOException {
        Process process = new ProcessBuilder(splitCommand(launchCommand))
                .directory(repositoryRoot.toFile())
                .start();
        return new RunningProgram(
                process,
                process.inputReader(StandardCharsets.UTF_8),
                process.errorReader(StandardCharsets.UTF_8),
                process.outputWriter(StandardCharsets.UTF_8));
    }

    private String readOutputBlock(RunningProgram program) throws IOException {
        List<String> lines = new ArrayList<>();
        int separatorCount = 0;
        while (separatorCount < 2) {
            String line = program.output().readLine();
            if (line == null) {
                String errorOutput = program.error().lines().reduce("", (left, right) -> left + right + "\n");
                throw new IllegalStateException(
                        "The application ended before a complete output block was received.\n" + errorOutput);
            }
            lines.add(line);
            if (line.equals(SEPARATOR)) {
                separatorCount++;
            }
        }
        return String.join("\n", lines);
    }

    private void stopProgram(RunningProgram program) throws InterruptedException, IOException {
        if (program == null) {
            return;
        }
        program.input().close();
        if (program.process().isAlive()) {
            program.process().destroy();
            if (!program.process().waitFor(1, TimeUnit.SECONDS)) {
                program.process().destroyForcibly();
            }
        }
        program.process().waitFor();
    }

    private void runDocumentedCommand(String command) throws Exception {
        Process process = new ProcessBuilder(splitCommand(command))
                .directory(repositoryRoot.toFile())
                .inheritIO()
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(
                    "Documented command failed with exit code " + exitCode + ": " + command);
        }
    }

    private List<UiTestCase> getTestCases(String markdown) {
        List<UiTestCase> testCases = new ArrayList<>();
        Matcher caseMatcher = CASE_PATTERN.matcher(markdown);
        while (caseMatcher.find()) {
            String body = caseMatcher.group("body");
            int secondSessionPosition = body.indexOf("- Second session inputs");
            List<MatcherResult> commands = findCommands(body);
            if (commands.isEmpty()) {
                throw new IllegalArgumentException(caseMatcher.group("id") + " does not contain any command inputs.");
            }

            List<TestStep> steps = new ArrayList<>();
            for (int index = 0; index < commands.size(); index++) {
                MatcherResult command = commands.get(index);
                int sectionStart = command.end();
                int sectionEnd = index + 1 < commands.size() ? commands.get(index + 1).start() : body.length();
                String section = body.substring(sectionStart, sectionEnd);
                Matcher outputMatcher = OUTPUT_PATTERN.matcher(section);
                if (!outputMatcher.find()) {
                    throw new IllegalArgumentException(caseMatcher.group("id")
                            + " has no expected output for '" + command.command() + "'.");
                }

                Matcher fileMatcher = FILE_PATTERN.matcher(section);
                boolean hasFileAssertion = fileMatcher.find();
                int session = secondSessionPosition >= 0 && command.start() > secondSessionPosition ? 2 : 1;
                steps.add(new TestStep(
                        command.command(),
                        removeMarkdownIndent(outputMatcher.group("content")),
                        session,
                        hasFileAssertion ? fileMatcher.group("path") : null,
                        hasFileAssertion ? removeMarkdownIndent(fileMatcher.group("content")) : null));
            }

            testCases.add(new UiTestCase(
                    caseMatcher.group("id"),
                    caseMatcher.group("name"),
                    List.copyOf(steps),
                    body.contains("delete the `data` directory recursively if it exists"),
                    body.contains("Confirm that `data` exists after startup")));
        }
        return testCases;
    }

    private List<MatcherResult> findCommands(String body) {
        List<MatcherResult> commands = new ArrayList<>();
        Matcher matcher = COMMAND_PATTERN.matcher(body);
        while (matcher.find()) {
            commands.add(new MatcherResult(matcher.start(), matcher.end(), matcher.group("command")));
        }
        return commands;
    }

    private String getRequiredJavaVersion(String markdown) {
        Matcher matcher = Pattern.compile("(?m)^- Java version: (?<version>\\d+)\\s*$").matcher(markdown);
        if (!matcher.find()) {
            throw new IllegalArgumentException("The plan does not define 'Java version'.");
        }
        return matcher.group("version");
    }

    private String getInlineCommand(String markdown, String label) {
        Pattern pattern = Pattern.compile("(?m)^- " + Pattern.quote(label) + ": `(?<command>[^`]+)`$");
        Matcher matcher = pattern.matcher(markdown);
        if (!matcher.find()) {
            throw new IllegalArgumentException("The plan does not define '" + label + "'.");
        }
        return matcher.group("command");
    }

    private List<String> splitCommand(String command) {
        if (command.contains("\"") || command.contains("'")) {
            throw new IllegalArgumentException(
                    "Quoted arguments are not supported in documented commands: " + command);
        }
        return List.of(command.trim().split("\\s+"));
    }

    private Path resolvePath(String relativePath) {
        Path resolvedPath = repositoryRoot;
        for (String segment : relativePath.split("[/\\\\]")) {
            if (!segment.isEmpty()) {
                resolvedPath = resolvedPath.resolve(segment);
            }
        }
        return resolvedPath.normalize();
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path currentPath : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(currentPath);
            }
        }
    }

    private String readAllOutput(Process process) throws IOException {
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            return reader.lines().reduce("", (left, right) -> left + right + "\n");
        }
    }

    private String removeMarkdownIndent(String text) {
        String[] lines = normalizeLineEndings(text).split("\n", -1);
        int minimumIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (!line.isEmpty()) {
                minimumIndent = Math.min(minimumIndent, line.length() - line.stripLeading().length());
            }
        }
        if (minimumIndent == Integer.MAX_VALUE) {
            return "";
        }

        List<String> adjustedLines = new ArrayList<>();
        for (String line : lines) {
            adjustedLines.add(line.length() >= minimumIndent ? line.substring(minimumIndent) : line);
        }
        return trimTrailingNewlines(String.join("\n", adjustedLines));
    }

    private String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n");
    }

    private String trimTrailingNewlines(String text) {
        return text.replaceFirst("\\n+$", "");
    }

    private double elapsedSeconds(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000_000.0;
    }

    private record UiTestCase(String id, String name, List<TestStep> steps,
                              boolean shouldRemoveDataDirectory, boolean shouldRequireDataDirectory) {
    }

    private record TestStep(String command, String expectedOutput, int session,
                            String filePath, String expectedFile) {
    }

    private record MatcherResult(int start, int end, String command) {
    }

    private record RunningProgram(Process process, BufferedReader output, BufferedReader error,
                                  BufferedWriter input) {
    }
}
