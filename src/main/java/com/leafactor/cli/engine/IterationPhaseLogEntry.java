package com.leafactor.cli.engine;

import java.time.Duration;
import java.time.Instant;

public interface IterationPhaseLogEntry extends IterationLogEntry {
    Instant getStartPhaseTimestamp();
    Instant getEndPhaseTimestamp();
    Duration getPhaseDuration();
}
