package com.leafactor.cli.rules.ViewHolderCasesOfInterest;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.IterationContext;
import com.leafactor.cli.engine.RefactoringIterationContext;
import com.leafactor.cli.rules.ViewHolderRefactoringRule;

import java.util.Optional;

public class ConvertViewReuseWithTernary extends CaseOfInterest {
    String variableName;

    public ConvertViewReuseWithTernary(String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
    }

    private static boolean isConditionalInflateAssignment(ConditionalExpr conditionalExpr, IterationContext context) {
        String argumentName = context.methodDeclaration.getParameter(1).getName().getIdentifier();
        BinaryExpr binaryExpr = conditionalExpr.getCondition().asBinaryExpr();

        if (binaryExpr.getOperator() != BinaryExpr.Operator.EQUALS
                && binaryExpr.getOperator() != BinaryExpr.Operator.NOT_EQUALS) {
            // Not a condition that of interest.
            return false;
        }

        // Check both orders if we are check equality with the ConvertView
        if ((!binaryExpr.getLeft().isNameExpr()
                || !binaryExpr.getRight().isNullLiteralExpr()
                || !binaryExpr.getLeft().asNameExpr().getNameAsString().equals(argumentName)) &&
                (!binaryExpr.getLeft().isNullLiteralExpr()
                        || !binaryExpr.getRight().isNameExpr()
                        || !binaryExpr.getLeft().asNameExpr().getNameAsString().equals(argumentName))) {
            // Not a condition that of interest.
            return false;
        }

        Expression expressionA = conditionalExpr.getThenExpr(); // ConvertView variable
        Expression expressionB = conditionalExpr.getElseExpr(); // Inflation
        if (binaryExpr.getOperator() == BinaryExpr.Operator.EQUALS) {
            // We have to invert the expressions
            Expression temporary = expressionA;
            expressionA = expressionB;
            expressionB = temporary;
        }

        if (!expressionA.isNameExpr()
                || !expressionB.isMethodCallExpr()) {
            // Not a condition that of interest.
            return false;
        }

        MethodCallExpr methodCallExpr = expressionB.asMethodCallExpr();
        boolean isInflateCall = methodCallExpr.getName().getIdentifier().equals("inflate");
        boolean takesTwoArguments = methodCallExpr.getArguments().size() == 2;
        boolean validInstance = methodCallExpr.getScope().isPresent()
                && ViewHolderRefactoringRule.checkDeclaredLayoutInflator(context, methodCallExpr.getScope().get().asNameExpr());
        return expressionA.asNameExpr().getNameAsString().equals(argumentName) && isInflateCall && takesTwoArguments && validInstance;
    }

    public static void checkStatement(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if (expression.isAssignExpr() && expression.asAssignExpr().getValue().isConditionalExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            // We are assigning to the variable a method call
            ConditionalExpr conditionalExpr = assignExpr.getValue().asConditionalExpr();
            if (isConditionalInflateAssignment(conditionalExpr, context)) {
                Expression target = assignExpr.getTarget();
                if (!target.isNameExpr()) {
                    return;
                }
                context.caseOfInterests.add(new ConvertViewReuseWithTernary(target.asNameExpr().getNameAsString(), context));
            }
        } else if (expression.isVariableDeclarationExpr()) {
            for (VariableDeclarator variableDeclarator : expression.asVariableDeclarationExpr().getVariables()) {
                String variableName = variableDeclarator.getNameAsString();
                Type type = variableDeclarator.getType();
                Optional<Expression> optionalInitializer = variableDeclarator.getInitializer();
                if (!optionalInitializer.isPresent()) {
                    continue;
                }
                if (!optionalInitializer.get().isConditionalExpr()) {
                    continue;
                }
                ConditionalExpr conditionalExpr = optionalInitializer.get().asConditionalExpr();
                if (isConditionalInflateAssignment(conditionalExpr, context)) {
                    context.caseOfInterests.add(new ConvertViewReuseWithTernary(variableName, context));
                }
            }
        }
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {
        // Left empty
    }
}