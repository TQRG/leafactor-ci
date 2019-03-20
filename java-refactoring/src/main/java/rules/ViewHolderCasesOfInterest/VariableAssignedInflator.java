package rules.ViewHolderCasesOfInterest;

import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;

public class VariableAssignedInflator extends CaseOfInterest {
    // Other variable was reassigned with an inflated View
    // Consequence: We need to keep track of it
    String variableName;
    public VariableAssignedInflator(String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {
        // TODO - We need to check reuse of convertView
    }
}