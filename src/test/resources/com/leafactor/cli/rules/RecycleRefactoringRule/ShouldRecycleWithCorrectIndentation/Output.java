package test.resources.com.leafactor.cli.rules.RecycleRefactoringRule.ShouldRecycleWithCorrectIndentation;

public class Input {
    public void wrong1(AttributeSet attrs , int defStyle) {
        get((test) -> {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
            String example = a.getString(0);
            if(a != null) {
                a.recycle();
            }
        });
    }
}