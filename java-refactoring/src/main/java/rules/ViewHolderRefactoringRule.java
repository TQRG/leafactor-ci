package rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
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

    private class IterationContext {
        NodeWithOptionalBlockStmt container;
        boolean iteratingRoot;
        BlockStmt blockStmt;
        int statementIndex;
        Statement statement;

        boolean reusedConvertView = false;
        boolean refactoredConvertView = false;
        boolean detectedInflation = false;

        // This is a map of variable-statement that reference the convertView received by arg
        Map<String, Statement> convertViewReferences = new LinkedHashMap<>();
        // This is a list of variable-statement that reference an inflated view
        Map<String, Statement> inflationViewReferences = new LinkedHashMap<>();
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

    private boolean checkInflation(IterationContext context) {
        // We only check if the call to inflate is being done on a variable declared in the current class file,
        // we ignore if the LayoutInflator was created outside of the class because it is impossible for
        // JavaParser to know the scope type of the call to inflate.



        // TODO - THIS CHECK IS SHALLOW - Only true if it is an expression
        // TODO - check if an inflation method is being called
        return false;
    }

    private boolean checkReusingConvertView(IterationContext context) {
        // TODO - THIS CHECK IS SHALLOW
        // TODO - Check if the Convert View is being reutilized in this statement.
        // Check if there is a reassign of a View variable with the convertView or the convertView itself
        // TODO - If true add the variable to variableReferences if necessary.
        return false;
    }

    // This iteration can occur in inner blocks too
    private void iterate(IterationContext context) {
        if (context.statement instanceof NodeWithOptionalBlockStmt) {
            IterationContext deeperContext = iterateWithNewContext(false, (NodeWithOptionalBlockStmt) context.statement);
            // TODO - Do something with the deeper context, compile the information that was gathered into the og context.
        }
        boolean hasInflation = checkInflation(context);
        boolean reusingConvertView = checkReusingConvertView(context);
    }

    private IterationContext iterateWithNewContext(boolean iteratingRoot, NodeWithOptionalBlockStmt currentStatement) {
        Optional optionalBody = currentStatement.getBody();
        if (optionalBody.isPresent() && optionalBody.get() instanceof BlockStmt) {
            BlockStmt blockStmt = (BlockStmt) optionalBody.get();
            IterationContext context = new IterationContext();
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
        IterationContext context = iterateWithNewContext(true, methodDeclaration);
        // TODO - Refactoring is done in the end.
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
