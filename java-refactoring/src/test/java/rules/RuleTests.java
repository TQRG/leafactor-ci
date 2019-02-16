package rules;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import engine.RefactoringRule;
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

import static org.junit.Assert.fail;

class RuleTests {
    private static final PrintStream out = System.out;
    private static final PrintStream dummy = new PrintStream(new OutputStream() {@Override public void write(int b){} });

    private static void togglePrints(boolean on) {
        System.setOut(on?out:dummy);
    }

    private String relativePathToAbsolutePath(String relativePath) {
        return RuleTests.class.getResource(relativePath).getPath().substring(1);
    }

    private String loadSample(String relativePath) throws IOException {
        String absolutePath = RuleTests.class.getResource(relativePath).getPath().substring(1);
        return new String(Files.readAllBytes(Paths.get(absolutePath)));
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsWithCollection() throws IOException {
        String dir = relativePathToAbsolutePath("./");
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
                                                CompilationUnit beforeCompilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(inputSample));
                                                Class<?> clazz = Class.forName("rules." + file.getFileName().toString());
                                                Constructor<?> ctor = clazz.getConstructor();
                                                RefactoringRule rule = (RefactoringRule) ctor.newInstance();

                                                // Applying refactoring
                                                rule.apply(beforeCompilationUnit);
                                                String result = LexicalPreservingPrinter.print(beforeCompilationUnit);

                                                togglePrints(true);
                                                System.out.println("[" + subFile.getFileName().toString() + "] Comparing result");
                                                // Compare result with the sample

                                                if(!result.equals(outputSample)) {
                                                    System.out.println("RESULT \n[" + result + "]");
                                                    System.out.println("EXPECTED \n[" + outputSample + "]");
                                                    fail("Result does not match expected output");
                                                    return;
                                                }

                                                System.out.println("[" + subFile.getFileName().toString() + "] Results matches output");
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
