package com.leafactor.cli.rules.ViewHolderCasesOfInterest;

import com.github.javaparser.ast.expr.*;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.DetectionPhaseContext;
import com.leafactor.cli.engine.RefactoringPhaseContext;
import com.leafactor.cli.rules.ViewHolderRefactoringRule;

public class ConvertViewReassignInflator extends CaseOfInterest {
    private MethodCallExpr methodCallExpr;
    private AssignExpr assignExpr;
    private ConvertViewReassignInflator(AssignExpr assignExpr, MethodCallExpr methodCallExpr, DetectionPhaseContext context) {
        super(context);
        this.methodCallExpr = methodCallExpr;
        this.assignExpr = assignExpr;
    }

    public static void detect(DetectionPhaseContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        boolean isAssignExpression = expression.isAssignExpr();
        if (!isAssignExpression) {
            return;
        }
        AssignExpr assignExpr = expression.asAssignExpr();
        if(!assignExpr.getValue().isMethodCallExpr()) {
            return;
        }
        MethodCallExpr methodCallExpr = assignExpr.getValue().asMethodCallExpr();
        boolean isInflateCall = methodCallExpr.getName().getIdentifier().equals("inflate");
        boolean takesTwoArguments = methodCallExpr.getArguments().size() == 2;
        boolean validInstance = methodCallExpr.getScope().isPresent()
                && ViewHolderRefactoringRule.checkDeclaredLayoutInflator(context, methodCallExpr.getScope().get().asNameExpr());
        if (isInflateCall && takesTwoArguments && validInstance) {
            // Here we know that we are calling method with the same signature
            Expression target = assignExpr.getTarget();
            if (!target.isNameExpr()) {
                return;
            }
            String targetName = target.asNameExpr().getName().getIdentifier();
            String argumentName = context.getClosestMethodDeclarationParent().getParameter(1).getName().getIdentifier();
            boolean assignedToConvertView = targetName.equals(argumentName);
            if (assignedToConvertView) {
                context.caseOfInterestList.add(new ConvertViewReassignInflator(assignExpr, methodCallExpr, context));
            }
        }
    }

    @Override
    public void refactorIteration(RefactoringPhaseContext refactoringPhaseContext) {
        String argumentName = refactoringPhaseContext.getClosestMethodDeclarationParent()
                .getParameter(1).getName().getIdentifier();
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