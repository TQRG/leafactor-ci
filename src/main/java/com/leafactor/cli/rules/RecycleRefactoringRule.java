package com.leafactor.cli.rules;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalScope;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.engine.RefactoringRule;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO - Change to Iteration

/**
 * Refactoring rule that undoes the Recycle anti-pattern
 */
public class RecycleRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {

    // List of classes that need to be recycled
    private Map<String, String> opportunities = new LinkedHashMap<>();
    private List<Predicate<MethodCallExpr>> exceptions = new ArrayList<>();
    private IterationLogger logger;

    public RecycleRefactoringRule(IterationLogger logger) {
        this.logger = logger;
        opportunities.put("TypedArray", "recycle");
        opportunities.put("Bitmap", "recycle");
        opportunities.put("Cursor", "close");
        opportunities.put("VelocityTracker", "recycle");
        opportunities.put("Message", "recycle");
        opportunities.put("MotionEvent", "recycle");
        opportunities.put("Parcel", "recycle");
        opportunities.put("ContentProviderClient", "release");

        // We already know that the variable is being sent in this methodCallExpr as an argument
        exceptions.add(methodCallExpr -> {
            Optional<Expression> scope = methodCallExpr.getScope();
            return scope.isPresent()
                    && scope.get() instanceof NameExpr
                    && scope.get().asNameExpr().getName().getIdentifier().equals("MotionEvent")
                    && methodCallExpr.getName().getIdentifier().equals("obtain");
        });
    }

    private Map<String, String> getVariablesOfInterestDeclaredNarrow(VariableDeclarationExpr expression) {

        List<VariableDeclarator> filtered = expression.getVariables().stream()
                .filter((element) -> element.getType().stream()
                        .filter(ClassOrInterfaceType.class::isInstance)
                        .map(type -> (ClassOrInterfaceType) type)
                        .map(ClassOrInterfaceType::getName)
                        .anyMatch(simpleName -> opportunities.keySet().contains(simpleName.getIdentifier()))).collect(Collectors.toList());
        Map<String, String> mapIdentifierToType = new LinkedHashMap<>();
        filtered.forEach(element -> {
            String identifier = element.getName().getIdentifier();
            String type = element.getType().asClassOrInterfaceType().getName().getIdentifier();
            mapIdentifierToType.put(identifier, type);
        });
        return mapIdentifierToType;
    }

    /**
     *
     * @param expression The expression to analyse
     * @return A map where the key is the name of the variable and the value is the value assigned
     */
    private Map<String, Node> getVariablesOfInterestReassignedNarrow(ExpressionStmt expression, Collection<String> declaredVariables) {
        List<AssignExpr> assignments = Stream.of(expression.getExpression())
                .filter(AssignExpr.class::isInstance)
                .map(AssignExpr.class::cast)
                .collect(Collectors.toList());

        List<AssignExpr> filtered = assignments.stream()
                .filter((element) -> element.getTarget().stream()
                        .filter(NameExpr.class::isInstance)
                        .map(NameExpr.class::cast)
                        .map(NameExpr::getName)
                        .anyMatch(simpleName -> declaredVariables.contains(simpleName.getIdentifier())))
                .collect(Collectors.toList());

        Map<String, Node> mapIdentifierToValue = new LinkedHashMap<>();
        filtered.forEach(element -> {
            String identifier = element.getTarget().asNameExpr().getName().getIdentifier();
            mapIdentifierToValue.put(identifier, element.getValue());
        });
        return mapIdentifierToValue;
    }

    private boolean wasRecycled(Node root, String variableName, String recyclingMethodName) {
        return RefactoringRule.hasNodeOfInterest(root,
                node -> {
                    boolean scopeMatchesVariableName = Stream.of(node)
                            .filter(MethodCallExpr.class::isInstance)
                            .map(MethodCallExpr.class::cast)
                            .map(MethodCallExpr::getScope)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(NameExpr.class::isInstance)
                            .map(element -> (NameExpr) element)
                            .map(NameExpr::getName)
                            .anyMatch(name -> name.getIdentifier().equals(variableName));

                    boolean callIsRecycle = Stream.of(node)
                            .filter(MethodCallExpr.class::isInstance)
                            .map(MethodCallExpr.class::cast)
                            .map(MethodCallExpr::getName)
                            .filter(SimpleName.class::isInstance)
                            .map(SimpleName.class::cast)
                            .anyMatch(name -> name.getIdentifier().equals(recyclingMethodName));

                    return scopeMatchesVariableName && callIsRecycle;
                });
    }

