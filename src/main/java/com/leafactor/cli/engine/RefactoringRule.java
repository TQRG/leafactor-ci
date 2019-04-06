package com.leafactor.cli.engine;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Refactoring rule interface
 */
public interface RefactoringRule {
    /**
     * Applies the refactoring rule to a given compilation unit
     * @param compilationUnit The compilation unit to which the rule will apply
     */
    void apply(CompilationUnit compilationUnit);

    /**
     * Finds a node of interest
     * @param root The baseline node
     * @param isNodeOfInterest A predicate that identifies the interested node
     * @return True if the node of interest was found in the tree, false otherwise
     */
    static boolean hasNodeOfInterest(Node root, Predicate<Node> isNodeOfInterest) {
        return isNodeOfInterest.test(root) ||
                root.getChildNodes().stream()
                        .anyMatch(node -> hasNodeOfInterest(node, isNodeOfInterest));
    }

    /**
     * Finds a node of interest
     * @param root The baseline node
     * @param isNodeOfInterest A predicate that identifies the interested node
     * @return True if the node of interest was found in the tree, false otherwise
     */
    static <T> List<T> getNodesOfInterest(Node root, Predicate<Node> isNodeOfInterest, Class<T> type) {
        Stream<T> a = isNodeOfInterest.test(root) ? Stream.of(type.cast(root)) : Stream.empty();
        Stream<T> b = root.getChildNodes().stream()
                .map(node -> RefactoringRule.getNodesOfInterest(node, isNodeOfInterest, type))
                .flatMap(List::stream);
        return Stream.concat(a, b).collect(Collectors.toList());
    }

    /**
     * Finds a node of interest with filter
     * @param root The baseline node
     * @param isNodeOfInterest A predicate that identifies the interested node
     * @return True if the node of interest was found in the tree, false otherwise
     */
    static <T> List<T> getNodesOfInterestWithFilter(Node root, Predicate<Node> isNodeOfInterest, Predicate<Node> filter, Class<T> type) {
        if(filter.test(root)) {
            return List.of();
        }
        Stream<T> a = isNodeOfInterest.test(root) ? Stream.of(type.cast(root)) : Stream.empty();
        Stream<T> b = root.getChildNodes().stream()
                .map(node -> RefactoringRule.getNodesOfInterestWithFilter(node, isNodeOfInterest, filter, type))
                .flatMap(List::stream);
        return Stream.concat(a, b).collect(Collectors.toList());
    }

    /**
     * The closest BlockStmt by bubbling up
     * @param node The node from which to start
     * @return The closest BlockStmt by bubbling up
     */
    static BlockStmt getClosestBlockStmtParent(Node node) {
        Node root = node.getParentNode().orElse(node);
        while (!(root instanceof BlockStmt) && root.getParentNode().isPresent()) {
            root = root.getParentNode().get();
        }
        if(!(root instanceof BlockStmt)) {
            return null;
        }
        return (BlockStmt)root;
    }

    /**
     * The closest ClassOrInterfaceDeclaration by bubbling up
     * @param node The node from which to start
     * @return The closest ClassOrInterfaceDeclaration by bubbling up
     */
    static ClassOrInterfaceDeclaration getClosestClassOrInterfaceDeclarationParent(Node node) {
        Node root = node.getParentNode().orElse(node);
        while (!(root instanceof ClassOrInterfaceDeclaration) && root.getParentNode().isPresent()) {
            root = root.getParentNode().get();
        }
        if(!(root instanceof ClassOrInterfaceDeclaration)) {
            return null;
        }
        return (ClassOrInterfaceDeclaration)root;
    }

    /**
     * The closest MethodDeclaration by bubbling up
     * @param node The node from which to start
     * @return The closest MethodDeclaration by bubbling up
     */
    static MethodDeclaration getClosestMethodDeclarationParent(Node node) {
        Node root = node.getParentNode().orElse(node);
        while (!(root instanceof MethodDeclaration) && root.getParentNode().isPresent()) {
            root = root.getParentNode().get();
        }
        if(!(root instanceof MethodDeclaration)) {
            return null;
        }
        return (MethodDeclaration)root;
    }
}
