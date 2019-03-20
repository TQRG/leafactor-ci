public abstract class ViewHolderSample extends BaseAdapter {
    public static class Adapter2 extends ViewHolderSample {
        LayoutInflater mInflater;

        static class ViewHolderItem {
            TextView text;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = convertView != null ? convertView : mInflater.inflate(R.layout.your_layout, null);

            ViewHolderItem viewHolderItem = (ViewHolderItem) convertView.getTag();
            if(viewHolderItem == null) {
                convertView.setTag(new ViewHolderItem());
            }
            viewHolderItem.text = (TextView) convertView.findViewById(R.id.text);
            text.setText("Position " + position);

            return convertView;
        }
    }
}