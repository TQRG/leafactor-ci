package com.leafactor.cli.rules.ViewHolderCasesOfInterest;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.IterationContext;
import com.leafactor.cli.engine.RefactoringIterationContext;

import java.util.Optional;

public class VariableAssignedGetTag extends CaseOfInterest {
    String variableName;
    // assignExpr and variableDeclarator are mutual exclusive
    private AssignExpr assignExpr;
    private VariableDeclarator variableDeclarator;
    private Type castType;
    private VariableAssignedGetTag(AssignExpr assignExpr, Type castType, String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
        this.assignExpr = assignExpr;
        this.castType = castType;
    }

    private VariableAssignedGetTag(VariableDeclarator variableDeclarator, Type castType, String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
        this.variableDeclarator = variableDeclarator;
        this.castType = castType;
    }

    static private boolean isGetTagCall(MethodCallExpr methodCallExpr) {
        boolean isFindViewByIdCall = methodCallExpr.getName().getIdentifier().equals("getTag");
        boolean takesOneArguments = methodCallExpr.getArguments().size() == 0;
        boolean validInstance = true;
        return isFindViewByIdCall && takesOneArguments && validInstance;
    }

    public static void detect(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            Optional<Type> castType = Optional.empty();
            Expression value = assignExpr.getValue();

            if(value.isCastExpr()) {
                CastExpr castExpr = value.asCastExpr();
                castType = Optional.of(castExpr.getType());
                value = castExpr.getExpression();
            }

            if (!value.isMethodCallExpr()) {
                return;
            }
            // We are assigning to the variable a method call
            MethodCallExpr methodCallExpr = value.asMethodCallExpr();
            if(isGetTagCall(methodCallExpr)) {
                Expression target = assignExpr.getTarget();
                // TODO - Reconsider other possibilities
                if (!target.isNameExpr()) {
                    return;
                }
                String targetName = target.asNameExpr().getName().getIdentifier();
                context.caseOfInterests.add(new VariableAssignedGetTag(assignExpr, castType.orElse(null), targetName, context));
            }
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
                if(isGetTagCall(initializer)) {
                    context.caseOfInterests.add(new VariableAssignedGetTag(variableDeclarator, castType.orElse(null), variableName, context));
                }
            }
        }
    }

    @Override
    public void refactorIteration(RefactoringIterationContext refactoringIterationContext) {

    }
}