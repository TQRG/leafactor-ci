public abstract class ViewHolderSample extends BaseAdapter {
    public static class Adapter1 extends ViewHolderSample {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
    public static class Adapter5 extends ViewHolderSample {
        LayoutInflater mInflater;

        public View getView(int position, View convertView, ViewGroup parent) {
            // Already using View Holder pattern
            convertView = convertView == null ? mInflater.inflate(R.layout.your_layout, null) : convertView;

            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText("Position " + position);

            return convertView;
        }
    }
}