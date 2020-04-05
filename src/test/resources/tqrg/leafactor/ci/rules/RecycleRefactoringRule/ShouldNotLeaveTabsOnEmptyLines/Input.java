package test.resources.tqrg.leafactor.ci.rules.RecycleRefactoringRule.ShouldNotLeaveTabsOnEmptyLines;

public class Input {
    public void testMultipleAssignment(Uri uri, ContentProvider provider){
        Cursor query = provider.query(uri, null, null, null, null);
        query.getLong(0);
        query = provider.query(uri, null, null, null, null);

    }
}
