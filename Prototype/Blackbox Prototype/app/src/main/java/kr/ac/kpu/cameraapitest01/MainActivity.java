package kr.ac.kpu.cameraapitest01;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.PictureCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final float FOCUS_AREA_SIZE = 75f;
    Preview preview;
    Camera camera;
    Context ctx;

    private final static int PERMISSIONS_REQUEST_CODE = 100;
    private final static int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;
    private AppCompatActivity mActivity;
    boolean hasVideoTaken;
    private MediaRecorder mediaRecorder;
    Button btnRecord;

    public static void doRestart(Context c){
        try {
            if(c != null){
                PackageManager pm = c.getPackageManager();

                if(pm != null){
                    Intent mStartActivity = pm.getLaunchIntentForPackage(c.getPackageName());
                    if(mStartActivity != null){
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        int mPendingIntentID=223344;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(c, mPendingIntentID, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);

                        AlarmManager armMgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        armMgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

                        System.exit(0);
                    } else {
                        Log.e(TAG, "Wasn't able to restart Application. mStartActivity null");
                    }
                } else{
                    Log.e(TAG, "Wasn't able to restart Application. PackageManager null");
                }
            }else{
                Log.e(TAG, "Wasn't able to restart Application. Context null");
            }
        } catch (Exception e){
            Log.e(TAG, "Wasn't able to restart Application.");
        }
    }

    public void startCamera(){
        if(preview == null){
            preview = new Preview(this, (SurfaceView)findViewById(R.id.cameraView));
            preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            preview.setKeepScreenOn(true);
            ////// 터치 시 포커스 맞추기 TEST
            /*
            preview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (camera != null) {
                        camera.cancelAutoFocus();
                        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);

                        Camera.Parameters parameters = camera.getParameters();
                        if (parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        }
                        if (parameters.getMaxNumFocusAreas() > 0) {
                            List<Camera.Area> mylist = new ArrayList<Camera.Area>();
                            mylist.add(new Camera.Area(focusRect, 1000));
                            parameters.setFocusAreas(mylist);
                        }

                        try {
                            camera.cancelAutoFocus();
                            camera.setParameters(parameters);
                            camera.startPreview();
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                                        Camera.Parameters parameters = camera.getParameters();
                                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                        if (parameters.getMaxNumFocusAreas() > 0) {
                                            parameters.setFocusAreas(null);
                                        }
                                        camera.setParameters(parameters);
                                        camera.startPreview();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            });
            */
        }

        preview.setCamera(null);

        if(camera != null){
            camera.release();
            camera = null;
        }

        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open(CAMERA_FACING);

                camera.setDisplayOrientation(setCameraDisplayOrientation(this, CAMERA_FACING, camera));
                Camera.Parameters params = camera.getParameters();

                params.setRotation(setCameraDisplayOrientation(this, CAMERA_FACING, camera));
                camera.startPreview();
            } catch(RuntimeException re){
                Log.e(TAG, "CAMERA_NOT_FOUNT" + re.getMessage().toString());
            }
        }
        preview.setCamera(camera);
    }

    ////// 터치 시 포커스 맞추기 TEStt
    /*
    private Rect calculateTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(FOCUS_AREA_SIZE * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, preview.getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, preview.getHeight() - areaSize);

        RectF rect = new RectF(left, top, left + areaSize, top + areaSize);
        Log.d(TAG, "log: " + rect.toShortString());

        return round(rect);
    }

    private Rect round(RectF rect) {
        return new Rect(Math.round(rect.left), Math.round(rect.top), Math.round(rect.right), Math.round(rect.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        mActivity = this;
        hasVideoTaken = false;


        //상태바 없애기
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        SurfaceView cameraView = (SurfaceView)findViewById(R.id.cameraView);


        Button btnCapture = (Button)findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
            }
        });
        //http://ilililililililililili.blogspot.com/2013/07/android-database.html

        btnRecord = (Button)findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/recordertest");
                if(!dir.exists()) dir.mkdirs();
                String fileName = "";
                if(!hasVideoTaken){ //녹화중 아닐경우 시작
                    try{
                        if(mediaRecorder == null){
                            mediaRecorder=new MediaRecorder();
                        }
                        fileName = sdCard.getAbsolutePath()+"/recordertest/"+System.currentTimeMillis()+".mp4";
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        mediaRecorder.setVideoEncodingBitRate(10000000);
                        mediaRecorder.setVideoFrameRate(30);
                        //mediaRecorder.setVideoSize(1920, 1080);
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                        mediaRecorder.setOutputFile(fileName);
                        mediaRecorder.setPreviewDisplay(preview.mHolder.getSurface());
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        Log.d("VideoRecorder", "Video Record Started...");
                        btnRecord.setText("STOP");
                        hasVideoTaken=true;
                    }catch (Exception ex){
                        ex.printStackTrace();
                        mediaRecorder.release();
                        mediaRecorder = null;
                    }
                } else {            //녹화중일경우 종료
                    btnRecord.setText("RECORD");
                    hasVideoTaken=false;

                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;

                    ContentValues values = new ContentValues(10);


                    values.put(MediaStore.MediaColumns.TITLE, "RecordedVideo");
                    values.put(MediaStore.Audio.Media.ALBUM, "Video Album");
                    values.put(MediaStore.Audio.Media.ARTIST, "Mike");
                    values.put(MediaStore.Audio.Media.DISPLAY_NAME, "Recorded Video");
                    values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                    values.put(MediaStore.Audio.Media.DATA, fileName);

                    Uri videoUri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                    if (videoUri == null) {
                        Log.d("SampleVideoRecorder", "Video insert failed.");
                        return;
                    }

                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, videoUri));
                }
            }
        });

        Button btnSetting = (Button)findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //API 23 이상이면
                // 런타임 퍼미션 처리 필요

                int hasCameraPermission = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA);
                int hasWriteExternalStoragePermission =
                        ContextCompat.checkSelfPermission(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                int hasRecordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

                if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                        && hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED
                        && hasRecordAudioPermission == PackageManager.PERMISSION_GRANTED){
                    //이미 퍼미션을 가지고 있음
                }
                else {
                    //퍼미션 요청
                    ActivityCompat.requestPermissions( this,
                            new String[]{Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO},
                            PERMISSIONS_REQUEST_CODE);
                }
            }
            else{
            }
        } else {
            Log.e(TAG, "CAMERA NOT SUPPORTED");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Surface will be destroyed when we return, so stop the preview.
        if(camera != null) {
            // Call stopPreview() to stop updating the preview surface
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }

        ((FrameLayout) findViewById(R.id.activity_main)).removeView(preview);
        preview = null;
    }

    private void resetCam(){
        startCamera();
    }

    private void refreshGallery(File file){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    ShutterCallback shutterCallback = new ShutterCallback(){
        public void onShutter(){
            Log.d(TAG, "onShutter'd");
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw");
        }
    };

    //참고 : http://stackoverflow.com/q/37135675
    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            //이미지의 너비와 높이 결정
            int w = camera.getParameters().getPictureSize().width;
            int h = camera.getParameters().getPictureSize().height;

            int orientation = setCameraDisplayOrientation(MainActivity.this,
                    CAMERA_FACING, camera);

            //byte array를 bitmap으로 변환
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeByteArray( data, 0, data.length, options);
            //int w = bitmap.getWidth();
            //int h = bitmap.getHeight();

            //이미지를 디바이스 방향으로 회전
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            bitmap =  Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);

            //bitmap을 byte array로 변환
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] currentData = stream.toByteArray();

            //파일로 저장
            new SaveImageTask().execute(currentData);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/camtest");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to "
                        + outFile.getAbsolutePath());

                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }
    }

    private class SaveVideoTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/videotest");
                dir.mkdirs();

                String fileName = String.format("%d.mp4", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onVideoTaken - wrote bytes: " + data.length + " to "
                        + outFile.getAbsolutePath());

                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }
    }

    /**
     *
     * @param activity
     * @param cameraId  Camera.CameraInfo.CAMERA_FACING_FRONT,
     *                    Camera.CameraInfo.CAMERA_FACING_BACK
     * @param camera
     *
     * Camera Orientation
     * reference by https://developer.android.com/reference/android/hardware/Camera.html
     */
    public static int setCameraDisplayOrientation(Activity activity,
                                                  int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( requestCode == PERMISSIONS_REQUEST_CODE && grandResults.length > 0) {

            int hasCameraPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA);
            int hasWriteExternalStoragePermission =
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasRecordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

            if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                    && hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED
                    && hasRecordAudioPermission == PackageManager.PERMISSION_GRANTED){

                //이미 퍼미션을 가지고 있음
                doRestart(this);
            }
            else{
                checkPermissions();
            }
        }

    }


    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        int hasCameraPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        int hasWriteExternalStoragePermission =
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int hasRecordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        boolean cameraRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA);
        boolean writeExternalStorageRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean recordAudioRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO);


        if ( (hasCameraPermission == PackageManager.PERMISSION_DENIED && cameraRationale)
                || (hasWriteExternalStoragePermission== PackageManager.PERMISSION_DENIED
                && writeExternalStorageRationale) || (hasRecordAudioPermission == PackageManager.PERMISSION_DENIED && recordAudioRationale))
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

        else if ( (hasCameraPermission == PackageManager.PERMISSION_DENIED && !cameraRationale)
                || (hasWriteExternalStoragePermission== PackageManager.PERMISSION_DENIED
                && !writeExternalStorageRationale || (hasRecordAudioPermission == PackageManager.PERMISSION_DENIED && !recordAudioRationale)))
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");

        else if ( hasCameraPermission == PackageManager.PERMISSION_GRANTED
                || hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED
                || hasRecordAudioPermission == PackageManager.PERMISSION_GRANTED) {
            doRestart(this);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //퍼미션 요청
                ActivityCompat.requestPermissions( MainActivity.this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                        PERMISSIONS_REQUEST_CODE);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    //Nexus 5X Camera 반전 해결 코드

}