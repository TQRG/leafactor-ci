package rules.recycle;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.Test;
import rules.RecycleRefactoringRule;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RecycleTests {
    private static final PrintStream out = System.out;
    private static final PrintStream dummy = new PrintStream(new OutputStream() {@Override public void write(int b){} });
    private static void togglePrints(boolean on) {
        System.setOut(on?out:dummy);
    }

    private String loadSample(String relativePath) throws IOException {
        String absolutePath = RecycleTests.class.getResource(relativePath).getPath().substring(1);
        return new String(Files.readAllBytes(Paths.get(absolutePath)));
    }

    @Test
    public void testBasic() throws IOException, DiffException {
        togglePrints(false);
        // Load input files
        String beforeSample = loadSample("./BasicBefore.java");
        String afterSample = loadSample("./BasicAfter.java");

        // Apply refactoring
        CompilationUnit beforeCompilationUnit = LexicalPreservingPrinter.setup(JavaParser.parse(beforeSample));
        RecycleRefactoringRule rule = new RecycleRefactoringRule();
        rule.apply(beforeCompilationUnit);
        String after = LexicalPreservingPrinter.print(beforeCompilationUnit);

        // Compare result with the sample
        assert(after.equals(afterSample));
    }

}
