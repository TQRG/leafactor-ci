package com.leafactor.cli.rules;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.engine.RefactoringRule;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestRules {
    private static final PrintStream out = System.out;
    private static final PrintStream dummy = new PrintStream(new OutputStream() {@Override public void write(int b){} });

    private static void togglePrints(boolean on) {
        System.setOut(on?out:dummy);
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsWithCollection() throws IOException {
        String dir = TestRules.class.getResource("./").getPath().substring(1);
        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            return paths
                    .filter((file) -> !file.equals(Paths.get(dir)))
                    .filter(Files::isDirectory)
                    .map((file) -> {
                        try (Stream<Path> subPaths = Files.walk(file)) {
                            return subPaths.filter((subFile) -> !subFile.equals(file))
                                    .filter(Files::isDirectory)
                                    .map((subFile) -> DynamicTest.dynamicTest(
                                            file.getFileName() + "-" + subFile.getFileName(),
                                            () -> {
                                                togglePrints(true);
                                                String beforePath = subFile.toAbsolutePath() + "\\Input.java";
                                                String afterPath = subFile.toAbsolutePath() + "\\Output.java";

                                                // Load input files
                                                System.out.println("[" + subFile.getFileName().toString() + "] Loading Files");
                                                String inputSample = new String(Files.readAllBytes(Paths.get(beforePath)));
                                                String outputSample = new String(Files.readAllBytes(Paths.get(afterPath)));

                                                System.out.println("[" + subFile.getFileName().toString() + "] Finding and refactoring opportunities");
                                                togglePrints(false);

                                                // Dynamic rule instantiation
                                                CompilationUnit beforeCompilationUnit = LexicalPreservingPrinter.setup(StaticJavaParser.parse(inputSample));
                                                Class<?> clazz = Class.forName("com.leafactor.cli.rules." + file.getFileName().toString());
                                                IterationLogger logger = new IterationLogger();
                                                Constructor<?> ctor = clazz.getConstructor(IterationLogger.class);
                                                RefactoringRule rule = (RefactoringRule) ctor.newInstance(logger);

                                                // Applying refactoring
                                                rule.apply(beforeCompilationUnit);
                                                String result = LexicalPreservingPrinter.print(beforeCompilationUnit);

                                                togglePrints(true);
                                                System.out.println("[" + subFile.getFileName().toString() + "] Comparing result");
                                                // Compare result with the sample
                                                assertEquals(outputSample, result);
                                            })).collect(Collectors.toList());
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }

}
