package test.resources.com.leafactor.cli.rules.ViewHolderRefactoringRule.ShouldRefactorConvertView;

import R.layout.your_layout;
import android.R.layout.simple_list_item_1;

public abstract class Input extends BaseAdapter {

    public static class Adapter1 extends ViewHolderSample {
        LayoutInflater inflater;

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View rootView;
            final int itemViewType = getItemViewType(position);
            switch (itemViewType) {
                case 0:
                    rootView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                    break;
            }
            return rootView;
        }
    }

    public static class Adapter2 extends ViewHolderSample {
        LayoutInflater mInflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.your_layout, null);
            return convertView;
        }
    }
}