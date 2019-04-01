package com.leafactor.cli.engine;

import java.time.Instant;

public interface IterationLogEntry {
    RefactoringRule getRule();
    String getName();
    String getDescription();
    Instant getTimeStamp();
}
