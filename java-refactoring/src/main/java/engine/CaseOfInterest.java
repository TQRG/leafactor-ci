package engine;

import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

public abstract class CaseOfInterest implements Comparable<CaseOfInterest> {
    protected int index; // Order
    protected boolean iteratingRoot;
    protected int statementIndex;
    protected Statement statement;
    protected NodeWithOptionalBlockStmt<BlockStmt> container;

    public CaseOfInterest(IterationContext context) {
        this.index = context.statementIndex;
        this.statementIndex = context.statementIndex;
        this.statement = context.statement;
        this.iteratingRoot = context.iteratingRoot;
        this.container = context.container;
    }

    public abstract void refactoringIteration(RefactoringIterationContext refactoringIterationContext);

    @Override
    public int compareTo(CaseOfInterest caseOfInterest) {
        if (caseOfInterest == null || !container.equals(caseOfInterest.container)) {
            return 0;
        }
        return this.index - caseOfInterest.index;
    }
}