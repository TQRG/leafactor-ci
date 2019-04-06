package com.leafactor.cli.rules.GenericCasesOfInterest;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.DetectionPhaseContext;
import com.leafactor.cli.engine.RefactoringPhaseContext;

public class VariableDeclared extends CaseOfInterest {
    private Type variableType;
    private String variableName;
    private VariableDeclared(Type variableType, String variableName, DetectionPhaseContext context) {
        super(context);
        this.variableType = variableType;
        this.variableName = variableName;
    }

    public static void detect(DetectionPhaseContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if(expression.isVariableDeclarationExpr()) {
            for(VariableDeclarator variableDeclarator : expression.asVariableDeclarationExpr().getVariables()) {
                String variableName = variableDeclarator.getNameAsString();
                Type type = variableDeclarator.getType();
                context.caseOfInterestList.add(new VariableDeclared(type, variableName, context));
            }
        }
    }

    @Override
    public void refactorIteration(RefactoringPhaseContext refactoringPhaseContext) {

    }
}