package recycle1;

public class Recycle {
    public void wrong1(AttributeSet attrs , int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
        String example = a.getString(0);
        // Advanced example with Lambda expressions
//        Test b = a.get((test) -> {
//            final c = 5;
//            final TypedArray d = getContext().obtainStyledAttributes(attrs, new int[]{0}, defStyle, 0);
//        });
        if (a == null) {

        }
    }
}
