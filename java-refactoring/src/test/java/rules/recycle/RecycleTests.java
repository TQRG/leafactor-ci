package rules.recycle;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import rules.RecycleRefactoringRule;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class RecycleTests {
    private static final PrintStream out = System.out;
    private static final PrintStream dummy = new PrintStream(new OutputStream() {@Override public void write(int b){} });

    private static void togglePrints(boolean on) {
        System.setOut(on?out:dummy);
    }

    private String relativePathToAbsolutePath(String relativePath) {
        return RecycleTests.class.getResource(relativePath).getPath().substring(1);
    }

    private String loadSample(String relativePath) throws IOException {
        String absolutePath = RecycleTests.class.getResource(relativePath).getPath().substring(1);
        return new String(Files.readAllBytes(Paths.get(absolutePath)));
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsWithCollection() throws IOException {
        String dir = relativePathToAbsolutePath("./");
        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            return paths
                    .filter((file) ->  !file.equals(Paths.get(dir)))
                    .filter(Files::isDirectory)
                    .map((file) -> DynamicTest.dynamicTest(file.getFileName().toString(),
                            () -> {
                                togglePrints(true);

                                String beforePath = file.toAbsolutePath() + "\\Before.java";
                                String afterPath = file.toAbsolutePath() + "\\After.java";

                                // Load input files
                                System.out.println("[" + file.getFileName().toString() + "] Loading Files");
                                String beforeSample = new String(Files.readAllBytes(Paths.get(beforePath)));
                                String afterSample = new String(Files.readAllBytes(Paths.get(afterPath)));

                                // Apply refactoring
                                System.out.println("[" + file.getFileName().toString() + "] Finding and refactoring opportunities");
                                togglePrints(false);
                                CompilationUnit beforeCompilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(beforeSample));
                                RecycleRefactoringRule rule = new RecycleRefactoringRule();
                                rule.apply(beforeCompilationUnit);
                                String after = LexicalPreservingPrinter.print(beforeCompilationUnit);

                                togglePrints(true);
                                System.out.println("[" + file.getFileName().toString() + "] Comparing result");
                                // Compare result with the sample
                                assert(after.equals(afterSample));
                            })).collect(Collectors.toList());
        }
    }

}
