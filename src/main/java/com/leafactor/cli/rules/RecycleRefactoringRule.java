package com.leafactor.cli.rules;

import com.leafactor.cli.engine.*;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.rules.Cases.VariableDeclared;
import com.leafactor.cli.rules.Cases.VariableReassigned;
import com.leafactor.cli.rules.Cases.VariableUsed;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.factory.Factory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RecycleRefactoringRule extends AbstractProcessor<CtClass> implements RefactoringRule<CtClass> {
    // List of classes that need to be recycled
    private Map<String, String> opportunities = new LinkedHashMap<>();
    private List<Predicate<CtInvocation>> exceptions = new ArrayList<>();
    private IterationLogger logger;

    public RecycleRefactoringRule(IterationLogger logger) {
        this.logger = logger;
        opportunities.put("TypedArray", "recycle");
        opportunities.put("Bitmap", "recycle");
        opportunities.put("Cursor", "close");
        opportunities.put("VelocityTracker", "recycle");
        opportunities.put("Message", "recycle");
        opportunities.put("MotionEvent", "recycle");
        opportunities.put("Parcel", "recycle");
        opportunities.put("ContentProviderClient", "release");

        // Exception for the MotionEvent.obtain
        exceptions.add(invocation -> {
            CtExpression expression = invocation.getTarget();
            return expression instanceof CtTypeAccess
                    && ((CtTypeAccess) expression).getAccessedType().getSimpleName().equals("MotionEvent")
                    && invocation.getExecutable().getSimpleName().equals("obtain");
        });
    }

    @Override
    public void detectCase(DetectionPhaseContext context) {
        // Detect variables declared
        VariableDeclared variableDeclared = VariableDeclared.detect(context);
        if (variableDeclared != null) {
            String typeName = variableDeclared.variable.getType().getSimpleName();
            if (opportunities.containsKey(typeName)) {
                context.caseOfInterestList.add(variableDeclared);
            }
        }
        // Detect variables reassigned
        VariableReassigned variableReassigned = VariableReassigned.detect(context);
        if (variableReassigned != null) {
            CtExpression lhs = variableReassigned.assignment.getAssigned();
            if (lhs instanceof CtVariableWrite) {
                context.caseOfInterestList.add(variableReassigned);
            }
        }
        // Detect variables usage
        VariableUsed variableUsed = VariableUsed.detect(context);
        if (variableUsed != null) {
            context.caseOfInterestList.add(variableUsed);
        }
    }

    @Override
    public void transformCase(TransformationPhaseContext context) {
        List<VariableDeclared> variables = context.caseOfInterestList.stream()
            .filter(VariableDeclared.class::isInstance)
            .map(VariableDeclared.class::cast).collect(Collectors.toList());
        if(context.caseOfInterest instanceof VariableUsed) {
            // Filtering considering the variables declared
            VariableUsed variableUsed = (VariableUsed) context.caseOfInterest;
            boolean interesting = variables.stream().anyMatch(variableDeclared -> variableUsed.variableAccesses.stream()
                    .anyMatch(ctVariableAccess -> ctVariableAccess.getVariable().getSimpleName()
                            .equals(variableDeclared.variable.getSimpleName())));
            if (interesting) {
                context.accept(context.caseOfInterest);
            }
        } else if(context.caseOfInterest instanceof VariableReassigned) {
            // Filtering considering the variables declared
            VariableReassigned variableReassigned = (VariableReassigned) context.caseOfInterest;
            boolean interesting = variables.stream().anyMatch(variableDeclared -> {
                CtExpression assigned = variableReassigned.assignment.getAssigned();
                return (assigned instanceof CtVariableWrite &&
                        ((CtVariableWrite)assigned).getVariable().getSimpleName()
                                .equals(variableDeclared.variable.getSimpleName()));
            });
            if (interesting) {
                context.accept(context.caseOfInterest);
            }
        } else {
            CaseTransformer.createPassThroughTransformation().transformCase(context);
        }
    }

    private List<CaseOfInterest> getCasesByVariableName(String variableName, List<CaseOfInterest> caseOfInterests) {
        return caseOfInterests.stream().filter(caseOfInterest -> {
            if(caseOfInterest instanceof VariableDeclared) {
                return ((VariableDeclared) caseOfInterest).variable.getSimpleName().equals(variableName);
            } else if(caseOfInterest instanceof VariableReassigned) {
                CtExpression assigned = ((VariableReassigned) caseOfInterest).assignment.getAssigned();
                if(assigned instanceof CtVariableWrite) {
                    return ((CtVariableWrite) assigned).getVariable().getSimpleName().equals(variableName);
                }
            } else if(caseOfInterest instanceof VariableUsed) {
                return ((VariableUsed) caseOfInterest).variableAccesses.stream()
                        .anyMatch(ctVariableAccess -> ctVariableAccess.getVariable().getSimpleName()
                                .equals(variableName));
            }
            return false;
        }).collect(Collectors.toList());
    }

    private String getTypeByVariableName(String variableName, List<CaseOfInterest> caseOfInterests) {
        Optional<VariableDeclared> match = caseOfInterests.stream().filter(VariableDeclared.class::isInstance)
                .map(VariableDeclared.class::cast)
                .filter(variableDeclared -> variableDeclared.variable.getSimpleName().equals(variableName))
                .findFirst();
        if(!match.isPresent()) {
            return null;
        }
        return match.get().variable.getType().getSimpleName();
    }

    private boolean isVariableUnderControl(String variableName, RefactoringPhaseContext context) {
        List<CaseOfInterest> filtered = getCasesByVariableName(variableName, context.caseOfInterests);
        // NOTE: Only check up to this point in the phase
        // TODO - Check if used inside lambda
        // TODO - Check if was returned
        // TODO - Check if was sent as argument
        return false;
    }

    private boolean wasVariableRecycled(String variableName, RefactoringPhaseContext context) {
        List<CaseOfInterest> filtered = getCasesByVariableName(variableName, context.caseOfInterests);
        int index = filtered.indexOf(context.caseOfInterest);
        if(context.caseOfInterest instanceof VariableReassigned) {
            // We do not want to consider this case of interest
            index --;
        }
        filtered = filtered.subList(0, index);
        for(int i = filtered.size() - 1; i >= 0; i --) {
            CaseOfInterest current = filtered.get(i);
            // NOTE: Only check up to this point in the phase and after the last declaration or redeclaration of this variable
            if(current instanceof VariableReassigned || current instanceof VariableDeclared) {
                break;
            } else if(current instanceof VariableUsed) {
                VariableUsed variableUsed = (VariableUsed) current;
                if(variableUsed.getStatement() instanceof CtInvocation) {
                    CtInvocation invocation = ((CtInvocation)current.getStatement());
                    String recyclingMethod = opportunities.get(getTypeByVariableName(variableName, filtered));
                    if(invocation.getExecutable().getSimpleName().equals(recyclingMethod)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void recycleVariableReassigned(RefactoringPhaseContext context) {
        if(!(context.caseOfInterest instanceof VariableReassigned)) {
            return;
        }
        // We consider reassigns because there could be no usage in between Declarations and Reassignments
        CtExpression assigned = ((VariableReassigned) context.caseOfInterest).assignment.getAssigned();
        if(assigned instanceof CtVariableWrite) {
            String variableName = ((CtVariableWrite) assigned).getVariable().getSimpleName();
            String typeName = opportunities.get(getTypeByVariableName(variableName, context.caseOfInterests));
            if(typeName == null) {
                return;
            }
            List<CaseOfInterest> casesOfInterest = getCasesByVariableName(variableName, context.caseOfInterests);
            boolean isLast = casesOfInterest.get(casesOfInterest.size() - 1).equals(context.caseOfInterest);
            if(!isLast) {
                return;
            }
            Factory factory = assigned.getFactory();
            context.caseOfInterest.getStatement().insertAfter(factory
                    .createCodeSnippetStatement(variableName + "." + typeName + "()"));
            boolean wasVariableRecycled = wasVariableRecycled(variableName, context);
            if(wasVariableRecycled) {
                return;
            }
            // Todo - Check if the variable is under control
            boolean isInControl = true;
            if(!isInControl) {
                return;
            }
            context.caseOfInterest.getStatement().insertBefore(factory
                    .createCodeSnippetStatement(variableName + "." + typeName + "()"));
        }
    }

    private void recycleVariableUsed(RefactoringPhaseContext context) {
        if(!(context.caseOfInterest instanceof VariableUsed)) {
            return;
        }
        List<CtVariableAccess> variableAccesses = ((VariableUsed) context.caseOfInterest).variableAccesses;
        variableAccesses.forEach(ctVariableAccess -> {
            String variableName = ctVariableAccess.getVariable().getSimpleName();
            if(!opportunities.containsKey(getTypeByVariableName(variableName, context.caseOfInterests))) {
                return;
            }
            List<CaseOfInterest> casesOfInterest = getCasesByVariableName(variableName, context.caseOfInterests);
            boolean isLast = casesOfInterest.get(casesOfInterest.size() - 1).equals(context.caseOfInterest);
            if(!isLast) {
                return;
            }
            boolean wasVariableRecycled = wasVariableRecycled(variableName, context);
            if(wasVariableRecycled) {
                return;
            }
            // Todo - Check if the variable is under control
            boolean isInControl = true;
            if(!isInControl) {
                return;
            }
            Factory factory = ctVariableAccess.getFactory();
            context.caseOfInterest.getStatement().insertAfter(factory
                    .createCodeSnippetStatement(variableName + "." + opportunities.get(variableName) + "()"));

        });
    }

    @Override
    public void processCase(RefactoringPhaseContext context) {
        recycleVariableReassigned(context);
        recycleVariableUsed(context);
    }

    private void refactor(CtMethod method) {
        Iteration.iterateMethod(this, logger, method,false);
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
    public void onWillTransform(List<CaseOfInterest> caseOfInterests) {

    }

    @Override
    public void onWillTransformCase(TransformationPhaseContext context) {

    }

    @Override
    public void onDidTransformCase(TransformationPhaseContext context) {

    }

    @Override
    public void onWillRefactor(List<CaseOfInterest> caseOfInterests) {

    }

    @Override
    public void onWillRefactorCase(RefactoringPhaseContext context) {

    }

    @Override
    public void onDidRefactorCase(RefactoringPhaseContext context) {

    }
}