    private void refactor(BlockStmt root) {
        // Variables declared in this scope
        Map<String, String> variableDeclared = new LinkedHashMap<>();
        Map<String, String> variableRedeclared = new LinkedHashMap<>();
        Map<String, Integer> variableLastRedeclaration = new LinkedHashMap<>();
        Map<String, Integer> variableLastUsage = new LinkedHashMap<>();
        int index = 0;
        for(Statement statement : root.getStatements()) {
            final int currentIndex = index;
            if(statement.isExpressionStmt() && statement.asExpressionStmt().getExpression() instanceof VariableDeclarationExpr) {
                // Get the variables declared in this statement if any
                Map<String, String> newVariables = getVariablesOfInterestDeclaredNarrow((VariableDeclarationExpr) statement.asExpressionStmt().getExpression());
                for (String name : newVariables.keySet()) {
                    variableLastUsage.put(name, currentIndex);
                }
                variableDeclared.putAll(newVariables); // Add them to the list of variables
            } else if(statement.isExpressionStmt() && statement.asExpressionStmt().getExpression() instanceof AssignExpr) {
                // Find redeclarations
                Map<String, Node> redeclarations = getVariablesOfInterestReassignedNarrow(statement.asExpressionStmt(), variableDeclared.keySet());
                for(String name : redeclarations.keySet()) {
                    // We want to check the last usage first, in order to pull the recycle call as up as possible
                    variableRedeclared.put(name, variableDeclared.get(name));
                    variableLastRedeclaration.put(name, variableLastUsage.getOrDefault(name, currentIndex - 1));
                    variableLastUsage.put(name, currentIndex); // This needs to be after the previous statement
                }
            }

            Iterator<String> iterator = variableDeclared.keySet().iterator();
            while(iterator.hasNext()) {
                String variableName = iterator.next();
                if (!hasVariableUsages(statement, variableName)) {
                    continue;
                }
                // Variable has usages, lets check if the reference was lost
                if(!isVariableUnderControl(statement, variableName)) {
                    // The scope no longer has control over the variable reference... ignore it
                    iterator.remove();
                    variableLastUsage.remove(variableName);
                    // Notice that redeclarations remain
                } else if(wasRecycled(statement, variableName, opportunities.get(variableDeclared.get(variableName)))) {
                    iterator.remove();
                    variableLastUsage.remove(variableName);
                    // Notice that redeclarations remain
                } else {
                    // Variable was used in this statement and remains under control
                    variableLastUsage.put(variableName, currentIndex);
                }
            }
            index++;
        }

        // Apply refactoring recycle calls
        Stream.concat(variableLastUsage.entrySet().stream(), variableLastRedeclaration.entrySet().stream())
                // Order by descending value to avoid shifting indexes
                .sorted((Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) -> o2.getValue() - o1.getValue())
                .forEach((entry) -> {
                    String type = variableDeclared.get(entry.getKey());
                    if(type == null) {
                        type = variableRedeclared.get(entry.getKey());
                    }
                    int spacing = root.getEnd().get().column -1;
                    Statement newStatement = createRecycleExpression(spacing / 4 + 1, entry.getKey(),
                            opportunities.get(type));

                    root.addStatement(entry.getValue() + 1, newStatement);
                });
    }

    /**
     * Finds every variable usage
     * @param root The root node
     * @param variableName The name of the variable to search
     * @return A list of NodeWithSimpleName with the same name as the variable
     */
    private boolean hasVariableUsages(Node root, String variableName) {
        return RefactoringRule.hasNodeOfInterest(root,
                node -> {

                    // Check if a method of the variable is being called somewhere
                    if(Stream.of(node)
                            .filter(NodeWithOptionalScope.class::isInstance)
                            .map(NodeWithOptionalScope.class::cast)
                            .map(NodeWithOptionalScope::getScope)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(NameExpr.class::isInstance)
                            .map(element -> (NameExpr) element)
                            .map(NameExpr::getName)
                            .anyMatch(name -> name.getIdentifier().equals(variableName)
                            )){
                        return true;
                    }

                    // Check if the variable is being returned somewhere
                    if(Stream.of(node)
                            .filter(ReturnStmt.class::isInstance)
                            .map(ReturnStmt.class::cast)
                            .map(ReturnStmt::getExpression)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(NameExpr.class::isInstance)
                            .map(nameExpr -> (NameExpr) nameExpr)
                            .map(NameExpr::getName)
                            .anyMatch(name -> name.getIdentifier().equals(variableName))
                            ){
                        return true;
                    }

                    // Check if the variable is being sent through a method call
                    if(Stream.of(node)
                            .filter(MethodCallExpr.class::isInstance)
                            .map(MethodCallExpr.class::cast)
                            .map(MethodCallExpr::getArguments)
                            .flatMap(Collection::stream)
                            .filter(NameExpr.class::isInstance)
                            .map(nameExpr -> (NameExpr) nameExpr)
                            .map(NameExpr::getName)
                            .anyMatch(name -> name.getIdentifier().equals(variableName))
                            ){
                        return true;
                    }

                    // todo - being assigned to another variable.

                    return false;
                });
    }

