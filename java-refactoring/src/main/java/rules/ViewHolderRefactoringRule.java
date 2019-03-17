package rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import engine.RefactoringRule;

import java.util.*;

/**
 * Refactoring rule that applies the view holder pattern
 */
public class ViewHolderRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {

    private abstract class CaseOfInterest implements Comparable<CaseOfInterest> {
        String caseIdentifier;
        int index; // Order
        boolean iteratingRoot;
        int statementIndex;
        Statement statement;
        NodeWithOptionalBlockStmt container;

        CaseOfInterest(String caseIdentifier, IterationContext context) {
            this.caseIdentifier = caseIdentifier;
            this.index = context.statementIndex;
            this.statementIndex = context.statementIndex;
            this.statement = context.statement;
            this.iteratingRoot = context.iteratingRoot;
            this.container = context.container;
        }

        @Override
        public int compareTo(CaseOfInterest caseOfInterest) {
            if (caseOfInterest == null || !container.equals(caseOfInterest.container)) {
                return 0;
            }
            return caseOfInterest.index - this.index;
        }
    }

    private class ConvertViewReassignInflator extends CaseOfInterest {
        static final String IDENTIFIER = "CONVERT_VIEW_REASSIGNED_INFLATOR";
        // We do not know if the convertView was used up to this point, we might have something like:
                    /*  if(convertView != null) {
                            return convertView;
                        }
                        convertView = layoutInflator.inflate(...);
                    */
        // Or
                    /*
                        if(convertView == null) {
                            convertView = layoutInflator.inflate(...);
                        }
                     */
        // ConvertView was reassigned with an inflated View
        // Consequence: This is considered bad practice, should be refactored to a ternary expression
        ConvertViewReassignInflator(IterationContext context) {
            super(IDENTIFIER, context);
        }
    }

    private class VariableAssignedInflator extends CaseOfInterest {
        static final String IDENTIFIER = "VARIABLE_ASSIGNED_INFLATOR";
        // Other variable was reassigned with an inflated View
        // Consequence: We need to keep track of it
        String variableName;
        VariableAssignedInflator(String variableName, IterationContext context) {
            super(IDENTIFIER, context);
            this.variableName = variableName;
        }
    }

    private class ConvertViewReuseWithTernary extends CaseOfInterest {
        static final String IDENTIFIER = "CONVERT_VIEW_REUSE_WITH_TERNARY";
        String variableName;
        ConvertViewReuseWithTernary(IterationContext context) {
            super(IDENTIFIER, context);
            this.variableName = variableName;
        }
    }

    private class VariableAssignedFindViewById extends CaseOfInterest {
        static final String IDENTIFIER = "VARIABLE_ASSIGNED_FIND_VIEW_BY_ID";
        String variableName;
        VariableAssignedFindViewById(String variableName, IterationContext context) {
            super(IDENTIFIER, context);
            this.variableName = variableName;
        }
    }

    private class IterationContext {
        MethodDeclaration methodDeclaration;
        NodeWithOptionalBlockStmt container;
        boolean iteratingRoot;
        BlockStmt blockStmt;
        int statementIndex;
        Statement statement;
        Set<CaseOfInterest> caseOfInterests = new TreeSet<>();
    }

    private boolean methodSignatureMatches(MethodDeclaration methodDeclaration) {
        // public View getView(final int position, final View convertView, final ViewGroup parent)
        boolean nameMatch = methodDeclaration.getNameAsString().equals("getView");
        Type type = methodDeclaration.getType();
        boolean returnTypeMatch = type.isClassOrInterfaceType() && type.asClassOrInterfaceType().getName()
                .getIdentifier().equals("View");

        boolean isPublic = methodDeclaration.getModifiers().contains(Modifier.publicModifier());
        boolean hasSameNumberOfArguments = methodDeclaration.getParameters().size() == 3;
        System.out.println("Name match: " + nameMatch);
        System.out.println("Is public: " + isPublic);
        System.out.println("Same Number of arguments: " + hasSameNumberOfArguments);
        if (hasSameNumberOfArguments) {
            Type firstArgumentType = methodDeclaration.getParameter(0).getType();
            boolean firstArgumentTypeMatches = firstArgumentType.isPrimitiveType() &&
                    firstArgumentType.asPrimitiveType().getType().asString().equals("int");

            Type secondArgumentType = methodDeclaration.getParameter(1).getType();
            boolean secondArgumentTypeMatches = secondArgumentType.isClassOrInterfaceType() &&
                    secondArgumentType.asClassOrInterfaceType().getName().getIdentifier().equals("View");

            Type thirdArgumentType = methodDeclaration.getParameter(2).getType();
            boolean thirdArgumentTypeMatches = thirdArgumentType.isClassOrInterfaceType() &&
                    thirdArgumentType.asClassOrInterfaceType().getName().getIdentifier().equals("ViewGroup");

            System.out.println("First argument type matches: " + firstArgumentTypeMatches);
            System.out.println("Second argument type matches: " + secondArgumentTypeMatches);
            System.out.println("Third argument type matches: " + thirdArgumentTypeMatches);

            return isPublic &&
                    nameMatch &&
                    returnTypeMatch &&
                    firstArgumentTypeMatches &&
                    secondArgumentTypeMatches &&
                    thirdArgumentTypeMatches;
        }

        return false;
    }

