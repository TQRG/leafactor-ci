package tqrg.leafactor.ci.rules.ViewHolderCases;

import tqrg.leafactor.ci.engine.CaseOfInterest;
import tqrg.leafactor.ci.engine.DetectionPhaseContext;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.reference.CtVariableReference;
import tqrg.leafactor.ci.engine.CaseOfInterest;
import tqrg.leafactor.ci.engine.DetectionPhaseContext;

import java.util.List;


public class VariableAssignedFindViewById extends CaseOfInterest {
    final public CtVariableReference variable; // The variable that is being assigned
    final public CtExpression resource;

    private VariableAssignedFindViewById(CtVariableReference variable,
                                         CtExpression resource,
                                         DetectionPhaseContext context) {
        super(context);
        this.variable = variable;
        this.resource = resource;
    }

    public static VariableAssignedFindViewById detect(DetectionPhaseContext context) {
        CtExpression assignmentExpression;
        CtVariableReference variableReference;
        if (context.statement instanceof CtVariable) {
            assignmentExpression = ((CtVariable) context.statement).getDefaultExpression();
            variableReference = ((CtVariable) context.statement).getReference();
        } else if (context.statement instanceof CtAssignment) {
            CtAssignment assignment = (CtAssignment) context.statement;
            assignmentExpression = assignment.getAssignment();
            CtExpression assignedExpression = assignment.getAssigned();
            if (!(assignedExpression instanceof CtVariableWrite)) {
                return null;
            }
            variableReference = ((CtVariableWrite) assignedExpression).getVariable();
        } else {
            return null;
        }

        CtInvocation invocation;
        if (assignmentExpression instanceof CtInvocation) {
            invocation = (CtInvocation) assignmentExpression;
        } else {
            return null;
        }

        boolean isInflateCall = invocation.getExecutable().getSimpleName().equals("findViewById");
        boolean argumentsMatch = invocation.getArguments().size() == 1;
        if (isInflateCall && argumentsMatch) {
            // Here we know that we are calling method with the same signature
            List<CtExpression> expressionList = invocation.getArguments();
            return new VariableAssignedFindViewById(variableReference, expressionList.get(0), context);
        }
        return null;
    }

    @Override
    public String toString() {
        return "VariableAssignedFindViewById{" +
                "variable=" + variable +
                ", index=" + index +
                ", statementIndex=" + statementIndex +
                ", statement=" + statement +
                '}';
    }
}