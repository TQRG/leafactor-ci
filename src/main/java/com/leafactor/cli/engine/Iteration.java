package com.leafactor.cli.engine;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.ArrayList;
import java.util.List;

public interface Iteration {

    void onSetup(IterationContext context);

    void onWillIterate(IterationContext context);

    void onDidIterate(IterationContext context);

    void onWillRefactor(List<CaseOfInterest> caseOfInterests);

    void onWillRefactorCase(RefactoringIterationContext context);

    void onDidRefactorCase(RefactoringIterationContext context);

    static void iterateMethod(RefactoringRule rule, IterationLogger logger, MethodDeclaration methodDeclaration, Iteration iteration, CaseDetector detector, boolean isDeep) {
        if(!methodDeclaration.getBody().isPresent()) {
            return;
        }
        iterateBlock(rule, logger, methodDeclaration.getBody().get(), iteration, detector, isDeep, 0);
    }

    static void iterateBlock(RefactoringRule rule, IterationLogger logger, BlockStmt blockStmt, Iteration iteration, CaseDetector detector, boolean isDeep, int depth) {
        SimpleIterationPhaseLogEntry setupLogEntry = new SimpleIterationPhaseLogEntry(rule, "Setting up iteration", "TODO - ADD meaningful information");

        // SETUP
        setupLogEntry.start();
        SimpleIterationPhaseLogEntry detectionPhaseLogEntry = new SimpleIterationPhaseLogEntry(rule, "Detecting Patterns", "TODO - ADD meaningful information");
        SimpleIterationPhaseLogEntry refactoringPhaseLogEntry = new SimpleIterationPhaseLogEntry(rule, "Refactoring Cases of Interest", "TODO - ADD meaningful information");
        IterationContext context = new IterationContext();
        context.blockStmt = blockStmt;
        iteration.onSetup(context);
        setupLogEntry.stop();

        // DETECTION PHASE
        detectionPhaseLogEntry.start();
        for (int i = 0; i < blockStmt.getStatements().size(); i++) {
            context.statement = blockStmt.getStatements().get(i);
            context.statementIndex = i;
            iteration.onWillIterate(context);
            if (isDeep && context.statement instanceof NodeWithOptionalBlockStmt) {
                NodeWithOptionalBlockStmt nodeWithOptionalBlockStmt = (NodeWithOptionalBlockStmt)context.statement;
                if(nodeWithOptionalBlockStmt.getBody().isPresent()) {
                    BlockStmt innerBlock = (BlockStmt)nodeWithOptionalBlockStmt.getBody().get();
                    Iteration.iterateBlock(rule, logger, innerBlock, iteration, detector, true, depth + 1);
                    // Todo: do something with the innerContext
                }
            }
            detector.detect(context);
            iteration.onDidIterate(context);
        }
        detectionPhaseLogEntry.stop();

        // Refactoring Phase
        refactoringPhaseLogEntry.start();
        iteration.onWillRefactor(context.caseOfInterests);
        RefactoringIterationContext refactoringIterationContext = new RefactoringIterationContext();
        List<CaseOfInterest> copy = new ArrayList<>(context.caseOfInterests);
        refactoringIterationContext.offset = 0;
        refactoringIterationContext.blockStmt = blockStmt;
        refactoringIterationContext.caseOfInterests = copy;
        for (CaseOfInterest caseOfInterest : copy) {
            refactoringIterationContext.caseOfInterest = caseOfInterest;
            iteration.onWillRefactorCase(refactoringIterationContext);
            caseOfInterest.refactorIteration(refactoringIterationContext);
            iteration.onDidRefactorCase(refactoringIterationContext);
        }
        refactoringPhaseLogEntry.stop();

        logger.getLogs().add(setupLogEntry);
        logger.getLogs().add(detectionPhaseLogEntry);
        logger.getLogs().add(refactoringPhaseLogEntry);
    }
}
