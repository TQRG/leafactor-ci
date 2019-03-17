public abstract class ViewHolderSample extends BaseAdapter {
    public static class Adapter1 extends ViewHolderSample {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
    public static class Adapter5 extends ViewHolderSample {
        LayoutInflater mInflater;

        static class ViewHolderItem {
            TextView text;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = convertView == null ? mInflater.inflate(R.layout.your_layout, null) : convertView;

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