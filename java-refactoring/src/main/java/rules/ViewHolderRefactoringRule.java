package rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import engine.RefactoringRule;

/**
 * Refactoring rule that undoes the Recycle anti-pattern
 */
public class ViewHolderRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        // Todo - Implement rule body
        super.visit(methodDeclaration, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }
}
