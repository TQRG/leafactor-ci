package com.leafactor.cli.rules.WakeLockCasesOfInterest;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.leafactor.cli.engine.CaseDetector;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.DetectionPhaseContext;
import com.leafactor.cli.engine.RefactoringPhaseContext;
import com.leafactor.cli.rules.GenericCasesOfInterest.VariableDeclared;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.expr.AssignExpr.Operator.ASSIGN;
import static com.leafactor.cli.engine.CaseDetector.EOL;
import static com.leafactor.cli.engine.CaseDetector.TAB;

public class WakeLockAcquire extends CaseOfInterest {
    private String variableName;
    private Statement statement;

    private WakeLockAcquire(String variableName, DetectionPhaseContext context) {
        super(context);
        this.variableName = variableName;
        this.statement = context.statement;
    }

    static private boolean isAcquireCall(MethodCallExpr methodCallExpr) {
        boolean isFindViewByIdCall = methodCallExpr.getName().getIdentifier().equals("acquire");
        boolean takesOneArguments = methodCallExpr.getArguments().size() == 0;
        boolean validInstance = true;
        return isFindViewByIdCall && takesOneArguments && validInstance;
    }

    public static void detect(DetectionPhaseContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if (expression.isMethodCallExpr()) {
            MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
            if (isAcquireCall(methodCallExpr)) {
                Optional<Expression> scope = methodCallExpr.getScope();
                // TODO - Reconsider other possibilities
                if (!scope.isPresent() || !scope.get().isNameExpr()) {
                    return;
                }
                String scopeName = scope.get().asNameExpr().getName().getIdentifier();
                context.caseOfInterestList.add(new WakeLockAcquire(scopeName, context));
            }
        }
    }

    private void removeReleaseCalls(BlockStmt blockStmt) {
        Statement statementToRemove = null;
        for (Statement statement : blockStmt.getStatements()) {
            if (statement.isExpressionStmt() && statement.asExpressionStmt().getExpression().isMethodCallExpr()) {
                MethodCallExpr methodCallExpr = statement.asExpressionStmt().getExpression().asMethodCallExpr();
                if (methodCallExpr.getNameAsString().equals("release")
                        && methodCallExpr.getScope().isPresent()
                        && methodCallExpr.getScope().get().isNameExpr()
                        && methodCallExpr.getScope().get().asNameExpr().getNameAsString().equals(variableName)) {
                    statementToRemove = statement;
                    break;
                }
            }
        }
        if (statementToRemove != null) {
            blockStmt.remove(statementToRemove);
        }
    }

    private void refactorOnPauseMethod(MethodDeclaration methodDeclaration) {
        // Finds a release call in an onPause method
        boolean hasRelease = methodDeclaration.stream().anyMatch(node -> {
            if (node instanceof MethodCallExpr) {
                MethodCallExpr methodCallExpr = (MethodCallExpr) node;
                boolean hasScope = methodCallExpr.getScope().isPresent();
                if (!hasScope) {
                    return false;
                }
                Expression expression = methodCallExpr.getScope().get();
                return expression.isNameExpr()
                        && expression.asNameExpr().getNameAsString().equals(variableName)
                        && methodCallExpr.getNameAsString().equals("release");
            }
            return false;
        });
        if (!hasRelease) {
            String baseTabPadding = CaseDetector.getBaseTabPadding(methodDeclaration.getBody().get());
            // The onPause method is there, lets add the call to release
            Statement statement = LexicalPreservingPrinter.setup(
                    StaticJavaParser.parseStatement(
                            String.format(baseTabPadding + "if(!%s.isHeld()) {" + EOL +
                                            baseTabPadding + TAB + "%s.release();" + EOL +
                                            baseTabPadding + "}" + EOL,
                                    variableName, variableName)));
            methodDeclaration.getBody().ifPresent(blockStmt -> blockStmt.addStatement(statement));
        }
    }

    private void createOnPauseMethod(BlockStmt blockStmt, ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        // We need to create onPause from scratch
        String baseTabPadding = CaseDetector.getBaseTabPadding(blockStmt);
        BodyDeclaration bodyDeclaration = LexicalPreservingPrinter.setup(
                StaticJavaParser.parseBodyDeclaration(
                        String.format("@Override() protected void onPause(){" + EOL +
                                        baseTabPadding + "super.onPause();" + EOL +
                                        baseTabPadding + "if (!%s.isHeld()) {" + EOL +
                                        baseTabPadding + TAB + "%s.release();" + EOL +
                                        baseTabPadding + "}" + EOL +
                                        baseTabPadding.substring(0, baseTabPadding.length() - TAB.length()) + "}" + EOL,
                                variableName, variableName)));
        classOrInterfaceDeclaration.addMember(bodyDeclaration);
    }

