package com.leafactor.cli.rules.DrawAllocationCasesOfInterest;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.DetectionPhaseContext;
import com.leafactor.cli.engine.RefactoringPhaseContext;

import java.util.Optional;

public class ObjectAllocation extends CaseOfInterest {
    private boolean isClearable;
    private String variableName;
    // assignExpr and variableDeclarator are mutual exclusive
    private AssignExpr assignExpr;
    private VariableDeclarator variableDeclarator;
    private Type castType;
    private ClassOrInterfaceDeclaration classDeclaration;

    private ObjectAllocation(ClassOrInterfaceDeclaration classDeclaration, AssignExpr assignExpr, Type castType, String variableName, DetectionPhaseContext context) {
        super(context);
        this.variableName = variableName;
        this.assignExpr = assignExpr;
        this.castType = castType;
        this.classDeclaration = classDeclaration;
    }

    private ObjectAllocation(ClassOrInterfaceDeclaration classDeclaration, boolean isClearable, VariableDeclarator variableDeclarator, Type castType, String variableName, DetectionPhaseContext context) {
        super(context);
        this.variableName = variableName;
        this.variableDeclarator = variableDeclarator;
        this.castType = castType;
        this.isClearable = isClearable;
        this.classDeclaration = classDeclaration;
    }

    private static boolean isClearable(Type type) {
        return type.asString().startsWith("Collection") || type.asString().startsWith("java.util.List")
                || type.asString().startsWith("List") || type.asString().startsWith("java.util.List")
                || type.asString().startsWith("ArrayList") || type.asString().startsWith("java.util.ArrayList")
                || type.asString().startsWith("LinkedList") || type.asString().startsWith("java.util.LinkedList")
                || type.asString().startsWith("Vector") || type.asString().startsWith("java.util.Vector")
                || type.asString().startsWith("Stack") || type.asString().startsWith("java.util.Stack")
                || type.asString().startsWith("Set") || type.asString().startsWith("java.util.Set")
                || type.asString().startsWith("HashSet") || type.asString().startsWith("java.util.HashSet")
                || type.asString().startsWith("LinkedHashSet") || type.asString().startsWith("java.util.LinkedHashSet")
                || type.asString().startsWith("SortedSet") || type.asString().startsWith("java.util.SortedSet")
                || type.asString().startsWith("NavigableSet") || type.asString().startsWith("java.util.NavigableSet")
                || type.asString().startsWith("TreeSet") || type.asString().startsWith("java.util.TreeSet")
                || type.asString().startsWith("EnumSet") || type.asString().startsWith("java.util.EnumSet")
                || type.asString().startsWith("Queue") || type.asString().startsWith("java.util.Queue")
                || type.asString().startsWith("PriorityQueue") || type.asString().startsWith("java.util.PriorityQueue")
                || type.asString().startsWith("Deque") || type.asString().startsWith("java.util.Deque")
                || type.asString().startsWith("ArrayDeque") || type.asString().startsWith("java.util.ArrayDeque")
                || type.asString().startsWith("Map") || type.asString().startsWith("java.util.Map")
                || type.asString().startsWith("HashMap") || type.asString().startsWith("java.util.HashMap")
                || type.asString().startsWith("SortedMap") || type.asString().startsWith("java.util.SortedMap")
                || type.asString().startsWith("NavigableMap") || type.asString().startsWith("java.util.NavigableMap")
                || type.asString().startsWith("TreeMap") || type.asString().startsWith("java.util.TreeMap");
    }

    private static boolean isInterestingObjectAllocation(ObjectCreationExpr objectCreationExpr) {
        // todo - check cast
        if(objectCreationExpr.getTypeAsString().equals("Integer")) {
            return false;
        }
        return true;
    }

