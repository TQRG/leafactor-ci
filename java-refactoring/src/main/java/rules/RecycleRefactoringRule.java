package rules;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import utility.Color;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Refactoring rule that undoes the Recycle anti-pattern
 */
public class RecycleRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {

    // List of classes that need to be recycled
    private List<String> opportunities = List.of("TypedArray", "Bitmap");

    /**
     * Find the TypedArray/Bitmap variable declaration
     * @param methodDeclaration The root method declaration
     * @return List of variable declarations nodes of type either TypeArray or Bitmap
     */
    private List<VariableDeclarator> findVariableDeclaratorNodes(MethodDeclaration methodDeclaration) {
        return RefactoringRule.getNodesOfInterest(methodDeclaration,
                node -> Stream.of(node)
                        .filter(VariableDeclarator.class::isInstance)
                        .map(element -> (VariableDeclarator) element)
                        .map(VariableDeclarator::getType)
                        .filter(ClassOrInterfaceType.class::isInstance)
                        .map(element -> (ClassOrInterfaceType) element)
                        .map(ClassOrInterfaceType::getName)
                        .anyMatch(simpleName -> opportunities.contains(simpleName.getIdentifier())), VariableDeclarator.class);
    }

    /**
     * Find the recycle call within the method and check if its called on the given declared variable
     * @param methodDeclaration The method declaration function
     * @param variableDeclarator The variable declaration
     * @return True if there is a matching recycle method call false otherwise
     */
    private boolean hasRecycle(MethodDeclaration methodDeclaration, VariableDeclarator variableDeclarator) {
        return RefactoringRule.hasNodeOfInterest(methodDeclaration,
                node -> node.stream()
                        .filter(MethodCallExpr.class::isInstance)
                        .map(element -> (MethodCallExpr) element)
                        .anyMatch((element) -> {
                            return element.getScope()
                                    .stream()
                                    .filter(NodeWithSimpleName.class::isInstance)
                                    .map((var) -> (NodeWithSimpleName) var)
                                    .anyMatch((NodeWithSimpleName var) ->
                                            variableDeclarator.getName().equals(var.getName())
                                                    && element.getName().getIdentifier().equals("recycle"));

                        }));
    }

    private Statement createRecycleExpression(int tabs, String variableName) {
        String tab = "    ";
        String baseTabPadding = tabs > 0 ? "  " : "";
        for(int i = 1; i < tabs; i++) {
            baseTabPadding += baseTabPadding;
        }
        return LexicalPreservingPrinter.setup(
                JavaParser.parseStatement(
                        String.format(
                                System.getProperty("line.separator") +
                                "if(%s != null) {" + System.getProperty("line.separator") +
                                        baseTabPadding + tab + "%s.recycle();" + System.getProperty("line.separator") +
                                        baseTabPadding + "}",
                                variableName,
                                variableName)));
    }

    private void refactor(MethodDeclaration methodDeclaration, List<VariableDeclarator> variableDeclaratorsWithoutRecycle) {
        variableDeclaratorsWithoutRecycle
                .forEach((declaration)-> {
                    methodDeclaration.getBody()
                            .ifPresent(blockStmt -> {
                                blockStmt.addStatement(
                                        createRecycleExpression(3, declaration.getName().getIdentifier()));
                            });
                });
    }


    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {

        List<VariableDeclarator> variableDeclaratorsWithoutRecycle = findVariableDeclaratorNodes(methodDeclaration)
                .stream()
                // Find the variable declarations that do not have a corresponding recycle method
                .filter((variable) -> !hasRecycle(methodDeclaration, variable))
                .collect(Collectors.toList());

        if(variableDeclaratorsWithoutRecycle.size() > 0) {
            System.out.println(Color.RED);
            System.out.println("[RECYCLE_PATTERN] Found variable declarations with no matching recycle call: \n > " + variableDeclaratorsWithoutRecycle);
            System.out.println("[RECYCLE_PATTERN] Possibilities for refactoring: " + variableDeclaratorsWithoutRecycle.size());
            System.out.println(Color.RESET);
            // We have reached a condition for refactoring
            refactor(methodDeclaration, variableDeclaratorsWithoutRecycle);
            // Todo - Check if the control flow for a particular method with a TypedArray/Bitmap always lead to a recycle call.
            // Todo - Add the call to recycle for every variable declaration that does not have a recycle call.
            // Todo - Detect lambda expressions (LambdaExpr) Note: use findOuterNodeOfInterest with outer predicate LambdaExpr.
            // Todo - Are there other objects other than the TypedArray and Bitmap?
            // Todo - What if the variable is sent to another method (possibly outside the current file)?
            // Todo - What if the return value of the method is used directly without reaching for a new variable.
            // Todo - Tabs over spaces (for now we use spaces)
        } else {
            System.out.println(Color.GREEN);
            System.out.println("[RECYCLE_PATTERN] No Refactoring opportunities found.");
            System.out.println(Color.RESET);
        }
        super.visit(methodDeclaration, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }
}
