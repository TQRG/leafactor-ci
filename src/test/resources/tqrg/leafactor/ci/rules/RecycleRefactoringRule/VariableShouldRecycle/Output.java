package test.resources.tqrg.leafactor.ci.rules.RecycleRefactoringRule.VariableShouldRecycle;

public class Input {
    public void wrong1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0);
        if (a != null) {
            a.recycle();
        }
    }
}