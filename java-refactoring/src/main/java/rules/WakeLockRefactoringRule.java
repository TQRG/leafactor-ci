package rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;
import engine.RefactoringRule;
import rules.ViewHolderCasesOfInterest.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Refactoring rule that applies the view holder pattern
 */
public class WakeLockRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {

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

    static public boolean checkDeclaredLayoutInflator(IterationContext context, NameExpr nameExpr) {
        // TODO - backtrack to find if the layout inflator was declared
        // Returning true because we might want to implement this last
        return true;
    }


    // This iteration can occur in inner blocks too
    private void iterate(IterationContext context) {
        if (context.statement instanceof NodeWithOptionalBlockStmt) {
            IterationContext deeperContext = iterateWithNewContext(context.methodDeclaration, false, (NodeWithOptionalBlockStmt) context.statement);
            iterate(deeperContext);
            // Todo: do something with the deeperContext
        }
        // TODO - do something
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
        RefactoringIterationContext refactoringIterationContext = new RefactoringIterationContext();
        refactoringIterationContext.context = context;

        List<CaseOfInterest> copy = new ArrayList<>();
        copy.addAll(context.caseOfInterests);
        Iterator<CaseOfInterest> iterator = copy.iterator();
        refactoringIterationContext.iterator = iterator;
        while(iterator.hasNext()) {
            CaseOfInterest caseOfInterest = iterator.next();
            caseOfInterest.refactoringIteration(refactoringIterationContext);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg) {
        Iterator<Node> iterator = classOrInterfaceDeclaration.getChildNodes().iterator();
        while(iterator.hasNext()) {
            Node node = iterator.next();
            if(node instanceof MethodDeclaration) {
                refactor((MethodDeclaration) node);
            }
        }
        super.visit(classOrInterfaceDeclaration, arg);
    }

    @Override
    public void apply(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }

}
