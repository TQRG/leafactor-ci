package com.leafactor.cli.engine;

import java.time.Duration;
import java.time.Instant;

public class SimpleIterationPhaseLogEntry implements IterationPhaseLogEntry {
    private RefactoringRule rule;
    private String name;
    private String description;
    private Instant startPhaseTimestamp;
    private Instant endPhaseTimestamp;
    private Duration phaseDuration;

    public SimpleIterationPhaseLogEntry(RefactoringRule rule, String name, String description) {
        this.rule = rule;
        this.name = name;
        this.description = description;
    }

    public void start() {
        startPhaseTimestamp = Instant.now();
        endPhaseTimestamp = null;
        phaseDuration = null;
    }

    public void stop() {
        endPhaseTimestamp = Instant.now();
        phaseDuration = Duration.between(startPhaseTimestamp, endPhaseTimestamp);
    }

    @Override
    public RefactoringRule getRule() {
        return rule;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Instant getTimeStamp() {
        return startPhaseTimestamp;
    }

    @Override
    public Instant getStartPhaseTimestamp() {
        return startPhaseTimestamp;
    }

    @Override
    public Instant getEndPhaseTimestamp() {
        return endPhaseTimestamp;
    }

    @Override
    public Duration getPhaseDuration() {
        return phaseDuration;
    }
}
