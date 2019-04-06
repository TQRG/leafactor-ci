package com.leafactor.cli.engine;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.engine.logging.SimpleIterationPhaseLogEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an iteration over the block of statements
 */
public interface Iteration {

    /**
     * Lifecycle method that happens before the iteration starts. Helpful for setups.
     * @param context The detection phase context
     */
    void onSetup(DetectionPhaseContext context);

    /**
     * Lifecycle method that happens in an individual iteration of the statements on the detection phase
     * and before detecting cases of interest in the particular iteration.
     * @param context The detection phase context
     */
    void onWillIterate(DetectionPhaseContext context);

    /**
     * Lifecycle method that happens in an individual iteration of the statements on the detection phase
     * and after detecting cases of interest in the particular iteration.
     * @param context The detection phase context
     */
    void onDidIterate(DetectionPhaseContext context);

    /**
     * Lifecycle method that happens when the detection phase ends and the refactoring phase starts.
     * @param caseOfInterests A list of cases of interest detected overall
     */
    void onWillRefactor(List<CaseOfInterest> caseOfInterests);

    /**
     * Lifecycle method that happens in an individual iteration of the cases of interest found on the detection phase
     * and before refactoring is applied for that particular case of interest.
     * @param context The refactoring phase context
     */
    void onWillRefactorCase(RefactoringPhaseContext context);

    /**
     * Lifecycle method that happens in an individual iteration of the cases of interest found on the detection phase
     * and after refactoring is applied for that particular case of interest.
     * @param context The refactoring phase context
     */
    void onDidRefactorCase(RefactoringPhaseContext context);

    /**
     * Iterates over a particular method
     * @param rule The refactoring rule that is applied
     * @param logger The logger for entry logs and performance data
     * @param methodDeclaration The node of the method declaration
     * @param iteration The iteration definition
     * @param detector The detector that will be used in the detection phase
     * @param isDeep A flag that describes whether the search will go into inner blockStmt's or not.
     */
    static void iterateMethod(RefactoringRule rule, IterationLogger logger, MethodDeclaration methodDeclaration, Iteration iteration, CaseDetector detector, boolean isDeep) {
        if(!methodDeclaration.getBody().isPresent()) {
            return;
        }
        iterateBlock(rule, logger, methodDeclaration.getBody().get(), iteration, detector, isDeep, 0);
    }

    /**
     * Iterates over a particular method
     * @param rule The refactoring rule that is applied
     * @param logger The logger for entry logs and performance data
     * @param blockStmt The node of the method declaration
     * @param iteration The iteration definition
     * @param detector The detector that will be used in the detection phase
     * @param isDeep A flag that describes whether the search will go into inner blockStmt's or not.
     * @param depth An integer representing the current depth of the search
     */
    static void iterateBlock(RefactoringRule rule, IterationLogger logger, BlockStmt blockStmt, Iteration iteration, CaseDetector detector, boolean isDeep, int depth) {
        SimpleIterationPhaseLogEntry setupLogEntry = new SimpleIterationPhaseLogEntry(rule, "Setting up iteration", "TODO - ADD meaningful information");

        // SETUP
        setupLogEntry.start();
        SimpleIterationPhaseLogEntry detectionPhaseLogEntry = new SimpleIterationPhaseLogEntry(rule, "Detecting Patterns", "TODO - ADD meaningful information");
        SimpleIterationPhaseLogEntry refactoringPhaseLogEntry = new SimpleIterationPhaseLogEntry(rule, "Refactoring Cases of Interest", "TODO - ADD meaningful information");
        DetectionPhaseContext context = new DetectionPhaseContext();
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
//                    Iteration.iterateBlock(rule, logger, innerBlock, iteration, detector, true, depth + 1);
                    // Todo: do something with the innerContext
                }
            }
            detector.detect(context);
            iteration.onDidIterate(context);
        }
        detectionPhaseLogEntry.stop();

        // REFACTORING PHASE
        refactoringPhaseLogEntry.start();
        iteration.onWillRefactor(context.caseOfInterestList);
        RefactoringPhaseContext refactoringPhaseContext = new RefactoringPhaseContext();
        List<CaseOfInterest> copy = new ArrayList<>(context.caseOfInterestList);
        refactoringPhaseContext.offset = 0;
        refactoringPhaseContext.blockStmt = blockStmt;
        refactoringPhaseContext.caseOfInterests = copy;
        for (CaseOfInterest caseOfInterest : copy) {
            refactoringPhaseContext.caseOfInterest = caseOfInterest;
            iteration.onWillRefactorCase(refactoringPhaseContext);
            caseOfInterest.refactorIteration(refactoringPhaseContext);
            iteration.onDidRefactorCase(refactoringPhaseContext);
        }
        refactoringPhaseLogEntry.stop();

        logger.getLogs().add(setupLogEntry);
        logger.getLogs().add(detectionPhaseLogEntry);
        logger.getLogs().add(refactoringPhaseLogEntry);
    }
}
