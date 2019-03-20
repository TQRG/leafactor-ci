package rules.ViewHolderCasesOfInterest;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;
import engine.RefactoringRule;

import java.util.Optional;

public class VariableAssignedFindViewById extends CaseOfInterest {
    String variableName;
    // assignExpr and variableDeclarator are mutual exclusive
    AssignExpr assignExpr;
    VariableDeclarator variableDeclarator;
    Type castType;
    public VariableAssignedFindViewById(AssignExpr assignExpr, Type castType, String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
        this.assignExpr = assignExpr;
        this.castType = castType;
    }

    public VariableAssignedFindViewById(VariableDeclarator variableDeclarator, Type castType, String variableName, IterationContext context) {
        super(context);
        this.variableName = variableName;
        this.variableDeclarator = variableDeclarator;
        this.castType = castType;
    }

    @Override
    public void refactoringIteration(RefactoringIterationContext refactoringIterationContext) {
        Optional<BlockStmt> optionalBlockStmt = this.container.getBody();
        if (optionalBlockStmt.isPresent()) {
            BlockStmt blockStmt = optionalBlockStmt.get();
            String argumentName = refactoringIterationContext.context.methodDeclaration.getParameter(1).getName().getIdentifier();
            Statement currentStatement = refactoringIterationContext.context.statement;
            // Regress to the root of the java document
            Node root = currentStatement.getParentNode().orElse(currentStatement);
            while(root.getParentNode().isPresent()) {
                root = root.getParentNode().get();
            }

            // Finding the viewHolder class implementation (we ignore view holders declared in other class files)
            boolean wasViewHolderClassDeclared = RefactoringRule.hasNodeOfInterest(root, node -> {
                // TODO - Check if the viewHolder class was declared
                // Todo - Check if the class has the variable of interest declared inside and if the findViewById cast matches, if not, add the variable to the class.
                return false;
            });

            if(!wasViewHolderClassDeclared) {
                // TODO - We need to create the class (note: we do not know every variable that was declared yet, should we search future usages right now or add them incrementally?).
            }

            // Todo - Check if viewHolderItem variable was declared in this scope(note that it could be declared later on, in such cases we simply ignore this case)
            // Todo - Check if convertView could null up to this point, if it could, it is necessary to make an additional condition refactoring
            // Todo - Check if ViewHolderItem class was created, if there isn't we should add the class or add the variable


            // We have a lot of assumptions from this point forward:
            //  -> convertView is not null
            //  -> ViewHolderItem class is created
            //  -> ViewHolderItem class has the variable we want to use
            //  -> viewHolderItem variable was not declared yet

            // GENERATES: ViewHolderItem viewHolderItem = (ViewHolderItem) convertView.getTag();
            String EOL = System.getProperty("line.separator");
            Statement viewHolderItemDeclaration = LexicalPreservingPrinter.setup(
                    StaticJavaParser.parseStatement(
                            String.format(
                                    "ViewHolderItem viewHolderItem = (ViewHolderItem) %s.getTag();" + EOL,
                                    argumentName)));
            blockStmt.addStatement(this.statementIndex + refactoringIterationContext.offset, viewHolderItemDeclaration);
            refactoringIterationContext.offset += 1;
            // GENERATES:
            // if(viewHolderItem == null) {
            //    viewHolderItem = new ViewHolderItem();
            //    convertView.setTag(new ViewHolderItem());
            // }
            int tabs = (blockStmt.getEnd().get().column -1) / 4 + 1;
            String tab = "    ";
            StringBuilder baseTabPadding = new StringBuilder(tabs > 0 ? tab : "");
            for (int i = 1; i < tabs; i++) {
                baseTabPadding.append(tab);
            }
            Statement ifStmt = LexicalPreservingPrinter.setup(
                    StaticJavaParser.parseStatement(
                            String.format("if(viewHolderItem == null) {" + EOL +
                                            baseTabPadding + tab + "viewHolderItem = new ViewHolderItem();" + EOL +
                                            baseTabPadding + tab + "%s.setTag(viewHolderItem);" + EOL +
                                            baseTabPadding + "}" + EOL,
                                    argumentName)));
            blockStmt.addStatement(this.statementIndex + refactoringIterationContext.offset, ifStmt);
            refactoringIterationContext.offset += 1;

            BinaryExpr binaryExpr = new BinaryExpr();
            binaryExpr.setOperator(BinaryExpr.Operator.NOT_EQUALS);
            binaryExpr.setLeft(new NameExpr("viewHolderItem"));
            binaryExpr.setRight(new NullLiteralExpr());
            ConditionalExpr conditionalExpr = new ConditionalExpr();
            conditionalExpr.setCondition(binaryExpr);
            conditionalExpr.setThenExpr(new NameExpr("viewHolderItem"));
            if(assignExpr != null) {
                conditionalExpr.setElseExpr(assignExpr.getValue());
                assignExpr.setValue(conditionalExpr);
            } else {
                conditionalExpr.setElseExpr(variableDeclarator.getInitializer().get());
                variableDeclarator.setInitializer(conditionalExpr);
            }
        }
    }
}