package tqrg.leafactor.ci.rules.Cases;

import tqrg.leafactor.ci.engine.CaseOfInterest;
import tqrg.leafactor.ci.engine.DetectionPhaseContext;
import spoon.reflect.declaration.CtVariable;

public class VariableDeclared extends CaseOfInterest {
    final public CtVariable variable;

    private VariableDeclared(CtVariable variable, DetectionPhaseContext context) {
        super(context);
        this.variable = variable;
    }

    public static VariableDeclared detect(DetectionPhaseContext context) {
        if (context.statement instanceof CtVariable) {
            CtVariable variable = (CtVariable) context.statement;
            return new VariableDeclared(variable, context);
        }
        return null;
    }

    @Override
    public String toString() {
        return "VariableDeclared{" +
                "variable=" + variable +
                ", index=" + index +
                ", statementIndex=" + statementIndex +
                ", statement=" + statement +
                '}';
    }
}