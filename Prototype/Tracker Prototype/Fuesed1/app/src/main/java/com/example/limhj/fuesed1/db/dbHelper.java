package com.example.limhj.fuesed1.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by limhj on 2017-02-10.
 */

public class dbHelper extends SQLiteOpenHelper{
    private static final String DATABASE_NAME="mydb";
    private static final int DATABASE_VERSION=2;
    public dbHelper(Context context)
    {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE riding ( _id INTEGER PRIMARY KEY  AUTOINCREMENT, name TEXT, tel TEXT);");
    }

    @Override

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public void insert(SQLiteDatabase db){
        db.execSQL("INSERT INTO riding VALUES (");

    }
}
