package rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;
import engine.RefactoringRule;
import rules.ViewHolderCasesOfInterest.*;

import java.util.*;

/**
 * Refactoring rule that applies the view holder pattern
 */
public class ViewHolderRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {

    // TODO - Add other cases of interest:
    // TODO -> getTag()
    // TODO -> convertView == null

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
                context.caseOfInterests.add(new ConvertViewReassignInflator(assignExpr, methodCallExpr, context));
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

            Expression target = assignExpr.getTarget();
            // TODO - Reconsider other possibilities
            if (!target.isNameExpr()) {
                return;
            }
            String targetName = target.asNameExpr().getName().getIdentifier();

            MethodCallExpr methodCallExpr = expressionB.asMethodCallExpr();
            boolean isInflateCall = methodCallExpr.getName().getIdentifier().equals("inflate");
            boolean takesTwoArguments = methodCallExpr.getArguments().size() == 2;
            boolean validInstance = methodCallExpr.getScope().isPresent() && checkDeclaredLayoutInflator(context, methodCallExpr.getScope().get().asNameExpr());
            if(expressionA.asNameExpr().getNameAsString().equals(argumentName) && isInflateCall && takesTwoArguments && validInstance) {
                context.caseOfInterests.add(new ConvertViewReuseWithTernary(targetName, context));
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

    private boolean isFindViewByIdCall(MethodCallExpr methodCallExpr) {
        boolean isFindViewByIdCall = methodCallExpr.getName().getIdentifier().equals("findViewById");
        boolean takesOneArguments = methodCallExpr.getArguments().size() == 1;
        boolean validInstance = true;
        return isFindViewByIdCall && takesOneArguments && validInstance;
    }

    private void checkDirectFindViewByIdAssignment(AssignExpr assignExpr, IterationContext context) {
        Optional<Type> castType = Optional.empty();
        Expression value = assignExpr.getValue();

        if(value.isCastExpr()) {
            CastExpr castExpr = value.asCastExpr();
            castType = Optional.of(castExpr.getType());
            value = castExpr.getExpression();
        }

        if (!value.isMethodCallExpr()) {
            return;
        }
        // We are assigning to the variable a method call
        MethodCallExpr methodCallExpr = value.asMethodCallExpr();
        if(isFindViewByIdCall(methodCallExpr)) {
            Expression target = assignExpr.getTarget();
            // TODO - Reconsider other possibilities
            if (!target.isNameExpr()) {
                return;
            }
            String targetName = target.asNameExpr().getName().getIdentifier();
            context.caseOfInterests.add(new VariableAssignedFindViewById(assignExpr, castType.orElse(null), targetName, context));
        }
    }

    private void checkDirectFindViewByIdDeclaration(VariableDeclarationExpr declarationExpr, IterationContext context) {
        // We are assigning to the variable a method call
        for(VariableDeclarator variableDeclarator : declarationExpr.getVariables()) {
            String variableName = variableDeclarator.getNameAsString();
            Type type = variableDeclarator.getType();
            context.caseOfInterests.add(new VariableDeclared(type, variableName, context));
            Optional<Expression> optionalInitializer = variableDeclarator.getInitializer();
            if(!optionalInitializer.isPresent()) {
                continue;
            }

            // Ignoring casts for now
            Optional<Type> castType = Optional.empty();
            Expression value = optionalInitializer.get();

            if(value.isCastExpr()) {
                CastExpr castExpr = value.asCastExpr();
                castType = Optional.of(castExpr.getType());
                value = castExpr.getExpression();
            }

            if (!value.isMethodCallExpr()) {
                continue;
            }

            MethodCallExpr initializer = value.asMethodCallExpr();
            if(isFindViewByIdCall(initializer)) {
                context.caseOfInterests.add(new VariableAssignedFindViewById(variableDeclarator, castType.orElse(null), variableName, context));
            }
        }
    }

    private void checkFindViewById(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if (expression.isAssignExpr()) {
            checkDirectFindViewByIdAssignment(expression.asAssignExpr(), context);
        } else if(expression.isVariableDeclarationExpr()) {
            checkDirectFindViewByIdDeclaration(expression.asVariableDeclarationExpr(), context);
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
            context.iteratingRoot = iteratingRoot;
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
        if(context == null) {
            return;
        }
        // TODO - Refactoring is done in the end.
        System.out.println("DONE");

        RefactoringIterationContext refactoringIterationContext = new RefactoringIterationContext();
        refactoringIterationContext.context = context;
        for(CaseOfInterest caseOfInterest : context.caseOfInterests) {
            caseOfInterest.refactoringIteration(refactoringIterationContext);
        }
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
