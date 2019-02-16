package rules.RecycleRefactoringRule.VariableAlreadyRecycledShouldNotRecycle;

public class Recycle {
    public TypedArray method1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0);
        int c = 5;
        int d = 6;
        return a;
    }
}