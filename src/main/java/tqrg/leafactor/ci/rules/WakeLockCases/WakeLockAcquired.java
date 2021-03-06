package tqrg.leafactor.ci.rules.WakeLockCases;

import tqrg.leafactor.ci.engine.CaseOfInterest;
import tqrg.leafactor.ci.engine.DetectionPhaseContext;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableRead;
import tqrg.leafactor.ci.engine.CaseOfInterest;
import tqrg.leafactor.ci.engine.DetectionPhaseContext;

public class WakeLockAcquired extends CaseOfInterest {
    final public CtVariableRead variable;

    private WakeLockAcquired(CtVariableRead variable, DetectionPhaseContext context) {
        super(context);
        this.variable = variable;
    }

    public static WakeLockAcquired detect(DetectionPhaseContext context) {
        if (context.statement instanceof CtInvocation) {
            CtInvocation invocation = (CtInvocation) context.statement;
            if (!invocation.getExecutable().getSimpleName().equals("acquire")) {
                return null;
            }
            if (invocation.getTarget() instanceof CtVariableRead) {
                return new WakeLockAcquired((CtVariableRead) invocation.getTarget(), context);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "WakeLockAcquired";
    }
}