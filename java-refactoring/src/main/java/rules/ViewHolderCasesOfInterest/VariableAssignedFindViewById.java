package rules.ViewHolderCasesOfInterest;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import engine.CaseOfInterest;
import engine.IterationContext;
import engine.RefactoringIterationContext;
import engine.RefactoringRule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    static private boolean isFindViewByIdCall(MethodCallExpr methodCallExpr) {
        boolean isFindViewByIdCall = methodCallExpr.getName().getIdentifier().equals("findViewById");
        boolean takesOneArguments = methodCallExpr.getArguments().size() == 1;
        boolean validInstance = true;
        return isFindViewByIdCall && takesOneArguments && validInstance;
    }

    public static void checkStatement(IterationContext context) {
        boolean isExpressionStmt = context.statement.isExpressionStmt();
        if (!isExpressionStmt) {
            return;
        }
        Expression expression = context.statement.asExpressionStmt().getExpression();
        if (expression.isAssignExpr()) {
            AssignExpr assignExpr = expression.asAssignExpr();
            Optional<Type> castType = Optional.empty();
            Expression value = assignExpr.getValue();

            if(value.isCastExpr()) {
                CastExpr castExpr = value.asCastExpr();
                castType = Optional.of(castExpr.getType());
                value = castExpr.getExpression();
            }

            if (!value.isMethodCallExpr()) {
                return;
            }
            // We are assigning to the variable a method call
            MethodCallExpr methodCallExpr = value.asMethodCallExpr();
            if(isFindViewByIdCall(methodCallExpr)) {
                Expression target = assignExpr.getTarget();
                if (!(target instanceof NodeWithSimpleName)) {
                    return;
                }
                String targetName = ((NodeWithSimpleName) target).getNameAsString();
                context.caseOfInterests.add(new VariableAssignedFindViewById(assignExpr, castType.orElse(null), targetName, context));
            }
        } else if(expression.isVariableDeclarationExpr()) {
            for(VariableDeclarator variableDeclarator : expression.asVariableDeclarationExpr().getVariables()) {
                String variableName = variableDeclarator.getNameAsString();
                Type type = variableDeclarator.getType();
                Optional<Expression> optionalInitializer = variableDeclarator.getInitializer();
                if(!optionalInitializer.isPresent()) {
                    continue;
                }

                // Ignoring casts for now
                Optional<Type> castType = Optional.empty();
                Expression value = optionalInitializer.get();

                if(value.isCastExpr()) {
                    CastExpr castExpr = value.asCastExpr();
                    castType = Optional.of(castExpr.getType());
                    value = castExpr.getExpression();
                }

                if (!value.isMethodCallExpr()) {
                    continue;
                }

                MethodCallExpr initializer = value.asMethodCallExpr();
                if(isFindViewByIdCall(initializer)) {
                    context.caseOfInterests.add(new VariableAssignedFindViewById(variableDeclarator, castType.orElse(null), variableName, context));
                }
            }
        }
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
            String EOL = System.getProperty("line.separator");
            int tabs = (blockStmt.getEnd().get().column -1) / 4 + 1;
            String tab = "    ";
            StringBuilder baseTabPadding = new StringBuilder(tabs > 0 ? tab : "");
            for (int i = 1; i < tabs; i++) {
                baseTabPadding.append(tab);
            }

            // Finding the viewHolder class implementation (we ignore view holders declared in other class files)
            List<ClassOrInterfaceDeclaration> viewHolderItemClasses = RefactoringRule.getNodesOfInterest(root, node -> {
                // TODO - We have a situation where class could be declared inside another inner class, we should search only narrowly
                if(node instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) node;
//                    boolean isStatic = classOrInterfaceDeclaration.isStatic();
                    return classOrInterfaceDeclaration.getNameAsString().equals("ViewHolderItem");
                }
                return false;
            }, ClassOrInterfaceDeclaration.class);

            if(viewHolderItemClasses.size() == 0) {
                System.out.println("View Holder Class not declared");
                Optional<Node> optionalNode = refactoringIterationContext.context.methodDeclaration.getParentNode();
                if(optionalNode.isPresent() && optionalNode.get() instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration)optionalNode.get();
                    String baseTabPaddingClass = baseTabPadding.substring(4);
                    TypeDeclaration viewHolderItemClass = LexicalPreservingPrinter.setup(
                            StaticJavaParser.parseTypeDeclaration(
                                    String.format(baseTabPaddingClass + "static class ViewHolderItem {" + EOL +
                                                    baseTabPaddingClass + tab + "%s %s;" + EOL +
                                                    baseTabPaddingClass + "}" + EOL,
                                            castType.asString(), variableName)));


                    // TODO - Should add the class to the top of the method, yet JavaParse has another bug with indentation
                    // Uncomment when JavaParser bug is fixed
                    // int i = classOrInterfaceDeclaration.getMembers().indexOf(refactoringIterationContext.context.methodDeclaration);
                    // classOrInterfaceDeclaration.getMembers().add(i, viewHolderItemClass);
                    classOrInterfaceDeclaration.addMember(viewHolderItemClass);
                }
                // TODO - We need to create the class (note: we do not know every variable that was declared yet, should we search future usages right now or add them incrementally?).
            } else {
                // Check if the class has the field, if not add the field.
                List<FieldDeclaration> fieldDeclarationList = viewHolderItemClasses.get(0).getFields();
                boolean hasField = fieldDeclarationList.stream().map(FieldDeclaration::getVariables)
                        .flatMap(Collection::stream).anyMatch(variableDeclarator1 -> variableDeclarator.getNameAsString().equals(variableName));
                if(!hasField) {
                    viewHolderItemClasses.get(0).addField(castType, variableName);
                }
                System.out.println("View Holder class declared");
            }


            // Todo - Check if convertView could be null up to this point, if it could, it is necessary to make an additional condition refactoring

            // We have a lot of assumptions from this point forward:
            //  -> convertView is not null
            //  -> ViewHolderItem class is created
            //  -> ViewHolderItem class has the variable we want to use


            List<VariableAssignedGetTag> variableAssignedGetTagList = refactoringIterationContext.context.caseOfInterests.stream()
                    .filter((caseOfInterest) -> caseOfInterest instanceof VariableAssignedGetTag)
                    .map(VariableAssignedGetTag.class::cast).collect(Collectors.toList());

            // Todo - We need to be careful because the VariableAssignedGetTag may be after the current case of interest

            String viewHolderVariableName = "viewHolderItem";
            // Reusing variables
            if(variableAssignedGetTagList.size() > 0) {
                // We want to use the last declared variable that retrieved the value through getTag
                VariableAssignedGetTag variableAssignedGetTag = variableAssignedGetTagList.get(variableAssignedGetTagList.size() - 1);
                viewHolderVariableName = variableAssignedGetTag.variableName;
            } else {
                // GENERATES: ViewHolderItem viewHolderItem = (ViewHolderItem) convertView.getTag();
                // TODO check if the viewHolderItem was created somewhere, what do we do if it was created after?
                Statement viewHolderItemDeclaration = LexicalPreservingPrinter.setup(
                        StaticJavaParser.parseStatement(
                                String.format(
                                        baseTabPadding + "ViewHolderItem viewHolderItem = (ViewHolderItem) %s.getTag();",
                                        argumentName)));
                blockStmt.addStatement(this.statementIndex + refactoringIterationContext.offset, viewHolderItemDeclaration);
                refactoringIterationContext.offset += 1;
                // TODO - Here we may want to create variableAssignedGetTagList
            }

            // GENERATES:
            // if(viewHolderItem == null) {
            //    viewHolderItem = new ViewHolderItem();
            //    convertView.setTag(new ViewHolderItem());
            // }

            // TODO - Here we may want to create a Case of interest
            String initializer;
            if(assignExpr != null) {
                initializer = assignExpr.getValue().toString();
            } else {
                initializer = variableDeclarator.getInitializer().get().toString();
            }
            List<VariableCheckNull> variableCheckNullList = refactoringIterationContext.context.caseOfInterests.stream()
                    .filter((caseOfInterest) -> caseOfInterest instanceof VariableCheckNull)
                    .map(VariableCheckNull.class::cast).collect(Collectors.toList());
            final String viewHolderVariableNameFinal = viewHolderVariableName;
            Optional<VariableCheckNull> optionalVariableCheckNull = variableCheckNullList.stream()
                    .filter((caseOfInterest) -> caseOfInterest.variableName.equals(viewHolderVariableNameFinal)).findFirst();
            // TODO check if the optionalVariableCheckNull was after the current statement, it should not be
            if(optionalVariableCheckNull.isPresent()) {
                VariableCheckNull variableCheckNull = optionalVariableCheckNull.get();
                IfStmt ifStmt = variableCheckNull.getStatement().asIfStmt();
                if(ifStmt.getThenStmt().isBlockStmt()) {
                    BlockStmt blockStmt1 = ifStmt.getThenStmt().asBlockStmt();
                     // TODO - We should add the reference to the viewHolder field, and check if it is there already
                    IterationContext context = new IterationContext();
                    context.methodDeclaration = refactoringIterationContext.context.methodDeclaration;
                    context.container = null;
                    context.blockStmt = blockStmt1;
                    context.iteratingRoot = iteratingRoot;
                    for (int i = 0; i < blockStmt1.getStatements().size(); i++) {
                        context.statement = blockStmt1.getStatements().get(i);
                        context.statementIndex = i;
                        VariableAssignedFindViewById.checkStatement(context);
                    }
                    if(!context.caseOfInterests.stream().anyMatch(caseOfInterest ->
                                    ((VariableAssignedFindViewById)caseOfInterest).variableName.equals(variableName))) {
                        Statement viewHolderItemDeclaration = LexicalPreservingPrinter.setup(
                                StaticJavaParser.parseStatement(
                                        String.format("%s.%s = %s;" + EOL,
                                                viewHolderVariableName,
                                                variableName,
                                                initializer)));
                        blockStmt1.addStatement(viewHolderItemDeclaration);
                    }
                    System.out.println("We should add the reference to the viewHolder field" + context.caseOfInterests);
                }
            } else {
                Statement ifStmt = LexicalPreservingPrinter.setup(
                        StaticJavaParser.parseStatement(
                                String.format(baseTabPadding +"if(%s == null) {" + EOL +
                                                baseTabPadding + tab + "%s = new ViewHolderItem();" + EOL +
                                                baseTabPadding + tab + "%s.setTag(%s);" + EOL +
                                                baseTabPadding + tab + "%s.%s = %s;" + EOL +
                                                baseTabPadding + "}" + EOL,
                                        viewHolderVariableName,
                                        viewHolderVariableName,
                                        argumentName,
                                        viewHolderVariableName,
                                        viewHolderVariableName,
                                        variableName,
                                        initializer)));
                blockStmt.addStatement(this.statementIndex + refactoringIterationContext.offset, ifStmt);
                refactoringIterationContext.offset += 1;
            }
            // Here we substitute the call to findViewById to the viewHolder object field
            FieldAccessExpr fieldAccessExpr = new FieldAccessExpr();
            fieldAccessExpr.setName(variableName);
            fieldAccessExpr.setScope(new NameExpr(viewHolderVariableName));
            if(assignExpr != null) {
                assignExpr.setValue(fieldAccessExpr);
            } else {
                variableDeclarator.setInitializer(fieldAccessExpr);
            }
        }
    }
}