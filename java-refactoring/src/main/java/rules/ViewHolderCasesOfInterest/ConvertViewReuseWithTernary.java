package rules.ViewHolderCasesOfInterest;

import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;

public class ConvertViewReuseWithTernary extends CaseOfInterest {
    String variableName;
    public ConvertViewReuseWithTernary(String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {
        // Left empty
    }
}