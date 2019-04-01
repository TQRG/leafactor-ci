package com.leafactor.cli.rules.WakeLockCasesOfInterest;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.IterationContext;
import com.leafactor.cli.engine.RefactoringIterationContext;
import java.util.Optional;

public class WakeLockAcquire extends CaseOfInterest {
    String variableName;

    public WakeLockAcquire(String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
    }

    static private boolean isAcquireCall(MethodCallExpr methodCallExpr) {
        boolean isFindViewByIdCall = methodCallExpr.getName().getIdentifier().equals("acquire");
        boolean takesOneArguments = methodCallExpr.getArguments().size() == 0;
        boolean validInstance = true;
        return isFindViewByIdCall && takesOneArguments && validInstance;
    }

    public static void checkStatement(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if (expression.isMethodCallExpr()) {
            MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
            if(isAcquireCall(methodCallExpr)) {
                Optional<Expression> scope = methodCallExpr.getScope();
                // TODO - Reconsider other possibilities
                if(!scope.isPresent() || !scope.get().isNameExpr()) {
                    return;
                }
                String scopeName = scope.get().asNameExpr().getName().getIdentifier();
                context.caseOfInterests.add(new WakeLockAcquire(scopeName, context));
            }
        }
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {
        // Todo - Check whether we have field declared with this name -> If not, declare
        // Todo - Check if we have a variable declared with this name

        Statement currentStatement = refactoringIterationContext.context.statement;
        Node root = currentStatement.getParentNode().orElse(currentStatement);
        while (root.getParentNode().isPresent()) {
            root = root.getParentNode().get();
        }
        if(!(root instanceof ClassOrInterfaceDeclaration)) {
            return;
        }

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) root;
        boolean hasField = classOrInterfaceDeclaration.getMembers().stream().anyMatch(node -> {
            if(node.isFieldDeclaration()) {
                FieldDeclaration fieldDeclaration = node.asFieldDeclaration();
                for(VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
                    if(variableDeclarator.getType().asString().endsWith("WakeLock") &&
                            variableDeclarator.getNameAsString().equals(variableName)) {
                        return true;
                    }
                }
            }
            return false;
        });

        if(!hasField) {

        }

    }
}