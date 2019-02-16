package rules.RecycleRefactoringRule.VariableAlreadyRecycledShouldNotRecycle;

public class Recycle {
    public void wrong1(AttributeSet attrs , int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0);
        int c = 5;
        int d = 6;
        a = getNewTypedArray();
        example = a.getString(0);
        a.recycle();
    }
}