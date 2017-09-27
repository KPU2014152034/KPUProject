package kr.ac.kpu.wheeling.blackbox.gallery;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import kr.ac.kpu.wheeling.BuildConfig;
import kr.ac.kpu.wheeling.R;
import kr.ac.kpu.wheeling.helper.SQLiteHandler;
import kr.ac.kpu.wheeling.helper.SessionManager;
import kr.ac.kpu.wheeling.login.LoginActivity;
import kr.ac.kpu.wheeling.view.TrackView;
import kr.ac.kpu.wheeling.view.ViewActivity;
import kr.ac.kpu.wheeling.view.ViewMainActivity;

public class GalleryActivity extends AppCompatActivity {
    private AsyncTask<Integer, String, Integer> mProgressDlg;

    private ProgressDialog dialog = null;
    private ListView fileList;
    private ArrayList<CommonData> arrayList;
    private GroupAdapter adapter;
    private Context context;
    private Cursor mVideoCursor;
    private int serverResponseCode = 0;
    final String uploadFilePath = getVideoStorageDir(context, "wheeling").getAbsolutePath();//경로를 모르겠으면, 갤러리 어플리케이션 가서 메뉴->상세 정보
    final String uploadServerUrl = "http://wheeling.tk/upload.php";
    private SQLiteHandler db;
    private SessionManager session;
    private String uid;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        setTitle("Gallery");
        context = getApplicationContext();
        makeFileList();
        MediaScanner mediaScanner = MediaScanner.newInstance(this);

        mediaScanner.mediaScanning(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/wheeling");

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Fetching user details from sqlite
        HashMap<String, String> user = db.getUserDetails();

        String name = user.get("name");
        String nickname = user.get("nickname");
        email = user.get("email");

        Log.d("GALLERY", "USER EMAIL: " + email);

        String[] splitted = email.split("@");
        String[] splitted2 = splitted[1].split("\\.");
        uid = splitted[0].trim() + splitted2[0].trim();

        Log.d("GALLERY", "USER UID: " + uid + "BLANK");
    }

    /**
     * Logging out the user. Will set isLoggedIn flag to false in shared
     * preferences Clears the user data from sqlite users table
     * */
    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(GalleryActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_sync:
                final String[] proj={
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.TITLE,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.RESOLUTION,
                };

                final Cursor mUploadCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, "_data LIKE"+"'"+getVideoStorageDir(context, "wheeling").getAbsolutePath()+"%'", null, null);

