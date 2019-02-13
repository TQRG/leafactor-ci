import rules.RecycleRefactoringRule;
import rules.RefactoringRule;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String [] args) throws IOException, URISyntaxException {
        List<RefactoringRule> refactoringRules = new ArrayList<>();
        refactoringRules.add(new RecycleRefactoringRule());
        URL url = Main.class.getResource("./samples/recycle/recycle1");
        File directory = Paths.get(url.toURI()).toFile();
        System.out.println("Absolute path: " + directory.getAbsolutePath());
        CompilationUnitGroup group = new CompilationUnitGroup(directory);
        group.run(refactoringRules);
        group.printYaml();
    }
}
