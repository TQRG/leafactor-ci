package test.resources.tqrg.leafactor.ci.rules.RecycleRefactoringRule.ShouldRecycleAndLeaveCommentaryUntouched;

public class Input {
    public void wrong1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0); // This commentary describes the statement
        // Another comment
        /*
        And yet another comment
         */
    }
}