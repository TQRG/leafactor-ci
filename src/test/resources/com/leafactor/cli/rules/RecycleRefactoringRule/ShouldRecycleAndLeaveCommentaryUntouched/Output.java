package test.resources.com.leafactor.cli.rules.RecycleRefactoringRule.ShouldRecycleAndLeaveCommentaryUntouched;

public class Recycle {
    public void wrong1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0); // This commentary describes the statement
        if(a != null) {
            a.recycle();
        }
        // Another comment
//        Yet another comment
        /*
        And yet another comment
         */
    }
}