package com.leafactor.cli.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.leafactor.cli.engine.*;
import com.leafactor.cli.rules.GenericCasesOfInterest.VariableDeclared;
import com.leafactor.cli.rules.ViewHolderCasesOfInterest.*;

import java.util.*;

/**
 * Refactoring rule that applies the view holder pattern
 */
public class ViewHolderRefactoringRule extends VoidVisitorAdapter<Void> implements RefactoringRule, Iteration {

    // TODO - Add other cases of interest:
    // TODO -> getTag()
    // TODO -> convertView == null
    private IterationLogger logger;

    public ViewHolderRefactoringRule(IterationLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onSetup(IterationContext context) {}

    @Override
    public void onWillIterate(IterationContext context) {}

    @Override
    public void onDidIterate(IterationContext context) {}

    @Override
    public void onWillRefactor(List<CaseOfInterest> caseOfInterests) {}

    @Override
    public void onWillRefactorCase(RefactoringIterationContext context) {}

    @Override
    public void onDidRefactorCase(RefactoringIterationContext context) {}


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

    private void refactor(MethodDeclaration methodDeclaration) {
        if (!methodSignatureMatches(methodDeclaration)) {
            System.out.println("Signature does not match");
            return;
        }
        System.out.println("Signature matches");
        CaseDetector caseDetector = CaseDetector.CompileCaseDetector(
                ConvertViewReassignInflator::detect,
                ConvertViewReuseWithTernary::detect,
                VariableAssignedFindViewById::detect,
                VariableAssignedInflator::detect,
                VariableDeclared::detect,
                VariableCheckNull::detect,
                VariableAssignedGetTag::detect
        );
        Iteration.iterateMethod(this, logger,methodDeclaration, this, caseDetector, false);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void arg) {
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
