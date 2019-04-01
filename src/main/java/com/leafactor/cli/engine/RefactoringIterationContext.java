package com.leafactor.cli.engine;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.ArrayList;
import java.util.List;

public class RefactoringIterationContext {
    public int offset = 0;
    public CaseOfInterest caseOfInterest;
    public BlockStmt blockStmt;
    public List<CaseOfInterest> caseOfInterests = new ArrayList<>();

    public BlockStmt getClosestBlockStmtParent() {
        return RefactoringRule.getClosestBlockStmtParent(blockStmt);
    }

    public ClassOrInterfaceDeclaration getClosestClassOrInterfaceDeclarationParent() {
        return RefactoringRule.getClosestClassOrInterfaceDeclarationParent(blockStmt);
    }

    public MethodDeclaration getClosestMethodDeclarationParent() {
        return RefactoringRule.getClosestMethodDeclarationParent(blockStmt);
    }
}
