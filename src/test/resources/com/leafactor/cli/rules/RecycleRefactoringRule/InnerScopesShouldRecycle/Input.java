package test.resources.com.leafactor.cli.rules.RecycleRefactoringRule.InnerScopesShouldRecycle;

public class Recycle {
    public void method1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0);
        if(foo()) {
            a = getNewTypedArray();
        }
        int c = 5;
        int d = 6;
    }
}