    private VariableDeclarator refactorDeclaredVariable(RefactoringPhaseContext refactoringPhaseContext) {
        List<VariableDeclared> variableDeclaredList = refactoringPhaseContext.caseOfInterests.stream()
                .filter(caseOfInterest -> caseOfInterest instanceof VariableDeclared)
                .map(VariableDeclared.class::cast)
                .filter(variableDeclared -> variableDeclared.getVariableType().asString().endsWith("WakeLock"))
                .filter(variableDeclared -> variableDeclared.getVariableName().equals(variableName))
                .collect(Collectors.toList());

        for (VariableDeclared variableDeclared : variableDeclaredList) {
            VariableDeclarationExpr variableDeclarationExpr = variableDeclared.getStatement()
                    .asExpressionStmt().getExpression().asVariableDeclarationExpr();

            for (int i = 0; i < variableDeclarationExpr.getVariables().size(); i++) {
                VariableDeclarator variableDeclarator = variableDeclarationExpr.getVariables().get(i);
                if (variableDeclarator.getNameAsString().equals(variableName)) {
//                            variableDeclarationExpr.getVariables().remove(i);
//                          // TODO - This is a workaround, it forcibly removes the expression from the variable declarator, since JAVAPARSE getVariables().remove is not working properly.
                    Expression expression = variableDeclarator.getInitializer().get().clone();
                    AssignExpr assignExpr = new AssignExpr(new NameExpr(variableName), expression, ASSIGN);
                    Statement statement = new ExpressionStmt(assignExpr);
                    refactoringPhaseContext.blockStmt.setStatement(variableDeclared.getStatementIndex(), statement);
                    return variableDeclarator;
                }
            }
        }
        return null;
    }

    @Override
    public void refactorIteration(RefactoringPhaseContext refactoringPhaseContext) {
        Node root = statement.getParentNode().orElse(statement);
        while (!(root instanceof ClassOrInterfaceDeclaration) && root.getParentNode().isPresent()) {
            root = root.getParentNode().get();
        }
        if (!(root instanceof ClassOrInterfaceDeclaration)) {
            return;
        }

        refactorDeclaredVariable(refactoringPhaseContext);

        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) root;
        boolean hasField = classOrInterfaceDeclaration.getMembers().stream().anyMatch(node -> {
            if (node.isFieldDeclaration()) {
                FieldDeclaration fieldDeclaration = node.asFieldDeclaration();
                for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
                    if (variableDeclarator.getType().asString().endsWith("WakeLock") &&
                            variableDeclarator.getNameAsString().equals(variableName)) {
                        return true;
                    }
                }
            }
            return false;
        });

        if (!hasField) {
            FieldDeclaration fieldDeclaration = new FieldDeclaration();
            VariableDeclarator variableDeclarator = new VariableDeclarator();
            // Todo - Here should use the type of the declared variable instead of simply WakeLock
            variableDeclarator.setType("WakeLock");
            variableDeclarator.setName(variableName);
            fieldDeclaration.addVariable(variableDeclarator);
            fieldDeclaration.setModifiers(Modifier.Keyword.PRIVATE);
            classOrInterfaceDeclaration.addMember(fieldDeclaration);
        }
        List<MethodDeclaration> methodDeclarationList = classOrInterfaceDeclaration.getMethods();
        boolean hasOnPause = false;
        for (MethodDeclaration methodDeclaration : methodDeclarationList) {
            if (methodDeclaration.getNameAsString().equals("onDestroy")
                    && methodDeclaration.getType().isVoidType()
                    && methodDeclaration.getParameters().size() == 0) {
                removeReleaseCalls(methodDeclaration.getBody().get());
            } else if (methodDeclaration.getNameAsString().equals("onPause")
                    && methodDeclaration.getType().isVoidType()
                    && methodDeclaration.getParameters().size() == 0) {
                hasOnPause = true;
                refactorOnPauseMethod(methodDeclaration);
            }
        }

        if (!hasOnPause) {
            createOnPauseMethod(refactoringPhaseContext.blockStmt, classOrInterfaceDeclaration);
        }
    }
}