package engine;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;

public class IterationContext {
    public MethodDeclaration methodDeclaration;
    public NodeWithOptionalBlockStmt<BlockStmt> container;
    public boolean iteratingRoot;
    public BlockStmt blockStmt;
    public int statementIndex;
    public Statement statement;
    public List<CaseOfInterest> caseOfInterests = new ArrayList<>();
}