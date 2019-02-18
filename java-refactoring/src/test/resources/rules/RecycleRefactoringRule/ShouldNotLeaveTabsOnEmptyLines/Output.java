package rules.RecycleRefactoringRule.VariableAlreadyRecycledShouldNotRecycle;

public class Teste {
    public void testMultipleAssignment(Uri uri, ContentProvider provider){
        Cursor query = provider.query(uri, null, null, null, null);
        query.getLong(0);
        if(query != null) {
            query.close();
        }
        query = provider.query(uri, null, null, null, null);
        if(query != null) {
            query.close();
        }

    }
}
