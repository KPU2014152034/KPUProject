package kr.ac.kpu.wheeling.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import kr.ac.kpu.wheeling.object.TrackObject;

/**
 * Created by limhj_000 on 2017-06-20.
 */

public class SQLiteHandler extends SQLiteOpenHelper {
    private static final String TAG = SQLiteHandler.class.getSimpleName();
    private ArrayList<TrackObject> location_list;

    private String Bowner="뭔데";
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 11;

    // Database Name
    private static final String DATABASE_NAME = "WheelingDB";

    // Login table name
    private static final String TABLE_USER = "user";

    // Login Table Columns names
    private static final String KEY_ID = "userid";
    private static final String KEY_EMAIL = "useremail";
    private static final String KEY_NAME = "username";
    private static final String KEY_NICKNAME = "usernick";
    private static final String KEY_REGDATE = "regdate";

    // Track table name
    private static final String TABLE_TRACK = "Track_info";

    // Track Table Columns names
    private static final String KEY_TID = "id";
    private static final String KEY_TBID = "blackbox_id";
    private static final String KEY_TIME = "time";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_ALT = "alt";
    private static final String KEY_DIS = "dis";
    private static final String KEY_Spd = "spd";
    private static final String KEY_Avrspd = "Avrspd";
    private static final String KEY_Mspd = "mspd";
    private static final String KEY_Adr = "adr";
    private static final String KEY_MTIME = "mtime";


    // blackbox Table name
    private static final String TABLE_BLACKBOX = "blackbox";

    // Track Table Columns names
    private static final String KEY_BID = "bid";
    private static final String KEY_BUSERID = "userid_id";
    private static final String KEY_BOWNER = "bowner";
    private static final String KEY_BHREF = "bhref";
    private static final String KEY_BTHUMBNAIL = "bthumbnail";
    private static final String KEY_BFILENAME = "bfilename";
    private static final String KEY_BUPLOADDATE = "buploaddate";


    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOGIN_TABLE = "CREATE TABLE " + TABLE_USER + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_EMAIL + " TEXT UNIQUE,"
                + KEY_NAME + " TEXT," + KEY_NICKNAME + " TEXT,"
                + KEY_REGDATE + " TEXT" + ");";

        String CREATE_BLACKBOX_TABLE = "CREATE TABLE " + TABLE_BLACKBOX + "("
                + KEY_BID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_BOWNER + " TEXT,"
                + KEY_BFILENAME + " TEXT," + KEY_BUPLOADDATE +  " DATETIME DEFAULT (datetime('now','localtime'))"
                //+ " FOREIGN KEY("+ KEY_BOWNER +") REFERENCES "+ TABLE_USER +"("+ KEY_EMAIL +")"
                + ");";


        String CREATE_TRACK_TABLE = "CREATE TABLE " + TABLE_TRACK + "("
                + KEY_TID + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ KEY_TBID + " INTEGER,"
                +  KEY_TIME + " INTEGER," + KEY_LAT + " DOUBLE,"
                + KEY_LON + " DOUBLE," + KEY_ALT + " DOUBLE," + KEY_DIS + " DOUBLE," + KEY_Spd + " DOUBLE," + KEY_Avrspd + " DOUBLE,"
                + KEY_Mspd + " DOUBLE," + KEY_Adr + " TEXT," + KEY_MTIME + " LONG"
               // + " FOREIGN KEY("+ KEY_TBOWNER +") REFERENCES "+ TABLE_BLACKBOX +"("+ KEY_BOWNER +")"
                + ")";

        db.execSQL(CREATE_LOGIN_TABLE);
        db.execSQL(CREATE_BLACKBOX_TABLE);
        db.execSQL(CREATE_TRACK_TABLE);

