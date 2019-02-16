package rules.RecycleRefactoringRule.VariableShouldRecycle;

public class Recycle {
    public void wrong1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0);
        Test b = a.get((test) -> {
            final TypedArray d = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
            if(d != null) {
                d.recycle();
            }
        });
    }
}