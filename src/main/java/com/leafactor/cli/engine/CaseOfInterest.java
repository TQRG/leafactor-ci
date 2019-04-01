package com.leafactor.cli.engine;

import com.github.javaparser.ast.stmt.Statement;

public abstract class CaseOfInterest implements Comparable<CaseOfInterest> {
    protected int index; // Order
    protected int statementIndex;
    protected Statement statement;

    public CaseOfInterest(IterationContext context) {
        this.index = context.statementIndex;
        this.statementIndex = context.statementIndex;
        this.statement = context.statement;
    }

    public abstract void refactorIteration(RefactoringIterationContext refactoringIterationContext);

    @Override
    public int compareTo(CaseOfInterest caseOfInterest) {
        if (caseOfInterest == null) {
            return 0;
        }
        return this.index - caseOfInterest.index;
    }

    public int getIndex() {
        return index;
    }

    public int getStatementIndex() {
        return statementIndex;
    }

    public Statement getStatement() {
        return statement;
    }

}