    public static void detect(DetectionPhaseContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = context.getClosestClassOrInterfaceDeclarationParent();
        if(classOrInterfaceDeclaration == null) {
            return;
        }
        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            Optional<Type> castType = Optional.empty();
            Expression value = assignExpr.getValue();

            if (value.isCastExpr()) {
                CastExpr castExpr = value.asCastExpr();
                castType = Optional.of(castExpr.getType());
                value = castExpr.getExpression();
            }

            if (!value.isObjectCreationExpr()) {
                return;
            }
            // We are assigning to the variable a method call
            ObjectCreationExpr objectCreationExpr = value.asObjectCreationExpr();
            if (isInterestingObjectAllocation(objectCreationExpr)) {
                Expression target = assignExpr.getTarget();
                if (!(target instanceof NodeWithSimpleName)) {
                    return;
                }
                String targetName = ((NodeWithSimpleName) target).getNameAsString();
                context.caseOfInterestList.add(
                        new ObjectAllocation(classOrInterfaceDeclaration,
                        assignExpr, castType.orElse(null), targetName, context));
            }
        } else if (expression.isVariableDeclarationExpr()) {
            for (VariableDeclarator variableDeclarator : expression.asVariableDeclarationExpr().getVariables()) {
                String variableName = variableDeclarator.getNameAsString();
                Type type = variableDeclarator.getType();
                Optional<Expression> optionalInitializer = variableDeclarator.getInitializer();
                if (!optionalInitializer.isPresent()) {
                    continue;
                }

                // Ignoring casts for now
                Optional<Type> castType = Optional.empty();
                Expression value = optionalInitializer.get();

                if (value.isCastExpr()) {
                    CastExpr castExpr = value.asCastExpr();
                    castType = Optional.of(castExpr.getType());
                    value = castExpr.getExpression();
                }

                if (!value.isObjectCreationExpr()) {
                    continue;
                }

                ObjectCreationExpr objectCreationExpr = value.asObjectCreationExpr();
                if (isInterestingObjectAllocation(objectCreationExpr)) {
                    context.caseOfInterestList.add(new ObjectAllocation(
                            classOrInterfaceDeclaration,
                            isClearable(objectCreationExpr.getType()),
                            variableDeclarator, castType.orElse(null), variableName, context));
                }
            }
        }
    }

    @Override
    public void refactorIteration(RefactoringPhaseContext refactoringPhaseContext) {
        // Here we know that we do want to pull the declaration to another place
        if(assignExpr != null) {
            // todo - find the variable declaration and put the initialization code there
        } else {

            // Add a new field to the class
            // todo - should check if the field exists before applying this change, if it does we should ignore
            FieldDeclaration fieldDeclaration = new FieldDeclaration();
            VariableDeclarator variableDeclarator = this.variableDeclarator.clone();
            variableDeclarator.setInitializer(variableDeclarator.getInitializer().get());
            fieldDeclaration.addVariable(variableDeclarator);
            classDeclaration.addMember(fieldDeclaration);
            if(isClearable) {
                // Add clear to the end of the statement
                // todo - account for return statements
                MethodCallExpr methodCallExpr = new MethodCallExpr();
                methodCallExpr.setScope(new NameExpr(variableName));
                methodCallExpr.setName(new SimpleName("clear"));
                Statement lastStatement = refactoringPhaseContext.blockStmt.getStatements()
                        .get(refactoringPhaseContext.blockStmt.getStatements().size() - 1);
                if(lastStatement.isReturnStmt()) {
                    refactoringPhaseContext.blockStmt.addStatement(
                            refactoringPhaseContext.blockStmt.getStatements().size() - 1,  new ExpressionStmt(methodCallExpr));
                } else {
                    refactoringPhaseContext.blockStmt.addStatement(new ExpressionStmt(methodCallExpr));
                }

            }
            // Remove the statement
            refactoringPhaseContext.blockStmt.getStatements().remove(refactoringPhaseContext.offset + statementIndex);
            refactoringPhaseContext.offset -= 1;
        }
    }

}