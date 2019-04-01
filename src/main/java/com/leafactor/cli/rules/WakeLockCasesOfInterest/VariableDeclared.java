package com.leafactor.cli.rules.WakeLockCasesOfInterest;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.IterationContext;
import com.leafactor.cli.engine.RefactoringIterationContext;

public class VariableDeclared extends CaseOfInterest {
    Type variableType;
    String variableName;
    public VariableDeclared(Type variableType, String variableName, IterationContext context) {
        super(context);
        this.variableType = variableType;
        this.variableName = variableName;
    }

    public static void checkStatement(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if(expression.isVariableDeclarationExpr()) {
            for(VariableDeclarator variableDeclarator : expression.asVariableDeclarationExpr().getVariables()) {
                String variableName = variableDeclarator.getNameAsString();
                Type type = variableDeclarator.getType();
                context.caseOfInterests.add(new VariableDeclared(type, variableName, context));
            }
        }
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {

    }
}