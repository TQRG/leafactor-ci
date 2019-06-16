package com.leafactor.cli.rules;

import com.leafactor.cli.engine.*;
import com.leafactor.cli.engine.logging.IterationLogger;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;

import java.util.*;


public class WakeLockRefactoringRule extends AbstractProcessor<CtClass> implements RefactoringRule<CtClass> {
    private IterationLogger logger;

    public WakeLockRefactoringRule(IterationLogger logger) {
        this.logger = logger;
    }

    @Override
    public void detectCase(DetectionPhaseContext context) {

    }

    @Override
    public void transformCase(TransformationPhaseContext context) {

    }

    @Override
    public void processCase(RefactoringPhaseContext context) {

    }

    private void refactor(CtMethod method) {
        List<CtBlock> blocks = RefactoringRule.getCtElementsOfInterest(method, CtBlock.class::isInstance, CtBlock.class);
        for (CtBlock block : blocks) {
            Iteration.iterateBlock(this, logger, block,false, 0);
        }
    }

    public void process(CtClass element) {
        Set methods = element.getMethods();
        for (Object method : methods) {
            if (method instanceof CtMethod) {
                refactor((CtMethod) method);
            }
        }
    }

    @Override
    public void onSetup(DetectionPhaseContext context) {

    }

    @Override
    public void onWillIterate(DetectionPhaseContext context) {

    }

    @Override
    public void onDidIterate(DetectionPhaseContext context) {

    }

    @Override
    public void onWillTransform(TransformationPhaseContext context) {

    }

    @Override
    public void onWillTransformCase(TransformationPhaseContext context) {

    }

    @Override
    public void onDidTransformCase(TransformationPhaseContext context) {

    }

    @Override
    public void onWillRefactor(RefactoringPhaseContext context) {

    }

    @Override
    public void onWillRefactorCase(RefactoringPhaseContext context) {

    }

    @Override
    public void onDidRefactorCase(RefactoringPhaseContext context) {

    }
}