package rules.RecycleRefactoringRule.VariableAlreadyRecycledShouldNotRecycle;

public class Teste {
    public void testMultipleAssignment(Uri uri, ContentProvider provider){
        Cursor query = provider.query(uri, null, null, null, null);
        query.getLong(0);
        query = provider.query(uri, null, null, null, null);

    }
}
