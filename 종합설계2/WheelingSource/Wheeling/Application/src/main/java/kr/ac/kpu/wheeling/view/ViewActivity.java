package kr.ac.kpu.wheeling.view;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import kr.ac.kpu.wheeling.R;
import kr.ac.kpu.wheeling.blackbox.Camera2VideoFragment;
import kr.ac.kpu.wheeling.blackbox.gallery.GalleryActivity;
import kr.ac.kpu.wheeling.helper.SQLiteHandler;
import kr.ac.kpu.wheeling.object.TrackObject;

import static com.google.android.gms.plus.PlusOneDummyView.TAG;

public class ViewActivity extends Fragment {
    private AsyncTask<Integer, String, Integer> mProgressDlg;
    private SQLiteHandler sqLiteHandler;
    private ArrayList<TrackObject> Tlist;
    String mJsonString;
    private int serverResponseCode = 0;
    private static final String TAG_JSON="webnautes";
    private static final String TAG_ID = "bid";

    final String uploadFilePath = getVideoStorageDir(getActivity(), "wheeling").getAbsolutePath();//경로를 모르겠으면, 갤러리 어플리케이션 가서 메뉴->상세 정보
    final String uploadServerUrl = "http://wheeling.tk/upload.php";

    public final static int URL = 1;
    public final static int SDCARD = 2;
    VideoView videoView;
    Button btnStart, btnStop,btnUpload;
    String filePath;
    int fileBid;
    Chronometer chronometer;
    LinearLayout Fview_track;
    int time=0;
    private int listsize = 0;
    private String uid;
    private String email;
    private String fileTitle;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            time = getArguments().getInt("LTime");
        }
        sqLiteHandler = new SQLiteHandler(getActivity());
        Tlist = new ArrayList<TrackObject>();
        Intent intent=getActivity().getIntent();
        filePath=intent.getStringExtra("FilePath");
        fileBid=intent.getIntExtra("FileBid",0);
        fileTitle=intent.getStringExtra("FileTitle");
        Toast.makeText(getActivity(), ""+fileTitle, Toast.LENGTH_SHORT).show();


        HashMap<String, String> user = sqLiteHandler.getUserDetails();

        String name = user.get("name");
        String nickname = user.get("nickname");
        email = user.get("email");

        Log.d("GALLERY", "USER EMAIL: " + email);

        String[] splitted = email.split("@");
        String[] splitted2 = splitted[1].split("\\.");
        uid = splitted[0].trim() + splitted2[0].trim();
    }
    public static ViewActivity newInstance(int time) {
        ViewActivity fragment=new ViewActivity();
        Bundle args = new Bundle();
        args.putInt("LTime",time);
        fragment.setArguments(args);
        return new ViewActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_view, null);
        chronometer=(Chronometer)view.findViewById(R.id.chronometer10);
        videoView = (VideoView) view.findViewById(R.id.view);
        btnStart = (Button) view.findViewById(R.id.btnStart);
        btnStop = (Button) view.findViewById(R.id.btnStop);
        btnUpload = (Button)view.findViewById(R.id.btnUpload);
        Fview_track=(LinearLayout)view.findViewById(R.id.Fview_track) ;

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.setVisibility(View.INVISIBLE);
                btnStart.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
            }
        });
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // Toast.makeText(getActivity(), ""+fileBid, Toast.LENGTH_SHORT).show();
                InsertData indata = new InsertData(); // MYSQL
                GetData getdata = new GetData(); // MYSQL GET BiD
               Tlist=sqLiteHandler.getTracking_info(fileBid);
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
                final Cursor mUploadCursor = getActivity().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, "_data LIKE"+"'"+getVideoStorageDir(getActivity(), "wheeling").getAbsolutePath()+"/" +  fileTitle + "%'", null, null);
                mProgressDlg = new ProgressDlg(getActivity()).execute(mUploadCursor.getCount());
               // Log.d(TAG, "sqLiteHandler.getTracking_info(fileBid) : "+ Tlist +"\n");

                getdata.execute("http://wheeling.tk/track_select.php");// MYSQL Get Bid

                indata.execute(Tlist); // MYSQL
            }
        });
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                time = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase())); // 1000
                playVideo1(time);
                if ((time*1000) >= listsize) {
                    chronometer.stop();
                }
            }
        });
        //미디어컨트롤러 추가하는 부분
        MediaController controller = new MediaController(getActivity());
        videoView.setMediaController(controller);

        //비디오뷰 포커스를 요청함
        videoView.requestFocus();

        int type = URL;
        switch (type) {
            case URL:
                //동영상 경로가 URL일 경우
                videoView.setVideoURI(Uri.parse(filePath));
                break;

            case SDCARD:
                //동영상 경로가 SDCARD일 경우
                String path = Environment.getExternalStorageDirectory()
                        + "/TestVideo.mp4";
                videoView.setVideoPath(path);
                break;
        }


        //동영상이 재생준비가 완료되었을 때를 알 수 있는 리스너 (실제 웹에서 영상을 다운받아 출력할 때 많이 사용됨)
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                //Toast.makeText(getActivity(),"동영상이 준비되었습니다. \n'시작' 버튼을 누르세요", Toast.LENGTH_SHORT).show();
               // playVideo(Ltime);6
                chronometer.start();
            }
        });

        //동영상 재생이 완료된 걸 알 수 있는 리스너
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //동영상 재생이 완료된 후 호출되는 메소드
               // Toast.makeText(getActivity(),"동영상 재생이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                videoView.pause();
                chronometer.stop();
            }
        });
       // playVideo();
        return view;

    }


    //정지 버튼 onClick Method
    public void StopButton(View v) {
        stopVideo();
    }

    //동영상 재생 Method
    private void playVideo1(int time) {
        //비디오를 처음부터 재생할 때 0으로 시작(파라메터 sec)
        videoView.seekTo(time);
        videoView.start();
    }
    private void playVideo() {
        //비디오를 처음부터 재생할 때 0으로 시작(파라메터 sec)
        videoView.seekTo(0);
        videoView.start();
    }
    //동영상 정지 Method
    private void stopVideo() {
        //비디오 재생 잠시 멈춤
        videoView.pause();
        //비디오 재생 완전 멈춤
//        videoView.stopPlayback();
        //videoView를 null로 반환 시 동영상의 반복 재생이 불가능
//        videoView = null;
    }


    class InsertData extends AsyncTask<ArrayList<TrackObject>, Void, String> {
        ProgressDialog progressDialog;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(getActivity(),
                    "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Toast.makeText(getActivity(),result, Toast.LENGTH_SHORT).show();
            // mTextViewResult.setText(result);
            Log.d(TAG, "POST response  - " + result);
        }


        @Override
        protected String doInBackground(ArrayList<TrackObject>... params) {
            Log.d(TAG, " dolnBackground - size"+ params[0].size());
            String result="";
            for (int cnt = 1; cnt < params[0].size(); cnt++) {
                String bid = (String) String.valueOf(params[0].get(cnt).getBid());
                String time= (String) String.valueOf(params[0].get(cnt).getTime());
                String lat = (String) String.valueOf(params[0].get(cnt).getLat());
                String lon = (String) String.valueOf(params[0].get(cnt).getLon());
                String alt = (String) String.valueOf(params[0].get(cnt).getAlt());
                String distance = (String) String.valueOf(params[0].get(cnt).getDistance());
                String speed = (String) String.valueOf(params[0].get(cnt).getSpeed());
                String maxspeed = (String) String.valueOf(params[0].get(cnt).getMaxspeed());
                String avrspeed = (String) String.valueOf(params[0].get(cnt).getAvrspeed());
                String address = (String)params[0].get(cnt).getAddress();
                String mtime = (String)String.valueOf(params[0].get(cnt).getMtime());

                String postParameters = "bid=" + bid + "&time=" + time + "&lat=" + lat + "&lon=" + lon + "&alt=" + alt + "&distance=" + distance + "&speed=" + speed + "&maxspeed=" + maxspeed + "&avrspeed=" + avrspeed +  "&address=" + address + "&mtime="+ mtime;
                Log.d(TAG, "POST postParameters - " + postParameters);
                result=register_location(postParameters);

            }
            return result;
        }


    }
    public String register_location(String postParameters){
        try {
            Thread.sleep(100);
            java.net.URL url = new URL("http://wheeling.tk/track_insert.php");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


            httpURLConnection.setReadTimeout(5000);
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.setRequestMethod("POST");
            //httpURLConnection.setRequestProperty("content-type", "application/json");
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();


            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(postParameters.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();


            int responseStatusCode = httpURLConnection.getResponseCode();
            Log.d(TAG, "POST response code - " + responseStatusCode);

            InputStream inputStream;
            if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                inputStream = httpURLConnection.getInputStream();
            } else {
                inputStream = httpURLConnection.getErrorStream();
            }


            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuilder sb = new StringBuilder();
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }


            bufferedReader.close();


            return sb.toString();


        } catch (Exception e) {

            Log.d(TAG, "InsertData: Error ", e);

            return new String("Error: " + e.getMessage());
        }

    }


    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(getActivity(),"Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            //  mTextViewResult.setText(result);
            Log.d(TAG, "response  - " + result);

            if (result == null) {

                // mTextViewResult.setText(errorString);
                Log.d("TrackerFragment_ bid" , "errorString :" + errorString);
            } else {

                mJsonString = result;
                showResult();
            }
        }


        @Override
        protected String doInBackground(String... params) {

            try {

                URL url = new URL("http://wheeling.tk/track_select.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.connect();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);
                errorString = e.toString();

                return null;
            }

        }
    }
    private void showResult(){
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for(int i=0;i<jsonArray.length();i++){

                JSONObject item = jsonArray.getJSONObject(i);

                String bid = item.getString(TAG_ID);
                fileBid=Integer.valueOf(bid);
                Log.d("showResult" ,"bid   : "+   bid);

            }
            Toast.makeText(getActivity(), ""+ fileBid , Toast.LENGTH_SHORT).show();
            input_bid(Tlist,fileBid);
            Log.d(TAG, "TOlist-toString()  - " + Tlist.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "showResult : ", e);
        }

    }

    public void input_bid(ArrayList<TrackObject> list,int bid){
        for(int i=0;i<list.size();i++){
            list.get(i).setBid(bid);
        }
    }


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

            final Cursor mUploadCursor = getActivity().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, "_data LIKE"+"'"+getVideoStorageDir(getActivity(), "wheeling").getAbsolutePath()+"/" +  fileTitle + "%'", null, null);

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

            getActivity().runOnUiThread(new Runnable() {
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

                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {

                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
                                    +uploadFileName;

                            //messageText.setText(msg);
                            Toast.makeText(getActivity(), "File Upload Complete.",
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

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(getActivity(), "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                //dialog.dismiss();
                e.printStackTrace();

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        //messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(getActivity(), "Got Exception : see logcat ",
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
}
