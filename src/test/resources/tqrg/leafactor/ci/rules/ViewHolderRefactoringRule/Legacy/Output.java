package test.resources.tqrg.leafactor.ci.rules.ViewHolderRefactoringRule.Legacy;

public abstract class Input extends BaseAdapter {
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static class Adapter1 extends ViewHolderSample {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }

    public static class Adapter2 extends ViewHolderSample {
        LayoutInflater mInflater;

        static class ViewHolderItem {
            public TextView text = null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = (convertView == null) ? mInflater.inflate(R.layout.your_layout, null) : convertView;
            ViewHolderItem viewHolderItem = (ViewHolderItem) convertView.getTag();
            if (viewHolderItem == null) {
                viewHolderItem = new ViewHolderItem();
                convertView.setTag(viewHolderItem);
                viewHolderItem.text = (TextView) convertView.findViewById(R.id.text);
            }

            TextView text = viewHolderItem.text;
            text.setText("Position " + position);

            return convertView;
        }
    }

    public static class Adapter3 extends ViewHolderSample {
        LayoutInflater mInflater;

        static class ViewHolderItem {
            public TextView text = null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = (convertView == null) ? mInflater.inflate(R.layout.your_layout, null) : convertView;
            ViewHolderItem viewHolderItem = (ViewHolderItem) v.getTag();
            if (viewHolderItem == null) {
                viewHolderItem = new ViewHolderItem();
                convertView.setTag(viewHolderItem);
                viewHolderItem.text = (TextView) v.findViewById(R.id.text);
            }

            TextView text = viewHolderItem.text;
            text.setText("Position " + position);

            return v;
        }
    }

    public static class Adapter5 extends ViewHolderSample {
        LayoutInflater mInflater;

        static class ViewHolderItem {
            public TextView text = null;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // Already using View Holder pattern
            convertView = convertView == null ? mInflater.inflate(R.layout.your_layout, null) : convertView;
            ViewHolderItem viewHolderItem = (ViewHolderItem) convertView.getTag();
            if (viewHolderItem == null) {
                viewHolderItem = new ViewHolderItem();
                convertView.setTag(viewHolderItem);
                viewHolderItem.text = (TextView) convertView.findViewById(R.id.text);
            }

            TextView text = viewHolderItem.text;
            text.setText("Position " + position);

            return convertView;
        }
    }

    public static class Adapter7 extends ViewHolderSample {
        LayoutInflater inflater;

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View rootView = convertView;
            final int itemViewType = getItemViewType(position);
            switch (itemViewType) {
                case 0:
                    if (rootView != null)
                        return rootView;
                    rootView = inflater.inflate(R.layout.your_layout, parent, false);
                    break;
            }
            return rootView;
        }
    }

    /* TODO low priority VieHolder cornercase
    public static class CornerCase extends ViewHolderSample {
        LayoutInflater inflater;
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View rootView = convertView;
            if (rootView != null)
                return rootView;
            rootView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return rootView;
        }
    }*/

    private static class R {
        public static class layout {
            public static final int your_layout = 2;
        }
        public static class id {
            public static final int text = 2;
        }
    }
}