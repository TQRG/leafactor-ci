package com.leafactor.cli.engine;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;

public class IterationContext {
    public BlockStmt blockStmt;
    public int statementIndex;
    public Statement statement;
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