        Log.d(TAG, "Database tables created");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLACKBOX);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        // Create tables again
        onCreate(db);
    }




    /**
     * Storing user details in database
     * */


    public void addLocation_new(ArrayList<TrackObject> location_list){
        long id;
        // Log.d(TAG, "addLocation -  list.size : " + location_list.size() );
        for(int cnt=0;cnt<location_list.size();cnt++) {
               Log.d(TAG, "addLocation -  list.size : " + location_list.get(cnt).getLat() + " lon : "+ location_list.get(cnt).getLon()
                      +" alt : " + location_list.get(cnt).getAlt());
            SQLiteDatabase db = this.getWritableDatabase();
            try {
                ContentValues values = new ContentValues();
                // values.put(KEY_TBOWNER,); //
                values.put(KEY_TBID, location_list.get(cnt).getBid());
                values.put(KEY_TIME, location_list.get(cnt).getTime()); //
                values.put(KEY_LAT, location_list.get(cnt).getLat()); //
                values.put(KEY_LON, location_list.get(cnt).getLon()); //
                values.put(KEY_ALT, location_list.get(cnt).getAlt()); //
                values.put(KEY_DIS, location_list.get(cnt).getDistance());
                values.put(KEY_Spd, location_list.get(cnt).getSpeed());
                values.put(KEY_Avrspd, location_list.get(cnt).getAvrspeed());
                values.put(KEY_Mspd, location_list.get(cnt).getMaxspeed());
                values.put(KEY_Adr, location_list.get(cnt).getAddress());
                values.put(KEY_MTIME,location_list.get(cnt).getMtime());
                // Inserting Row
                id = db.insert(TABLE_TRACK, null, values);
                Log.d(TAG, "New Location inserted into sqlite: " + id);
            }
            catch (Exception e){
                e.getMessage();
            }
            db.close(); // Closing database connection
        }
    }




    public void select() {
        SQLiteDatabase db = this.getReadableDatabase();
        //String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX +"where bowner='q@q.com'";
        String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX;
        Cursor c = db.rawQuery(selectQuery, null);

        while(c.moveToNext()) {
            String bowner = c.getString(1);
            Log.d(TAG ,"select(String a): "+bowner);
            // Double name = c.getDouble(2);
            //  Double alt = c.getDouble(3);
            // Log.d(TAG,"lat:"+id+",lon:"+name +",alt:"+alt);
        }

    }

    public ArrayList<TrackObject> getlocationALL() {

        location_list=new ArrayList<TrackObject>();

        String selectQuery = "SELECT  * FROM " + TABLE_TRACK;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        //  cursor.moveToFirst();
        while(cursor.moveToNext()) {
            TrackObject trackObject=new TrackObject();
            trackObject.setBid(cursor.getInt(1));
            // Log.d(TAG, "  BOWNER: " + cursor.getString(1));
            trackObject.setTime(cursor.getInt(2));
            trackObject.setLat(cursor.getDouble(3));
            trackObject.setLon(cursor.getDouble(4));
            trackObject.setAlt(cursor.getDouble(5));
            trackObject.setDistance(cursor.getDouble(6));
            trackObject.setSpeed(cursor.getDouble(7));
            trackObject.setAvrspeed(cursor.getDouble(8));
            trackObject.setMaxspeed(cursor.getDouble(9));
            trackObject.setAddress(cursor.getString(10));
            trackObject.setMtime(cursor.getLong(11));
            location_list.add(trackObject);
            Log.d(TAG, " trackObject.toString():  "+trackObject.toString());
            //  user.put("created_at", cursor.getString(4));
        }
        cursor.close();
       //  Log.d(TAG, "Fetching user from locationlist.get(a).toString() : " + location_list.size());
        for(int a=0;a<location_list.size();a++){

          //  Log.d(TAG, "Fetching user from locationlist.get(a).toString() : " + location_list.get(a).getLat() + " lon " + location_list.get(a).getLon() + "alt :" +location_list.get(a).getAlt());
        }
        db.close();
        // return user
        // Log.d(TAG, "Fetching user from Sqlite: " + location.toString());

        return location_list;
    }
    public int getTracking_time(int bid) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT  * FROM " + TABLE_TRACK + " where blackbox_id = '" + bid + "'";
       // String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX;
        int time = 0;
        Cursor c = db.rawQuery(selectQuery, null);
        while(c.moveToNext()) {
            int timee = c.getInt(2);
            if(timee>=time){
                time=timee;
            }

        }
      //  Log.d(TAG, "etTracking_time : "+time );
        return time;
    }
    public ArrayList<TrackObject> getTracking_info(int bid) {

        location_list=new ArrayList<TrackObject>();

        String selectQuery = "SELECT  * FROM " + TABLE_TRACK + " where blackbox_id = '" + bid + "'";
        //select * from myTable where Category = 'category1'
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        //  cursor.moveToFirst();
        while(cursor.moveToNext()) {
            TrackObject trackObject=new TrackObject();
            trackObject.setBid(cursor.getInt(1));
            // Log.d(TAG, "  BOWNER: " + cursor.getString(1));
            trackObject.setTime(cursor.getInt(2));
            trackObject.setLat(cursor.getDouble(3));
            trackObject.setLon(cursor.getDouble(4));
            trackObject.setAlt(cursor.getDouble(5));
            trackObject.setDistance(cursor.getDouble(6));
            trackObject.setSpeed(cursor.getDouble(7));
            trackObject.setAvrspeed(cursor.getDouble(8));
            trackObject.setMaxspeed(cursor.getDouble(9));
            trackObject.setAddress(cursor.getString(10));
            trackObject.setMtime(cursor.getLong(11));
            location_list.add(trackObject);
           // Log.d(TAG, " trackObject.toString():  " +cursor.getInt(0) + " " +trackObject.toString());
            //  user.put("created_at", cursor.getString(4));
        }
        cursor.close();
        /* Log.d(TAG, "Fetching user from locationlist.get(a).toString() : " + location_list.size());
        for(int a=0;a<location_list.size();a++){

            Log.d(TAG, "Fetching user from locationlist.get(a).toString() : " + location_list.get(a).getLat() + " lon " + location_list.get(a).getLon() + "alt :" +location_list.get(a).getAlt());
        }*/
        db.close();
        // return user
        // Log.d(TAG, "Fetching user from Sqlite: " + location.toString());

        return location_list;
    }


    public void addblackbox(String bowner,String filename) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_BOWNER,bowner);
        values.put(KEY_BFILENAME, filename); // Name

        // Inserting Row
        long id = db.insert(TABLE_BLACKBOX, null, values);
        db.close(); // Closing database connection

        Log.d(TAG, "New  addblackbox(String filename): " + bowner + " filename :" + filename);
    }


    public void addblackbox_bid(int bid , String bowner,String filename) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BID,bid);
        values.put(KEY_BOWNER,bowner);
        values.put(KEY_BFILENAME, filename); // Name

        // Inserting Row
        long id = db.insert(TABLE_BLACKBOX, null, values);
        db.close(); // Closing database connection

        Log.d(TAG, "New  addblackbox(String filename): " + bowner + " filename :" + filename);
    }


    public void getblackbox() {
        SQLiteDatabase db = this.getReadableDatabase();
        //String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX +"where bowner='q@q.com'";
        String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX;
        Cursor c = db.rawQuery(selectQuery, null);
        while(c.moveToNext()) {
            int bid = c.getInt(0);
            String bowner = c.getString(1);
            String filename = c.getString(2);
            String d = c.getString(3);
            Log.d(TAG, "getblackbox()  bid : "+bid + "  bowner: "+ bowner + "  filename :" + filename + " date :"+ d);
        }
    }

    public int getblackbox_bid() {
        SQLiteDatabase db = this.getReadableDatabase();
        //String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX +"where bowner='q@q.com'";
        String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX;
        int bid = 0;
        Cursor c = db.rawQuery(selectQuery, null);
        while(c.moveToNext()) {
            int bida = c.getInt(0);
            bid=bida;
        }
         Log.d(TAG, "getblackbox_bid : "+bid );
        return bid;
    }

    public int getblackbox_input_filetitle(String title) {
        SQLiteDatabase db = this.getReadableDatabase();
        //String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX +"where bowner='q@q.com'";
      //  String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX +"where bfilename='" + title + "'";
        String selectQuery = "SELECT  * FROM "+ TABLE_BLACKBOX +" where bfilename = '" + title + ".mp4'";
        int bid = 0;
        Cursor c = db.rawQuery(selectQuery, null);
        while(c.moveToNext()) {
            int bida = c.getInt(0);
            bid=bida;
        }
        Log.d(TAG, "getblackbox_input_filetitle : "+bid );
        return bid;
    }


    /**
     * Storing user details in database
     * */
    public void addUser(String email, String name, String nickname, String regdate) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name); // Name
        values.put(KEY_EMAIL, email); // Email
        values.put(KEY_NICKNAME, nickname); // Nickname
        values.put(KEY_REGDATE, regdate); // registered date

        // Inserting Row
        long id = db.insert(TABLE_USER, null, values);
        db.close(); // Closing database connection

        Log.d(TAG, "New user inserted into sqlite: " + id);
    }

    /**
     * Getting user data from database
     * */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<String, String>();
        String selectQuery = "SELECT  * FROM " + TABLE_USER;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {

            user.put("email", cursor.getString(1));
            user.put("name", cursor.getString(2));
            user.put("nickname", cursor.getString(3));
            user.put("regdate", cursor.getString(4));
        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching user from Sqlite: " + user.toString());

        return user;
    }

    /**
     * Re crate database Delete all tables and create them again
     * */
    public void deleteUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(TABLE_USER, null, null);
        db.close();

        Log.d(TAG, "Deleted all user info from sqlite");
    }

}

