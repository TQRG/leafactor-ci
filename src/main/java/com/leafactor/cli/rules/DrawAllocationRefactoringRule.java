package com.leafactor.cli.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.leafactor.cli.engine.*;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.rules.DrawAllocationCasesOfInterest.ObjectAllocation;
import com.leafactor.cli.rules.GenericCasesOfInterest.VariableDeclared;

import java.util.List;

/**
 * Refactoring rule that applies the view holder pattern
 */
public class DrawAllocationRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule, Iteration {

    // TODO - Add other cases of interest:
    // TODO -> getTag()
    // TODO -> convertView == null

    private IterationLogger logger;

    public DrawAllocationRefactoringRule(IterationLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onSetup(DetectionPhaseContext context) {}

    @Override
    public void onWillIterate(DetectionPhaseContext context) {}

    @Override
    public void onDidIterate(DetectionPhaseContext context) {}

    @Override
    public void onWillRefactor(List<CaseOfInterest> caseOfInterests) {}

    @Override
    public void onWillRefactorCase(RefactoringPhaseContext context) {}

    @Override
    public void onDidRefactorCase(RefactoringPhaseContext context) {}

    private boolean methodSignatureMatches(MethodDeclaration methodDeclaration) {
        // public View getView(final int position, final View convertView, final ViewGroup parent)
        boolean nameMatch = methodDeclaration.getNameAsString().equals("onDraw");
        Type type = methodDeclaration.getType();
        boolean returnTypeMatch = type.isVoidType();
        boolean isProtected = methodDeclaration.getModifiers().contains(Modifier.protectedModifier());
        boolean hasSameNumberOfArguments = methodDeclaration.getParameters().size() == 1;
        if (hasSameNumberOfArguments) {
            Type firstArgumentType = methodDeclaration.getParameter(0).getType();
            boolean firstArgumentTypeMatches = firstArgumentType.isClassOrInterfaceType() &&
                    firstArgumentType.asClassOrInterfaceType().getNameAsString().endsWith("Canvas");
            return isProtected &&
                    nameMatch &&
                    returnTypeMatch &&
                    firstArgumentTypeMatches;
        }

        return false;
    }

    private void refactor(MethodDeclaration methodDeclaration) {
        if (!methodSignatureMatches(methodDeclaration)) {
            System.out.println("Signature does not match");
            return;
        }
        System.out.println("Signature matches");
        CaseDetector caseDetector = CaseDetector.CompileCaseDetector(VariableDeclared::detect, ObjectAllocation::detect);
        Iteration.iterateMethod(this, logger, methodDeclaration, this, caseDetector, false);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg) {
        // Todo - check class extension
        for (Node node : classOrInterfaceDeclaration.getChildNodes()) {
            if (node instanceof MethodDeclaration) {
                refactor((MethodDeclaration) node);
            }
        }
        super.visit(classOrInterfaceDeclaration, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }

}
