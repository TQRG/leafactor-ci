package com.leafactor.cli.engine;

import com.github.javaparser.ast.stmt.Statement;

/**
 * Represents a case of interest found in a given statement
 */
public abstract class CaseOfInterest implements Comparable<CaseOfInterest> {
    protected int index; // Order
    protected int statementIndex;
    protected Statement statement;

    /**
     * Constructor
     * @param context The iteration context
     */
    public CaseOfInterest(DetectionPhaseContext context) {
        this.index = context.statementIndex;
        this.statementIndex = context.statementIndex;
        this.statement = context.statement;
    }

    /**
     * Applies a refactoring using the refactoring iteration context
     * @param refactoringPhaseContext The refactoring iteration context
     */
    public abstract void refactorIteration(RefactoringPhaseContext refactoringPhaseContext);

    /**
     * Compares the instance with another case of interest based on their indexes
     * @param caseOfInterest The case of interest
     * @return Comparison value representing the order of the instances
     */
    @Override
    public int compareTo(CaseOfInterest caseOfInterest) {
        if (caseOfInterest == null) {
            return 0;
        }
        return this.index - caseOfInterest.index;
    }

    /**
     * Getter for the index
     * @return The index that represents order
     */
    public int getIndex() {
        return index;
    }

    /**
     * Getter for the statement index
     * @return The index of the statement for this case of interest
     */
    public int getStatementIndex() {
        return statementIndex;
    }

    /**
     * Getter for the statement
     * @return The statement where this case of interest was discovered
     */
    public Statement getStatement() {
        return statement;
    }

}