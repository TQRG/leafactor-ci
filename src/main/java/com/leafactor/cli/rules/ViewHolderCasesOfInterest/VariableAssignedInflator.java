package com.leafactor.cli.rules.ViewHolderCasesOfInterest;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.IterationContext;
import com.leafactor.cli.engine.RefactoringIterationContext;
import com.leafactor.cli.rules.ViewHolderRefactoringRule;

import java.util.Optional;

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

    private static boolean isInflateCall(MethodCallExpr methodCallExpr, IterationContext context) {
        boolean isInflateCall = methodCallExpr.getName().getIdentifier().equals("inflate");
        boolean takesTwoArguments = methodCallExpr.getArguments().size() == 2;
        boolean validInstance = methodCallExpr.getScope().isPresent()
                && ViewHolderRefactoringRule.checkDeclaredLayoutInflator(context, methodCallExpr.getScope().get().asNameExpr());
        return isInflateCall && takesTwoArguments && validInstance;
    }

    public static void checkStatement(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            if(!assignExpr.getValue().isMethodCallExpr()) {
                return;
            }
            // We are assigning to the variable a method call
            MethodCallExpr methodCallExpr = assignExpr.getValue().asMethodCallExpr();
            if(!isInflateCall(methodCallExpr, context)) {
                return;
            }
            Expression target = assignExpr.getTarget();
            if (!target.isNameExpr()) {
                return;
            }
            context.caseOfInterests.add(new VariableAssignedInflator(target.asNameExpr().getNameAsString(), context));

        } else if(expression.isVariableDeclarationExpr()) {
            for(VariableDeclarator variableDeclarator : expression.asVariableDeclarationExpr().getVariables()) {
                String variableName = variableDeclarator.getNameAsString();
                Type type = variableDeclarator.getType();
                Optional<Expression> optionalInitializer = variableDeclarator.getInitializer();
                if(!optionalInitializer.isPresent()) {
                    continue;
                }
                // Ignoring casts for now
                Optional<Type> castType = Optional.empty();
                Expression value = optionalInitializer.get();
                if(value.isCastExpr()) {
                    CastExpr castExpr = value.asCastExpr();
                    castType = Optional.of(castExpr.getType());
                    value = castExpr.getExpression();
                }
                if (!value.isMethodCallExpr()) {
                    continue;
                }
                MethodCallExpr initializer = value.asMethodCallExpr();
                if(isInflateCall(initializer, context)) {
                    context.caseOfInterests.add(new VariableAssignedInflator(variableName, context));
                }
            }
        }
    }
}