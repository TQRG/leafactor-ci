package com.leafactor.cli.rules;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.engine.RefactoringRule;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestWakeLockHardCoded {
    private static final PrintStream out = System.out;
    private static final PrintStream dummy = new PrintStream(new OutputStream() {@Override public void write(int b){} });

    private static void togglePrints(boolean on) {
        System.setOut(on?out:dummy);
    }

    @Test
    public void dynamicTestsWithCollection() {
        togglePrints(false);
        // Dynamic rule instantiation
        CompilationUnit beforeCompilationUnit = LexicalPreservingPrinter.setup(StaticJavaParser.parse(
                   "public class SimpleWakeLockWithoutOnPauseActivity extends Activity {\n" +
                        "\n" +
                        "        @Override\n" +
                        "        protected void onCreate(Bundle savedInstanceState) {\n" +
                        "            super.onCreate(savedInstanceState);\n" +
                        "\n" +
                        "            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);\n" +
                        "            WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, \"WakeLockSample\");\n" +
                        "            wl.acquire();\n" +
                        "        }\n" +
                        "\n" +
                        "        @Override\n" +
                        "        public void onDestroy(){\n" +
                        "            wl.release();\n" +
                        "            super.onDestroy();\n" +
                        "        }\n" +
                        "\n" +
                        "        @Override() protected void onPause(){\n" +
                        "            super.onPause();\n" +
                        "        }\n" +
                        "    }"

        ));
        IterationLogger logger = new IterationLogger();
        RefactoringRule rule = new WakeLockRefactoringRule(logger);

        // Applying refactoring
        rule.apply(beforeCompilationUnit);
        String result = LexicalPreservingPrinter.print(beforeCompilationUnit);

        togglePrints(true);
        String expected =
                "public class SimpleWakeLockWithoutOnPauseActivity extends Activity {\n" +
                "        private WakeLock wl;\n" +
                "\n" +
                "        @Override\n" +
                "        protected void onCreate(Bundle savedInstanceState) {\n" +
                "            super.onCreate(savedInstanceState);\n" +
                "\n" +
                "            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);\n" +
                "            wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, \"WakeLockSample\");\n" +
                "            wl.acquire();\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void onDestroy(){\n" +
                "            super.onDestroy();\n" +
                "        }\n" +
                "\n" +
                "        @Override() protected void onPause(){\n" +
                "            super.onPause();\n" +
                "            if (!wl.isHeld()) {\n" +
                "                wl.release();\n" +
                "            }\n" +
                "        }\n" +
                "    }";

        // Compare result with the sample
        assertEquals(expected, result);
    }
}
