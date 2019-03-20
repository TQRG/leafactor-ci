package rules.ViewHolderCasesOfInterest;

import com.github.javaparser.ast.type.Type;
import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;

public class VariableDeclared extends CaseOfInterest {
    Type variableType;
    String variableName;
    public VariableDeclared(Type variableType, String variableName, IterationContext context) {
        super(context);
        this.variableType = variableType;
        this.variableName = variableName;
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {

    }
}