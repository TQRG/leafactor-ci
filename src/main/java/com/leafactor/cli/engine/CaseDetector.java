package com.leafactor.cli.engine;

public interface CaseDetector {
    // Detects cases of interest
    void detect(IterationContext context);

    static CaseDetector CompileCaseDetector(CaseDetector ... caseDetectorList) {
        return context -> {
            for(CaseDetector caseDetector : caseDetectorList) {
                caseDetector.detect(context);
            }
        };
    }
}
