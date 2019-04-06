package com.leafactor.cli.engine;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the iteration context of the refactoring phase
 */
public class RefactoringPhaseContext {
    public int offset = 0;
    public CaseOfInterest caseOfInterest;
    public BlockStmt blockStmt;
    public List<CaseOfInterest> caseOfInterests = new ArrayList<>();

    /**
     * The closest BlockStmt by bubbling up
     * @return The closest BlockStmt by bubbling up
     */
    public BlockStmt getClosestBlockStmtParent() {
        return RefactoringRule.getClosestBlockStmtParent(blockStmt);
    }

    /**
     * The closest ClassOrInterfaceDeclaration by bubbling up
     * @return The closest ClassOrInterfaceDeclaration by bubbling up
     */
    public ClassOrInterfaceDeclaration getClosestClassOrInterfaceDeclarationParent() {
        return RefactoringRule.getClosestClassOrInterfaceDeclarationParent(blockStmt);
    }

    /**
     * The closest MethodDeclaration by bubbling up
     * @return The closest MethodDeclaration by bubbling up
     */
    public MethodDeclaration getClosestMethodDeclarationParent() {
        return RefactoringRule.getClosestMethodDeclarationParent(blockStmt);
    }
}
