public abstract class ViewHolderSample extends BaseAdapter {
    public static class Adapter2 extends ViewHolderSample {
        LayoutInflater mInflater;

        static class ViewHolderItem {
            TextView text;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolderItem viewHolderItem;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.your_layout, null);
                viewHolderItem = new ViewHolderItem();
                viewHolderItem.text = (TextView) convertView.findViewById(R.id.text);
                convertView.setTag(viewHolderItem);
            } else {
                viewHolderItem = (ViewHolderItem) convertView.getTag();
            }
            TextView text = viewHolderItem.text;
            text.setText("Position " + position);

            return convertView;
        }
    }
}
