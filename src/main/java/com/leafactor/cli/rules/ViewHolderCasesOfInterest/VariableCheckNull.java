package com.leafactor.cli.rules.ViewHolderCasesOfInterest;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.IterationContext;
import com.leafactor.cli.engine.RefactoringIterationContext;

public class VariableCheckNull extends CaseOfInterest {

    String variableName;
    public VariableCheckNull(String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
    }

    public VariableCheckNull(String variableName, IterationContext context, Statement statement, int index, int statementIndex) {
        super(context);
        this.variableName = variableName;
        this.statement = statement;
        this.index = index;
        this.statementIndex = statementIndex;
    }

    private static boolean isNullCondition(Expression conditionExpr, IterationContext context) {
        if(!conditionExpr.isBinaryExpr()) {
            return false;
        }
        BinaryExpr binaryExpr = conditionExpr.asBinaryExpr();
        if (binaryExpr.getOperator() != BinaryExpr.Operator.EQUALS
                && binaryExpr.getOperator() != BinaryExpr.Operator.NOT_EQUALS) {
            // Not a condition that of interest.
            return false;
        }

        // Check both orders if we are check equality with the ConvertView
        return (binaryExpr.getLeft().isNameExpr()
                && binaryExpr.getRight().isNullLiteralExpr()) ||
                (binaryExpr.getLeft().isNullLiteralExpr()
                        && binaryExpr.getRight().isNameExpr());
    }

    public static void checkStatement(IterationContext context) {
        boolean isIfStmt = context.statement.isIfStmt();
        if (!isIfStmt) {
            return;
        }
        IfStmt ifStmt = context.statement.asIfStmt();
        if (!isNullCondition(ifStmt.getCondition(), context)) {
            return;
        }
        String variableName;
        if(ifStmt.getCondition().asBinaryExpr().getLeft().isNameExpr()) {
            variableName = ifStmt.getCondition().asBinaryExpr().getLeft().asNameExpr().getNameAsString();
        } else {
            variableName = ifStmt.getCondition().asBinaryExpr().getRight().asNameExpr().getNameAsString();
        }
        context.caseOfInterests.add(new VariableCheckNull(variableName, context));
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {
        // Left empty
    }
}