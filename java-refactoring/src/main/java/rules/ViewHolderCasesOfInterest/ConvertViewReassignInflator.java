package rules.ViewHolderCasesOfInterest;

import com.github.javaparser.ast.expr.*;
import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;

public class ConvertViewReassignInflator extends CaseOfInterest {
    private MethodCallExpr methodCallExpr;
    private AssignExpr assignExpr;
    public ConvertViewReassignInflator(AssignExpr assignExpr, MethodCallExpr methodCallExpr, IterationContext context) {
        super(context);
        this.methodCallExpr = methodCallExpr;
        this.assignExpr = assignExpr;
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {
        String argumentName = refactoringIterationContext.context.methodDeclaration.getParameter(1).getName().getIdentifier();
        // TODO - We do not know if the convertView was used up to this point, we might have something like:
            /*  if(convertView != null) {
                    return convertView;
                }
                convertView = layoutInflator.inflate(...);
            */
        // Or
            /*
                if(convertView == null) {
                    convertView = layoutInflator.inflate(...);
                }
             */
        // ConvertView was reassigned with an inflated View
        // Consequence: This is considered bad practice, should be refactored to a ternary expression
        BinaryExpr binaryExpr = new BinaryExpr();
        binaryExpr.setOperator(BinaryExpr.Operator.NOT_EQUALS);
        binaryExpr.setLeft(new NameExpr(argumentName));
        binaryExpr.setRight(new NullLiteralExpr());
        ConditionalExpr conditionalExpr = new ConditionalExpr();
        conditionalExpr.setCondition(binaryExpr);
        conditionalExpr.setThenExpr(new NameExpr(argumentName));
        conditionalExpr.setElseExpr(methodCallExpr);
        assignExpr.setValue(conditionalExpr);
    }
}