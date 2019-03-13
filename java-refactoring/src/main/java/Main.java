import engine.CompilationUnitGroup;
        import rules.RecycleRefactoringRule;
        import engine.RefactoringRule;
import rules.ViewHolderRefactoringRule;

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
        refactoringRules.add(new ViewHolderRefactoringRule());
        URL url = Main.class.getResource("./samples/recycle/recycle1");
        File directory = Paths.get(url.toURI()).toFile();
        CompilationUnitGroup group = new CompilationUnitGroup(directory);
        group.run(refactoringRules);
        group.printYaml();
    }
}
