public abstract class ViewHolderSample extends BaseAdapter {
    public static class Adapter1 extends ViewHolderSample {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }

    public static class Adapter2 extends ViewHolderSample {
        LayoutInflater mInflater;

        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView != null) {
                return convertView
            }
            return  mInflater.inflate(R.layout.your_layout, null);
        }
    }

    public static class Adapter4 extends ViewHolderSample {
        LayoutInflater mInflater;

        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView != null) {
                return convertView
            }
            convertView = mInflater.inflate(R.layout.your_layout, null);
            return convertView;
        }
    }

    public static class Adapter5 extends ViewHolderSample {
        LayoutInflater mInflater;

        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = convertView == null ? mInflater.inflate(R.layout.your_layout, null) : convertView;
            return convertView;
        }
    }
}