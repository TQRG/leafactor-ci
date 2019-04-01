package com.leafactor.cli.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.leafactor.cli.engine.CaseOfInterest;
import com.leafactor.cli.engine.IterationContext;
import com.leafactor.cli.engine.RefactoringIterationContext;
import com.leafactor.cli.engine.RefactoringRule;
import com.leafactor.cli.rules.DrawAllocationCasesOfInterest.ObjectAllocation;
import com.leafactor.cli.rules.ViewHolderCasesOfInterest.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Refactoring rule that applies the view holder pattern
 */
public class DrawAllocationRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule {

    // TODO - Add other cases of interest:
    // TODO -> getTag()
    // TODO -> convertView == null

    private boolean methodSignatureMatches(MethodDeclaration methodDeclaration) {
        // public View getView(final int position, final View convertView, final ViewGroup parent)
        boolean nameMatch = methodDeclaration.getNameAsString().equals("onDraw");
        Type type = methodDeclaration.getType();
        boolean returnTypeMatch = type.isVoidType();
        boolean isProtected = methodDeclaration.getModifiers().contains(Modifier.PROTECTED);
        boolean hasSameNumberOfArguments = methodDeclaration.getParameters().size() == 1;
//        System.out.println("Name match: " + nameMatch);
//        System.out.println("Is public: " + isProtected);
//        System.out.println("Same Number of arguments: " + hasSameNumberOfArguments);
        if (hasSameNumberOfArguments) {
            Type firstArgumentType = methodDeclaration.getParameter(0).getType();
            boolean firstArgumentTypeMatches = firstArgumentType.isClassOrInterfaceType() &&
                    firstArgumentType.asClassOrInterfaceType().getNameAsString().endsWith("Canvas");

//            System.out.println("First argument type matches: " + firstArgumentTypeMatches);

            return isProtected &&
                    nameMatch &&
                    returnTypeMatch &&
                    firstArgumentTypeMatches;
        }

        return false;
    }

    // This iteration can occur in inner blocks too
    private void iterate(IterationContext context) {
        if (context.statement instanceof NodeWithOptionalBlockStmt) {
            IterationContext deeperContext = iterateWithNewContext(context.methodDeclaration, false, (NodeWithOptionalBlockStmt) context.statement);
            iterate(deeperContext);
            // Todo: do something with the deeperContext
        }
        VariableDeclared.checkStatement(context);
        ObjectAllocation.checkStatement(context);
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
//            System.out.println("Signature does not match");
            return;
        }
        System.out.println("Signature matches");
        IterationContext context = iterateWithNewContext(methodDeclaration, true, methodDeclaration);
        if(context == null) {
            return;
        }
        RefactoringIterationContext refactoringIterationContext = new RefactoringIterationContext();
        refactoringIterationContext.context = context;

        List<CaseOfInterest> copy = new ArrayList<>(context.caseOfInterests);
        Iterator<CaseOfInterest> iterator = copy.iterator();
        refactoringIterationContext.iterator = iterator;
        while(iterator.hasNext()) {
            CaseOfInterest caseOfInterest = iterator.next();
            System.out.println("HERE!" + caseOfInterest);
            caseOfInterest.refactoringIteration(refactoringIterationContext);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg) {
        // Todo - check class extension

        for (Node node : classOrInterfaceDeclaration.getChildNodes()) {
            if (node instanceof MethodDeclaration) {
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
