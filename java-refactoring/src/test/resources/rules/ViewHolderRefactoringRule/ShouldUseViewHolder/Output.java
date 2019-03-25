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
                viewHolderItem = new ViewHolderItem();
                convertView.setTag(viewHolderItem);
            }
            viewHolderItem.text = (TextView) convertView.findViewById(R.id.text);
            text.setText("Position " + position);

            return convertView;
        }
    }

    public static class Adapter3 extends ViewHolderSample {
        LayoutInflater mInflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = convertView != null ? convertView : mInflater.inflate(R.layout.your_layout, null);
            ViewHolderItem viewHolderItem = (ViewHolderItem) convertView.getTag();
            if(viewHolderItem == null) {
                viewHolderItem = new ViewHolderItem();
                convertView.setTag(viewHolderItem);
                viewHolderItem.text1 = (TextView) convertView.findViewById(R.id.text1);
                viewHolderItem.text2 = (TextView) convertView.findViewById(R.id.text2);
                viewHolderItem.text3 = (TextView) convertView.findViewById(R.id.text3);
                viewHolderItem.text4 = (TextView) convertView.findViewById(R.id.text4);
                viewHolderItem.text5 = (TextView) convertView.findViewById(R.id.text5);
                viewHolderItem.text6 = (TextView) convertView.findViewById(R.id.text6);
            }
            TextView text1 = viewHolderItem.text1;
            TextView text2 = viewHolderItem.text2;
            TextView text3 = viewHolderItem.text3;
            TextView text4 = viewHolderItem.text4;
            TextView text5 = viewHolderItem.text5;
            TextView text6 = viewHolderItem.text6;
            text1.setText("Position " + position);
            return convertView;
        }
    }
}