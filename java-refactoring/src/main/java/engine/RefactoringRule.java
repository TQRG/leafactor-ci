package engine;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import java.util.List;
import java.util.Optional;
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
    static <T extends Node> List<T> getNodesOfInterest(Node root, Predicate<Node> isNodeOfInterest, Class<T> type) {
        Stream<T> a = isNodeOfInterest.test(root) ? Stream.of(type.cast(root)) : Stream.empty();
        Stream<T> b = root.getChildNodes().stream()
                .map(node -> RefactoringRule.getNodesOfInterest(node, isNodeOfInterest, type))
                .flatMap(List::stream);
        return Stream.concat(a, b).collect(Collectors.toList());
    }

    /**
     * Searches the AST to find the deepest outer and inner nodes that match the predicates given
     * @param root The base node for searching
     * @param outerValidator The outer node checker predicate
     * @param innerValidator The inner node checker predicate
     * @return The deepest outer node that matches the outerValidator and innerValidator or null
     */
    static Optional<Node> findOuterNodeOfInterest(Node root, Predicate<Node> outerValidator, Predicate<Node> innerValidator) {
        if(root.getChildNodes().isEmpty()) {
            return Optional.empty();
        }

        // First we process the ones that are not outer because we want the deepest outer nodes
        Optional<Optional<Node>> optional = root.getChildNodes().stream()
                .filter(node -> !outerValidator.test(node))
                .map(node -> findOuterNodeOfInterest(node, outerValidator, innerValidator))
                .filter(Optional::isPresent)
                .findAny();

        // If the deeper nodes were not able to find a matching outer and inner combo, we explore the outers
        return optional.orElseGet(() -> root.getChildNodes().stream()
                .filter(outerValidator)
                .filter(node -> hasNodeOfInterest(node, innerValidator))
                .findAny().or(() -> root.stream()
                        .filter(outerValidator)
                        .filter(node -> hasNodeOfInterest(node, innerValidator))
                        .findAny()
                ));

    }
}
