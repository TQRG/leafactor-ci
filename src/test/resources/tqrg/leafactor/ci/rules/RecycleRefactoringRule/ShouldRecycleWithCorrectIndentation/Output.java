package test.resources.tqrg.leafactor.ci.rules.RecycleRefactoringRule.ShouldRecycleWithCorrectIndentation;

public class Input {
    public static interface DoSomething<F> {
        void convert(F from);
    }

    public void wrong1(AttributeSet attrs , int defStyle) {
        get(((DoSomething) (( test) -> {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
            String example = a.getString(0);
            if (a != null) {
                a.recycle();
            }
        })));
    }
}