    private boolean checkDeclaredLayoutInflator(IterationContext context, NameExpr nameExpr) {
        // TODO - backtrack to find if the layout inflator was declared
        // Returning true because we might want to implement this last
        return true;
    }

    private void checkDirectInflateAssignment(AssignExpr assignExpr, IterationContext context) {
        // We are assigning to the variable a method call
        MethodCallExpr methodCallExpr = assignExpr.getValue().asMethodCallExpr();
        boolean isInflateCall = methodCallExpr.getName().getIdentifier().equals("inflate");
        boolean takesTwoArguments = methodCallExpr.getArguments().size() == 2;
        boolean validInstance = methodCallExpr.getScope().isPresent() && checkDeclaredLayoutInflator(context, methodCallExpr.getScope().get().asNameExpr());
        if (isInflateCall && takesTwoArguments && validInstance) {
            // Here we know that we are calling method with the same signature
            Expression target = assignExpr.getTarget();
            // TODO - Reconsider other possibilities
            if (!target.isNameExpr()) {
                return;
            }
            String targetName = target.asNameExpr().getName().getIdentifier();
            String argumentName = context.methodDeclaration.getParameter(1).getName().getIdentifier();
            boolean assignedToConvertView = targetName.equals(argumentName);
            if (assignedToConvertView) {
                context.caseOfInterests.add(new ConvertViewReassignInflator(context));
            } else {
                context.caseOfInterests.add(new VariableAssignedInflator(targetName, context));
            }
        }
    }

    private void checkConditionalInflateAssignment(AssignExpr assignExpr, IterationContext context) {

        if (assignExpr.getValue().asConditionalExpr().getCondition().isBinaryExpr()) {
            String argumentName = context.methodDeclaration.getParameter(1).getName().getIdentifier();
            BinaryExpr binaryExpr = assignExpr.getValue().asConditionalExpr().getCondition().asBinaryExpr();


            if (binaryExpr.getOperator() != BinaryExpr.Operator.EQUALS
                    && binaryExpr.getOperator() != BinaryExpr.Operator.NOT_EQUALS) {
                // Not a condition that of interest.
                return;
            }

            // Check both orders if we are check equality with the ConvertView
            if ((!binaryExpr.getLeft().isNameExpr()
                    || !binaryExpr.getRight().isNullLiteralExpr()
                    || !binaryExpr.getLeft().asNameExpr().getNameAsString().equals(argumentName)) &&
                (!binaryExpr.getLeft().isNullLiteralExpr()
                    || !binaryExpr.getRight().isNameExpr()
                    || !binaryExpr.getLeft().asNameExpr().getNameAsString().equals(argumentName))) {
                // Not a condition that of interest.
                return;
            }

            Expression expressionA = assignExpr.getValue().asConditionalExpr().getThenExpr(); // ConvertView variable
            Expression expressionB = assignExpr.getValue().asConditionalExpr().getElseExpr(); // Inflation
            if(binaryExpr.getOperator() == BinaryExpr.Operator.EQUALS) {
                // We have to invert the expressions
                Expression temporary = expressionA;
                expressionA = expressionB;
                expressionB = temporary;
            }

            if(!expressionA.isNameExpr()
                    || !expressionB.isMethodCallExpr()) {
                // Not a condition that of interest.
                return;
            }

            MethodCallExpr methodCallExpr = expressionB.asMethodCallExpr();
            boolean isInflateCall = methodCallExpr.getName().getIdentifier().equals("inflate");
            boolean takesTwoArguments = methodCallExpr.getArguments().size() == 2;
            boolean validInstance = methodCallExpr.getScope().isPresent() && checkDeclaredLayoutInflator(context, methodCallExpr.getScope().get().asNameExpr());
            if(expressionA.asNameExpr().getNameAsString().equals(argumentName) && isInflateCall && takesTwoArguments && validInstance) {
                context.caseOfInterests.add(new ConvertViewReuseWithTernary(context));
            }
        }
    }

