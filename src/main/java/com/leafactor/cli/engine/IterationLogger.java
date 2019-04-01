package com.leafactor.cli.engine;

import java.util.Collection;
import java.util.LinkedList;

public class IterationLogger {
    private Collection<IterationLogEntry> logs;

    public IterationLogger() {
        this.logs = new LinkedList<>();
    }

    public Collection<IterationLogEntry> getLogs() {
        return logs;
    }
}
