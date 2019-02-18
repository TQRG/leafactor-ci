package rules.RecycleRefactoringRule.VariableAlreadyRecycledShouldNotRecycle;

/**
 * Inner scopes with redeclarations should not be recycled, as the outer scope might have reasigned the instance
 * to another another variable
 */
public class Recycle {
    public void method1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0);
        if(foo()) {
            if(a != null) {
                a.recycle();
            }
            a = getNewTypedArray();
        }
        if(a != null) {
            a.recycle();
        }
        int c = 5;
        int d = 6;
    }
}