    private void checkReusingConvertView(IterationContext context) {
        // We only check if the call to inflate is being done on a variable declared in the current class file,
        // we ignore if the LayoutInflator was created outside of the class because it is impossible for
        // JavaParser to know the scope type of the call to inflate.
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        // TODO - Variable declarations are also a thing we should consider
        boolean isAssignExpression = expression.isAssignExpr();
        if (!isAssignExpression) {
            return;
        }
        // We know up to this point that the current statement is a variable assignment
        AssignExpr assignExpr = expression.asAssignExpr();
        if (assignExpr.getValue().isMethodCallExpr()) {
            checkDirectInflateAssignment(assignExpr, context);
        } else if (assignExpr.getValue().isConditionalExpr()) {
            checkConditionalInflateAssignment(assignExpr, context);
        }
    }

    private void checkDirectFindViewByIdAssignment(AssignExpr assignExpr, IterationContext context) {
        // We are assigning to the variable a method call
        MethodCallExpr methodCallExpr = assignExpr.getValue().asMethodCallExpr();
        boolean isFindViewByIdCall = methodCallExpr.getName().getIdentifier().equals("findViewById");
        boolean takesOneArguments = methodCallExpr.getArguments().size() == 1;
        // TODO - check if we are using a View instance (low priority)
        boolean validInstance = true;
        if (isFindViewByIdCall && takesOneArguments && validInstance) {
            // Here we know that we are calling method with the same signature
            Expression target = assignExpr.getTarget();
            // TODO - Reconsider other possibilities
            if (!target.isNameExpr()) {
                return;
            }
            String targetName = target.asNameExpr().getName().getIdentifier();
            context.caseOfInterests.add(new VariableAssignedFindViewById(targetName, context));
        }
    }

    private void checkFindViewById(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        // TODO - Variable declarations are also a thing we should consider
        boolean isAssignExpression = expression.isAssignExpr();
        if (!isAssignExpression) {
            return;
        }
        // We know up to this point that the current statement is a variable assignment
        AssignExpr assignExpr = expression.asAssignExpr();
        if (assignExpr.getValue().isMethodCallExpr()) {
            checkDirectFindViewByIdAssignment(assignExpr, context);
        }
    }

    // This iteration can occur in inner blocks too
    private void iterate(IterationContext context) {
        if (context.statement instanceof NodeWithOptionalBlockStmt) {
            IterationContext deeperContext = iterateWithNewContext(context.methodDeclaration, false, (NodeWithOptionalBlockStmt) context.statement);
            // TODO - Do something with the deeper context, compile the information that was gathered into the og context.

        }
        checkReusingConvertView(context);
        checkFindViewById(context);
    }

    private IterationContext iterateWithNewContext(MethodDeclaration methodDeclaration, boolean iteratingRoot, NodeWithOptionalBlockStmt currentStatement) {
        Optional optionalBody = currentStatement.getBody();
        if (optionalBody.isPresent() && optionalBody.get() instanceof BlockStmt) {
            BlockStmt blockStmt = (BlockStmt) optionalBody.get();
            IterationContext context = new IterationContext();
            context.methodDeclaration = methodDeclaration;
            context.container = currentStatement;
            context.blockStmt = blockStmt;
            context.iteratingRoot = true;
            for (int i = 0; i < blockStmt.getStatements().size(); i++) {
                context.statement = blockStmt.getStatements().get(i);
                context.statementIndex = i;
                iterate(context);
            }
            return context;
        }
        return null;
    }

    private void refactor(MethodDeclaration methodDeclaration) {
        if (!methodSignatureMatches(methodDeclaration)) {
            System.out.println("Signature does not match");
            return;
        }
        System.out.println("Signature matches");
        IterationContext context = iterateWithNewContext(methodDeclaration, true, methodDeclaration);
        // TODO - Refactoring is done in the end.
        System.out.println("DONE");
    }


    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        refactor(methodDeclaration);
        super.visit(methodDeclaration, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }

}