    /**
     * Check if a variable is still under control
     * @param root The root node to start the search
     * @param variableName The name of the variable to test
     * @return True if the variable remains under control, false otherwise
     */
    private boolean isVariableUnderControl(Node root, String variableName) {
        // Finding Lambdas
        List<LambdaExpr> lambdaExprs = RefactoringRule.getNodesOfInterest(root, LambdaExpr.class::isInstance, LambdaExpr.class);

        // Lost variable reference, we no longer know when the variable will be used in the lambda
        boolean usedInsideLambda = lambdaExprs.stream().anyMatch(lambdaExpr -> hasVariableUsages(lambdaExpr, variableName));
        if(usedInsideLambda) {
            return false;
        }

        List<ReturnStmt> returnStmts = RefactoringRule.getNodesOfInterestWithFilter(root, ReturnStmt.class::isInstance, BlockStmt.class::isInstance, ReturnStmt.class);
        if(root instanceof ReturnStmt) {
            returnStmts.add((ReturnStmt) root);
        }

        boolean variableWasReturned = returnStmts.stream().map(ReturnStmt::getExpression)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(NameExpr.class::isInstance)
                .map(nameExpr -> (NameExpr) nameExpr)
                .map(NameExpr::getName)
                .anyMatch(name -> name.getIdentifier().equals(variableName));

        // Lost variable reference as it was returned
        if(variableWasReturned) {
            return false;
        }

        boolean sentAsArgument = RefactoringRule.hasNodeOfInterest(root, (node) -> Stream.of(node)
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .filter(methodCallExpr -> {
                  Optional<Boolean> hasExceptionsOptional = exceptions.stream()
                          .map(methodCallExprPredicate -> methodCallExprPredicate.test(methodCallExpr))
                          .reduce(Boolean::logicalOr);
                  return !(hasExceptionsOptional.isPresent() && hasExceptionsOptional.get());
                })
                .map(MethodCallExpr::getArguments)
                .flatMap(Collection::stream)
                .filter(NameExpr.class::isInstance)
                .map(nameExpr -> (NameExpr) nameExpr)
                .map(NameExpr::getName)
                .anyMatch(name -> name.getIdentifier().equals(variableName)));

        // Lost variable reference
        if(sentAsArgument) {
            return false;
        }

        List<BlockStmt> blocks = RefactoringRule.getNodesOfInterest(root, BlockStmt.class::isInstance, BlockStmt.class);

        // Lost variable reference (Recycling this variable is no longer a concern for the root block)
        boolean hasBlocksWithRedeclarations = blocks.stream()
                .anyMatch(node ->  RefactoringRule.hasNodeOfInterest(node, element -> {
                    List<AssignExpr> assignments = Stream.of(element)
                            .filter(AssignExpr.class::isInstance)
                            .map(assign -> (AssignExpr) assign).collect(Collectors.toList());

                    return assignments.stream().map(AssignExpr::getTarget).filter(NameExpr.class::isInstance)
                            .map(nameExpr -> (NameExpr) nameExpr)
                            .map(NameExpr::getName)
                            .anyMatch(name -> name.getIdentifier().equals(variableName));
                }));

        return !hasBlocksWithRedeclarations;
    }


    /**
     * Create an expression to recycle a given variable with a give recycling method
     * @param tabs The number of tabs for indentation purposes
     * @param variableName The name of the variable to be recycled
     * @param recyclingMethodName The name of the function to be called to recycle the object
     * @return The statement constructed with for recycling the variable
     */
    private Statement createRecycleExpression(int tabs, String variableName, String recyclingMethodName) {
        String tab = "    ";
        StringBuilder baseTabPadding = new StringBuilder(tabs > 0 ? tab : "");
        for (int i = 1; i < tabs; i++) {
            baseTabPadding.append(tab);
        }
        System.out.println("[" + baseTabPadding + "]");
        String EOL = System.getProperty("line.separator");

        return LexicalPreservingPrinter.setup(
                StaticJavaParser.parseStatement(
                        String.format("if(%s != null) {" + EOL +
                                        baseTabPadding + tab + "%s.%s();" + EOL +
                                        baseTabPadding + "}" + EOL,
                                variableName,
                                variableName,
                                recyclingMethodName)));
    }


    @Override
    public void visit(BlockStmt blockStmt, Void arg) {
        refactor(blockStmt);
        super.visit(blockStmt, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }
}