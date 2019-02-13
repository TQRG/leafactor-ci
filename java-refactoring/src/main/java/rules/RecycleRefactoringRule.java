package rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Refactoring rule that undoes the Recycle anti-pattern
 */
public class RecycleRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {
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
                        .anyMatch(simpleName -> simpleName.getIdentifier().equals("TypedArray")
                                || simpleName.getIdentifier().equals("Bitmap")), VariableDeclarator.class);
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

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        List<VariableDeclarator> variableDeclaratorsWithoutRecycle = findVariableDeclaratorNodes(methodDeclaration)
                .stream()
                // Find the variable declarations that do not have a corresponding recycle method
                .filter((variable) -> !hasRecycle(methodDeclaration, variable))
                .collect(Collectors.toList());
        System.out.println("[RECYCLE_PATTERN]Found variable Declarators without Recycle: \n > " + variableDeclaratorsWithoutRecycle);
        if(variableDeclaratorsWithoutRecycle.size() > 0) {
            System.out.println("[RECYCLE_PATTERN]POSSIBILITIES FOR REFACTORING: " + variableDeclaratorsWithoutRecycle.size());
        }
        // todo - check if the control flow for a particular method with a TypedArray/Bitmap always lead to a recycle call
        // todo - add the call to recycle for every variable declaration that does not have a recycle call
        super.visit(methodDeclaration, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }
}
