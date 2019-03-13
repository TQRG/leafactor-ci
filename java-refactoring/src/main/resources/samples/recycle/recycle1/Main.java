public class Teste {
    public void testMultipleAssignment(Uri uri, ContentProvider provider){
        Cursor query = provider.query(uri, null, null, null, null);
        query.getLong(0); // Teste

    }

    public void testMultipleAssignment1(Uri uri, ContentProvider provider){
        Cursor query = provider.query(uri, null, null, null, null);
        query.getLong(0); // Teste
    }

    public void testMultipleAssignment2(Uri uri, ContentProvider provider){
        Cursor query = provider.query(uri, null, null, null, null);
        query.getLong(0); // Teste
        System.out.println();
    }

    public void testMultipleAssignment3(Uri uri, ContentProvider provider){
        Cursor query = provider.query(uri, null, null, null, null);
        query.getLong(0);
    }
}

//public class Teste {
//    public void testMultipleAssignment(Uri uri, ContentProvider provider) {
//        a.get(() -> {
//            Cursor query = provider.query(uri, null, null, null, null);
//            query.getLong(0); // Teste
//        });
//    }
//}

