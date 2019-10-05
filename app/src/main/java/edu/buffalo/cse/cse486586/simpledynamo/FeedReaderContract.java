package edu.buffalo.cse.cse486586.simpledynamo;

import android.provider.BaseColumns;

public class FeedReaderContract {
    private FeedReaderContract() {}

    //https://developer.android.com/training/data-storage/sqlite.html
    //This stores the name of the the table and column names for use.
    public static class FeedEntry implements BaseColumns {
    public static final String TABLE_NAME = "Messages";
    public static final String KEY_NAME = "key";
    public static final String VALUE_NAME = "value";
    }
}
