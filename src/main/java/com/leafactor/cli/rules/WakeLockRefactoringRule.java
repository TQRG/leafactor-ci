package com.leafactor.cli.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.leafactor.cli.engine.*;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.rules.GenericCasesOfInterest.VariableDeclared;
import com.leafactor.cli.rules.WakeLockCasesOfInterest.WakeLockAcquire;

import java.util.List;

/**
 * Refactoring rule that applies the Wake lock pattern
 */
public class WakeLockRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule, Iteration {

    private IterationLogger logger;

    public WakeLockRefactoringRule(IterationLogger logger) {
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

    private void refactor(MethodDeclaration methodDeclaration) {
        System.out.println("Signature matches");
        if(!methodDeclaration.getBody().isPresent()) {
            return;
        }
        CaseDetector caseDetector = CaseDetector.CompileCaseDetector(VariableDeclared::detect, WakeLockAcquire::detect);
        Iteration.iterateMethod(this, logger,methodDeclaration, this, caseDetector, false);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg) {
        boolean extendsActivity = classOrInterfaceDeclaration.getExtendedTypes().stream()
                .anyMatch(classOrInterfaceType -> classOrInterfaceType.getNameAsString().equals("Activity"));
        if(extendsActivity) {
            for (Node node : classOrInterfaceDeclaration.getChildNodes()) {
                if (node instanceof MethodDeclaration) {
                    refactor((MethodDeclaration) node);
                }
            }
        }
        super.visit(classOrInterfaceDeclaration, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }
}
