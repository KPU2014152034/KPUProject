package kr.ac.kpu.wheeling.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import kr.ac.kpu.wheeling.R;
import kr.ac.kpu.wheeling.blackbox.Camera2VideoFragment;
import kr.ac.kpu.wheeling.helper.SQLiteHandler;
import kr.ac.kpu.wheeling.object.TrackObject;
import kr.ac.kpu.wheeling.view.TrackView;
import kr.ac.kpu.wheeling.view.ViewActivity;


import static android.widget.Toast.makeText;
import static com.google.android.gms.plus.PlusOneDummyView.TAG;
import static kr.ac.kpu.wheeling.R.id.time;

/**
 * Created by limhj_000 on 2017-05-11.
 */

public class TrackerFragment extends Fragment implements View.OnClickListener,OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private MapView mapView;
    private GoogleMap mMap;
    private int BID =0;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private FusedLocationProviderApi locationProvider= LocationServices.FusedLocationApi;

    String mJsonString;
    private static final String TAG_JSON="webnautes";
    private static final String TAG_ID = "bid";


    private SQLiteHandler sqLiteHandler;
    Chronometer chronometer;
    private ArrayList<TrackObject> TOlist;
    private ArrayList<Location> mlocation_list;

    TextView lon_latView, addrView,speedView,distanceView,avrspeedView,altView,maxspeedView;
    Button btn_start,btn_end,btn_temp,btn_test,btn_view;

    TrackObject trackObject;

    private static final int MY_LOCATION_REQUEST_CODE = 1;

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS=2;

    private boolean permissionIsGranted=false; // 안드로이드 6.0 이상 퍼미션


    double longitude;
    double latitude;
    double altitude;
    double accuracy;
    float bearing,fix_bearing;
    private int time_sec;
    private Location mlocation;
    CameraUpdate zoom;
    Geocoder geocoder;
    List<Address> list;
    String addr = null;
    String strdistance="0.0",str_Speed="0.0",str_MaxSpeed="0.0",str_avrSpeed="0.0";

    double mySpeed=0.0, maxSpeed=0.0,AvrSpeed=0.0;

    Location locationA,locationB;
    double startlat,startlon,alldistance,distance=0.0;
    //double distance;

    PolylineOptions rectOptions;
    boolean DrawWalk=false;
    boolean isGPSOn=false;



    boolean sensorChk=false;

    public interface CustomOnClickListener{
        public void onClicked(View v);
    }

    private CustomOnClickListener customOnClickListener;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mlocation_list = new ArrayList<Location>();
        sqLiteHandler = new SQLiteHandler(getActivity());
        TOlist= new ArrayList<TrackObject>();
        trackObject=new TrackObject();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracker, null);
        mapView=(MapView)view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);
       // mlocation_list = new ArrayList<Location>();
        BID = sqLiteHandler.getblackbox_bid()+1;


        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000*3);
        locationRequest.setFastestInterval(1000* 1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);



        geocoder = new Geocoder(getActivity());
        list = null;
        locationA = new Location("pointA");
        locationB = new Location("pointB");
        rectOptions = new PolylineOptions().width(10).color(Color.RED);


        distanceView = (TextView)view. findViewById(R.id.distanceView);
        speedView = (TextView)view. findViewById(R.id.speedView);
        addrView = (TextView)view. findViewById(R.id.addrView);
        lon_latView = (TextView) view.findViewById(R.id.lon_latView);
        btn_start = (Button)view. findViewById(R.id.btn_start);
        btn_end = (Button) view.findViewById(R.id.btn_end);
        btn_test=(Button)view.findViewById(R.id.btn_test);
        btn_temp=(Button)view.findViewById(R.id.btn_temp);
        btn_view=(Button)view.findViewById(R.id.btn_view);
        avrspeedView = (TextView) view.findViewById(R.id.avrspeedView);
        altView =(TextView)view.findViewById(R.id.altView);
        chronometer = (Chronometer)view.findViewById(R.id.chronometer_tracker);
        maxspeedView=(TextView)view.findViewById(R.id.maxSpeedView);

        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long time=(SystemClock.elapsedRealtime() - chronometer.getBase());

                trackObject.setMtime(time);
                time_sec = (int) (((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000)); // 1
                Log.d(TAG ," time_sec "+time_sec);
                chronometer.setFormat("00:00:00");
                trackObject.setTime(time_sec);
                Date date= new Date(time);
                DateFormat formatter= new SimpleDateFormat("HH:mm:ss");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                String dateformat= formatter.format(date);
                chronometer.setText(dateformat);
                AvrSpeed = ((alldistance) / (time_sec)) * 3.6;
                str_avrSpeed = String.format("%.1f", ((alldistance) / (time_sec)) * 3.6);
                if(str_avrSpeed.equals("NaN"))
                {
                    str_avrSpeed="0.0";
                }
                vieww();
                TOlist.add(trackObject);
            }
        });
        btn_start.setOnClickListener(this);
        btn_end.setOnClickListener(this);
        btn_test.setOnClickListener(this);
        btn_view.setOnClickListener(this);
        btn_temp.setOnClickListener(this);
        init();
        vieww();


        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:{
                DrawWalk = true;
                Toast.makeText(getActivity(),""+BID , Toast.LENGTH_SHORT).show();
                init();
                chronometer.start();
                mlocation_list = new ArrayList<Location>();
                startlat = latitude;
                startlon = longitude;

                Log.d("TrackerFragment" , "useremail  :"+sqLiteHandler.getUserDetails().get("email") + " bid() :"+sqLiteHandler.getblackbox_bid());

                customOnClickListener.onClicked(view);
                btn_start.setVisibility(View.GONE);
                btn_end.setVisibility(View.VISIBLE);
                break;
            }
            case R.id.btn_end: {
                DrawWalk = false;
                chronometer.stop();
                customOnClickListener.onClicked(view);
                BID = sqLiteHandler.getblackbox_bid()+1;
                //sqLiteHandler.addLocation_new(TOlist);  //SQL Lite inset
                //ArrayList<TrackObject> maplocation =sqLiteHandler.getlocationALL(); // SQL Lite select

                //InsertData task = new InsertData(); // MYSQL
                //task.execute(TOlist); // MYSQL
                sqLiteHandler.addLocation_new(TOlist);
                rectOptions = new PolylineOptions().width(10).color(Color.RED);
                btn_start.setVisibility(View.VISIBLE);
                btn_end.setVisibility(View.GONE);
                break;
            }
            case R.id.btn_test:
            {

               // sqLiteHandler.addLocation_new(TOlist);  //SQL Lite inset

                //Log.d(TAG , "useremail  :"+sqLiteHandler.getUserDetails().get("email") + " bid() :"+sqLiteHandler.getblackbox_bid());

                ArrayList<TrackObject> maplocation =sqLiteHandler.getlocationALL(); // SQL Lite select
                //InsertData task = new InsertData(); // MYSQL
                // task.execute(TOlist); // MYSQL
                break;
            }
            case R.id.btn_temp:
            {
                //InsertData task = new InsertData(); // MYSQL
                // task.execute(TOlist); // MYSQL
               // GetData task = new GetData(); // MYSQL GET BiD
               // task.execute("http://wheeling.tk/track_select.php");// MYSQL Get Bid
                ArrayList<TrackObject> maplocation =sqLiteHandler.getlocationALL();
                sqLiteHandler.getblackbox();
                break;
            }

            case R.id.btn_view:
            {
                //  sqLiteHandler.addLocation_new(TOlist);  //SQL Lite inset
                //ArrayList<TrackObject> maplocation =sqLiteHandler.getlocationALL(); // SQL Lite select
                //InsertData task = new InsertData(); // MYSQL
                // task.execute(TOlist); // MYSQL
                //sqLiteHandler.getblackbox();
                break;
            }
        }
    }

    public void buttonClicked(View v){
        customOnClickListener.onClicked(v);
    }

    @Override
    @Deprecated
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        customOnClickListener=(CustomOnClickListener) activity;
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return;
        }
        //mMap.setMyLocationEnabled(true);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        longitude = location.getLongitude(); //경도
        latitude = location.getLatitude();   //위도
        altitude = location.getAltitude();   //고도
        accuracy=location.getAccuracy(); //정확도
        bearing=location.getBearing();

        if (location != null) {
            mySpeed = location.getSpeed();
            if (mySpeed > maxSpeed) {
                maxSpeed = mySpeed;

            }
            str_MaxSpeed=String.format("%.1f",maxSpeed*3.6);
            str_Speed=String.format("%.1f",mySpeed*3.6);
        }
        addrView.setText("주소: " + getAddress(getActivity(),latitude,longitude));
        String addre=getAddress(getActivity(),latitude,longitude);
       //addr=getAddress(getActivity(),latitude,longitude);


        trackObject =new TrackObject();
        trackObject.setBid(BID);
        trackObject.setLat(location.getLatitude());
        trackObject.setLon(location.getLongitude());
        trackObject.setAlt(location.getAltitude());
        trackObject.setSpeed(location.getSpeed());
        trackObject.setDistance(getDistance());
        trackObject.setAvrspeed(AvrSpeed);
        trackObject.setMaxspeed(maxSpeed);
        trackObject.setAddress(addre);


        if(DrawWalk) {
           // strdistance=String.format("%.1f",distance.getDistance(latitude,longitude));
            strdistance=String.format("%.1f",getDistance());
           // TOlist.add(trackObject);
            //mlocation_list.add(mlocation);
           // Log.d(TAG, "mlocation_list  - " + mlocation_list.get(0).getLatitude() +" lon  :" + + mlocation_list.get(0).getLongitude() +"alt : "+ + mlocation_list.get(0).getAltitude());

        }


        if(DrawWalk && mySpeed > 0.9) {
            Log.d("polyline","polyline"+rectOptions);
            rectOptions.add(new LatLng(latitude, longitude));
            mMap.addPolyline(rectOptions);
        }


        if(!sensorChk){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
        }

        if(sensorChk && mySpeed > 0.9) {
            fix_bearing=bearing;
            CameraPosition currentPlace = new CameraPosition.Builder()
                    .target(new LatLng(latitude, longitude))
                    .bearing(bearing).zoom(18f).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        } else if (sensorChk && mySpeed <= 0.9) {
            CameraPosition currentPlace = new CameraPosition.Builder()
                    .target(new LatLng(latitude, longitude))
                    .bearing(fix_bearing).zoom(18f).build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        }

        zoom = CameraUpdateFactory.zoomTo(18);// 구글지도(지구) 에서의 zoom 레벨은 1~23 까지 가능합니다.
        mMap.animateCamera(zoom);
    }
    public static String getAddress(Context mContext, double lat, double lng) {
        String nowAddress ="현재 위치를 확인 할 수 없습니다.";
        Geocoder geocoder = new Geocoder(mContext, Locale.KOREA);
        List <Address> address;
        try {
            if (geocoder != null) {
                //세번째 파라미터는 좌표에 대해 주소를 리턴 받는 갯수로
                //한좌표에 대해 두개이상의 이름이 존재할수있기에 주소배열을 리턴받기 위해 최대갯수 설정
                address = geocoder.getFromLocation(lat, lng, 1);

                if (address != null && address.size() > 0) {
                    // 주소 받아오기
                    String currentLocationAddress = address.get(0).getAddressLine(0).toString();
                    nowAddress  = currentLocationAddress;

                }
            }

        } catch (IOException e) {


            e.printStackTrace();
        }
        return nowAddress;
    }


    public void input_bid(ArrayList<TrackObject> list,int bid){
        for(int i=0;i<list.size();i++){
            list.get(i).setBid(bid);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
        googleApiClient.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if(googleApiClient.isConnected()) {
            //if (!isGPSOn) {
            //  isGPSOn = chkGpsService();
            //}
            requestLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        //LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
    }

    public static TrackerFragment newInstance() {
        return new TrackerFragment();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
            mMap=googleMap;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        else
        {
            // Show rationale and request permission.
        }
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setTrafficEnabled(true);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Log.d("onPermissions","RequestPermissions OK :" +permissionIsGranted);
                    mMap.setMyLocationEnabled(true);
                    permissionIsGranted=true;

                }

            } else {
                mMap.setMyLocationEnabled(false);
                permissionIsGranted=false;
                Log.d("onPermissions","RequestPermission Off :" +permissionIsGranted);
                // Permission was denied. Display an error message.
            }
        }
    }

    private boolean chkGpsService() { //GSP On//Off 확인 함수
        String gps = android.provider.Settings.Secure.getString(getActivity().getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        Log.d(gps, "GPS On Off Chk");
        if (!(gps.matches(".*gps.*") && gps.matches(".*network.*"))) {
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(getActivity());
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("무선 네트워크 사용, GPS 위성 사용을 모두 체크하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    }).create().show();
            return false;
        } else {
            return true;
        }
    }

    private void checkUserPermission(){
        if(ActivityCompat.checkSelfPermission(getActivity(),Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
            else
            {
                ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }else{
            permissionIsGranted=true;
        }

    }

    public void vieww(){
        lon_latView.setText("\n위도 : " + longitude + "\n경도 : " + latitude
                + "\n고도 : " + altitude+"\n정확도 : "+accuracy);

        speedView.setText("현재 속도 : " + (str_Speed) + " km/h");

        distanceView.setText("이동거리 : "+strdistance+" m");
        avrspeedView.setText("평균속도 : "+str_avrSpeed+" Km/h");
        altView.setText("고도 : " +altitude);
        maxspeedView.setText("최고속도 : "+ str_MaxSpeed +" Km/h");
    }

    public void init() {
        if(!isGPSOn) {
            isGPSOn=chkGpsService();
        }
        checkUserPermission();

        time_sec = 0;
        alldistance = 0;
        maxSpeed = mySpeed = 0;
        chronometer.setBase(SystemClock.elapsedRealtime());
    }

    public double getDistance(){
        locationA.setLatitude(startlat);
        locationA.setLongitude(startlon);
        locationB.setLatitude(latitude);
        locationB.setLongitude(longitude);
        startlon=longitude;
        startlat=latitude;
        distance=locationA.distanceTo(locationB);
        alldistance+=distance;
        return alldistance;
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
            for (int cnt = 0; cnt < params[0].size(); cnt++) {
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
                URL url = new URL("http://wheeling.tk/track_insert.php");
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
                BID=Integer.valueOf(bid);
                Log.d("showResult" ,"bid   : "+   bid);
            }
            input_bid(TOlist,BID);
            Log.d(TAG, "TOlist-toString()  - " + TOlist.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "showResult : ", e);
        }

    }

}