                Toast.makeText(GalleryActivity.this, "Upload Start...", Toast.LENGTH_SHORT).show();
                mProgressDlg = new ProgressDlg(GalleryActivity.this).execute(mUploadCursor.getCount());
                /*
                    new Thread(new Runnable() {
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(GalleryActivity.this, "Upload Start...", Toast.LENGTH_SHORT).show();
                                }
                            });


                            //dialog.dismiss();
                        }
                    }).start();
                */
                return true;
            case R.id.action_logout:
                logoutUser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static boolean canWritable(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWritable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWritable = true;
            Log.d("STORAGE", "WRITABLE" + " Available: " + mExternalStorageAvailable + ", Writable: " + mExternalStorageWritable);
            return true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWritable = false;
            Log.d("STORAGE", "READABLE" + " Available: " + mExternalStorageAvailable + ", Writable: " + mExternalStorageWritable);
            return false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWritable = false;
            Log.e("STORAGE", "ERROR" + " Available: " + mExternalStorageAvailable + ", Writable: " + mExternalStorageWritable);
            return false;
        }
    }

    public File getVideoStorageDir(Context context, String albumName){
        //File file = new File(Environment.getExternalStorageDirectory(), albumName);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), albumName);

        if(canWritable() && !file.isDirectory()) {
            if (!file.mkdirs()) {
                Log.e("STORAGE", "Directory not created");
            }
        }

        return file;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<CommonData> mGetBVideoList(){
        ArrayList<CommonData> mTempVideoList = new ArrayList<>();


        String[] proj = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.RESOLUTION,
        };


        Context context = getApplicationContext();

        String videoPath[] = {"'"+getVideoStorageDir(context, "wheeling").getAbsolutePath()+"%'"};
        Log.d("PATH", "PATH: "+videoPath[0]);
        mVideoCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, "_data LIKE"+"'"+getVideoStorageDir(context, "wheeling").getAbsolutePath()+"%'", null, null);
        //mVideoCursor = managedQuery(videoPathUri, proj, null, null, null);

        if(mVideoCursor != null && mVideoCursor.moveToFirst())
        {
            int id = mVideoCursor.getColumnIndex(MediaStore.Video.Media._ID);
            int size = mVideoCursor.getColumnIndex(MediaStore.Video.Media.SIZE);
            int title = mVideoCursor.getColumnIndex(MediaStore.Video.Media.TITLE);
            int data = mVideoCursor.getColumnIndex(MediaStore.Video.Media.DATA);
            int duration = mVideoCursor.getColumnIndex(MediaStore.Video.Media.DURATION);
            int date = mVideoCursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED);
            int resolution = mVideoCursor.getColumnIndex(MediaStore.Video.Media.RESOLUTION);
            do
            {
                CommonData mTempCommonData = new CommonData();

                String filename = mVideoCursor.getString(data);
                long video_date = mVideoCursor.getLong(date);
                Date mDate = new Date(video_date);

                mTempCommonData.mVideoId = mVideoCursor.getLong(id);
                mTempCommonData.mVideoFilePath = filename; //FILE PATH
                mTempCommonData.mVideoDuration = mVideoCursor.getLong(duration);
                mTempCommonData.mVideoSize = mVideoCursor.getLong(size);
                mTempCommonData.mVideoTitle = mVideoCursor.getString(title);


                Log.e("GALLERY","mVideoFilePath :"+filename);
                Log.e("GALLERY","mVideoTitle :"+mVideoCursor.getString(title));

                mTempVideoList.add(mTempCommonData);
                mTempCommonData = null;

            }while(mVideoCursor.moveToNext());
        } else{
            Log.e("GALLERY", "mVideoCursor is NULL");
        }

        try{
            if(mVideoCursor!=null) mVideoCursor.close();
        }catch(Exception err){}

        return mTempVideoList;
    }

    public Bitmap mGetVideoThumbnailImg(long id){
        ContentResolver mCrThumb = this.getContentResolver();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        Bitmap mVideoThumbnailBm = MediaStore.Video.Thumbnails.getThumbnail(mCrThumb, id, MediaStore.Video.Thumbnails.MICRO_KIND, options);

        if(mVideoThumbnailBm != null){
            Log.d("THUMBNAIL", "Thumbnail Width = "+mVideoThumbnailBm.getWidth());
            Log.d("THUMBNAIL", "Thumbnail Height = "+mVideoThumbnailBm.getHeight());
        }

        mCrThumb = null;
        options = null;


        return mVideoThumbnailBm;
    }

    public void makeFileList(){
        fileList = (ListView)findViewById(R.id.listView);
        arrayList = mGetBVideoList();
        adapter = new GroupAdapter(this, R.layout.gallery_listview, arrayList);
        fileList.setAdapter(adapter);

        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CommonData item = arrayList.get(position);

                String fileTitle=item.mVideoTitle.toString();
                int bid= db.getblackbox_input_filetitle(fileTitle); // 해당하는 bid 검색
                File file=new File(item.mVideoFilePath);

                Intent intent = new Intent(GalleryActivity.this, ViewMainActivity.class);
                intent.putExtra("FileTitle",fileTitle);
                intent.putExtra("FilePath",file.toString());
                intent.putExtra("FileBid",bid);
                startActivity(intent);
                /*
                CommonData item = arrayList.get(position);

                File file=new File(item.mVideoFilePath);
                Uri uriFromVideoFile = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
                if(file.isFile() && file.canRead()){

                    Intent intent = new Intent(GalleryActivity.this,ViewActivity.class);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(uriFromVideoFile, "video/*");
                    Log.d("GALLERY", "Video Start: "+uriFromVideoFile.toString());
                    PackageManager pm = context.getPackageManager();
                    if (intent.resolveActivity(pm) != null) {
                        startActivity(intent);
                    }
                }
                */
            }
        });

        fileList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                PopupMenu popup = new PopupMenu(getApplicationContext(), view);

                getMenuInflater().inflate(R.menu.gallery_longclick, popup.getMenu());

                final int index = position;

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()) {
                            case R.id.popup_sync:
                                Log.d("POPUP", "sync");
                                return true;
                            case R.id.popup_delete:
                                Log.d("POPUP", "delete");
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                return false;
            }
        });
    }


    public void startMediaScanning(Context context, String fileName){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + getVideoStorageDir("wheeling") + "/" + fileName));
            //final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            //final Uri contentUri = Uri.fromFile(file);
            //scanIntent.setData(contentUri);
            context.sendBroadcast(intent);
            Log.d("BROADCAST", "Broadcast Complete!");
        } else {
            final Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory()));
            context.sendBroadcast(intent);
            Log.d("BROADCAST", "Broadcast Complete!(Low ver)");
        }
    }

    public File getVideoStorageDir(String albumName){
        //File file = new File(Environment.getExternalStorageDirectory(), albumName);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), albumName);

        if(canWritable() && !file.isDirectory()) {
            if (!file.mkdirs()) {
                Log.e("STORAGE", "Directory not created");
            }
        }

        return file;
    }
    
    private class GroupAdapter extends ArrayAdapter<Object> {
        private ArrayList<CommonData> item;
        private CommonData temp;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public GroupAdapter(Context ctx, int resourceID, ArrayList item) {
            super(ctx, resourceID, item);

            this.item = item;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if(v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.gallery_listview, null);
            }
            temp = item.get(position);

            if(temp != null) {
                ImageView icon = (ImageView)v.findViewById(R.id.imageView1);
                TextView name = (TextView)v.findViewById(R.id.textView1);
                TextView name2 = (TextView)v.findViewById(R.id.textView2);
                Bitmap bmp = mGetVideoThumbnailImg(temp.mVideoId);

                icon.setImageBitmap(bmp);
                name.setText(temp.mVideoTitle);
                name2.setText(temp.mVideoFilePath);
            }
            return v;
        }
    }

    public int uploadFile(String sourceFileUri, final String uploadFileName) {

        String fileName = sourceFileUri + "/" + uploadFileName;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(fileName);

        if (!sourceFile.isFile()) {

            //dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    +uploadFilePath + "/" + uploadFileName);

            runOnUiThread(new Runnable() {
                public void run() {
                    //messageText.setText("Source File not exist :"
                    //        +uploadFilePath + "" + uploadFileName);
                }
            });

            return 0;

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(uploadServerUrl);


                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\""
                        + fileName + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necessary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"userid\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(uid);
                dos.writeBytes(lineEnd);

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"useremail\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(email);
                dos.writeBytes(lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    runOnUiThread(new Runnable() {
                        public void run() {

                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
                                    +uploadFileName;

                            //messageText.setText(msg);
                            Toast.makeText(GalleryActivity.this, "File Upload Complete.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                //dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(GalleryActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                //dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(GalleryActivity.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("UPLOAD", "Exception : "
                        + e.getMessage(), e);
            }
            //dialog.dismiss();
            return serverResponseCode;

        } // End else block
        //출처: http://taetanee.tistory.com/entry/안드로이드-php-파일-전송-예제 [좋은 정보]
    }



/*
        //actionBar 객체를 가져올 수 있다.
        ActionBar actionBar = getSupportActionBar();

        //메뉴바에 '<' 버튼이 생긴다.(두개는 항상 같이다닌다)
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        //출처: http://ande226.tistory.com/141 [안디스토리]
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery_menu, menu);
        return true;
    }
    */
    public class ProgressDlg extends AsyncTask<Integer, String, Integer> {

        private ProgressDialog mDlg;
        private Context mCtx;

        //생성자
        public ProgressDlg(Context ctx) {
            mCtx = ctx;
        }

        @Override
        protected void onPreExecute() {
            //ProgressDialog 세팅
            mDlg = new ProgressDialog(mCtx);
            //스타일 설정
            mDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //프로그래스 다이얼로그 나올 때 메시지 설정.
            mDlg.setMessage("Uploading Files... Please Wait...");
            //세팅된 다이얼로그를 보여줌.
            mDlg.show();

            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            //프로그래스바 최대치가 몇인지 설정하는 변수
            final int taskCnt = params[0];
            //프로그래스바 최대치 설정
            publishProgress("max", Integer.toString(taskCnt));
            final String[] proj={
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.SIZE,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.RESOLUTION,
            };

            final Cursor mUploadCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, "_data LIKE"+"'"+getVideoStorageDir(context, "wheeling").getAbsolutePath()+"%'", null, null);

            if(null!=mUploadCursor && mUploadCursor.moveToFirst()) {
                int i = 1;
                do {
                    //dialog = ProgressDialog.show(GalleryActivity.this, "", "Uploading file... (" + i + "/" + mUploadCursor.getCount()+")", true);
                    uploadFile(uploadFilePath, mUploadCursor.getString(mUploadCursor.getColumnIndex(MediaStore.Video.Media.TITLE))+".mp4");
                    publishProgress("progress", Integer.toString(i),
                            "Task " + Integer.toString(i) + " number");
                    i++;
                }while(mUploadCursor.moveToNext());
            }
            /*
            for (int i = 0; i < taskCnt; i ++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //프로그래스바 현재 진행상황 설정
                publishProgress("progress", Integer.toString(i),
                        "Task " + Integer.toString(i) + " number");
            }
            */
            //PostExecute로 리턴
            return taskCnt;
        }

        //프로그래스가 업데이트 될때 호출
        @Override
        protected void onProgressUpdate(String... values) {
            if (values[0].equals("progress")) {
                mDlg.setProgress(Integer.parseInt(values[1]));
                mDlg.setMessage(values[2]);
            } else if (values[0].equals("max")){
                mDlg.setMax(Integer.parseInt(values[1]));
            }
        }

        //Background에서 처리가 완료되면 호출
        @Override
        protected void onPostExecute(Integer integer) {
            //다이얼로그를 없앰
            mDlg.dismiss();
            Toast.makeText(mCtx, Integer.toString(integer) + " total sum",
                    Toast.LENGTH_SHORT).show();
        }


        //출처: http://tony-programming.tistory.com/entry/ProgressDialog-만들기-예제 [Tony Programming]
    }
}

