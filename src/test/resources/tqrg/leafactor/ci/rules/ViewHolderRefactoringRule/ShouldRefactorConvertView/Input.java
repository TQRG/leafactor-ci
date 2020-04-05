package test.resources.tqrg.leafactor.ci.rules.ViewHolderRefactoringRule.ShouldRefactorConvertView;

import R.layout.your_layout;

public abstract class Input extends BaseAdapter {

    public static class Adapter2 extends ViewHolderSample {
        LayoutInflater mInflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.your_layout, null);
            return convertView;
        }
    }
}