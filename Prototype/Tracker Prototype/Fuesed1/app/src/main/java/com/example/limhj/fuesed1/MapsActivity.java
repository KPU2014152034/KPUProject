package com.example.limhj.fuesed1;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.example.limhj.fuesed1.db.dbHelper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.List;

import static android.widget.Toast.makeText;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,LocationListener {
    TextView lon_latView, addrView,speedView,distanceView,Time;
    Button btn_start,btn_end,btn_stats;

    private dbHelper dbHelper;

    private static final int MY_LOCATION_REQUEST_CODE = 1;

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS=2;

    private boolean permissionIsGranted=false; // 안드로이드 6.0 이상 퍼미션

    private GoogleMap mMap;
    double longitude;
    double latitude;
    double altitude;
    double accuracy;
    float bearing,fix_bearing;
    private int time_sec;
    private FusedLocationProviderApi locationProvider= LocationServices.FusedLocationApi;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;


    Chronometer chronometer;

    MarkerOptions marker;
    CameraUpdate zoom;
    Geocoder geocoder;
    List<Address> list;
    String addr = null;
    String strdistance="0.0",Speed="0.0",MaxSpeed="0.0",avrSpeed="0.0";

    double mySpeed, maxSpeed;

    Location locationA,locationB;
    double startlat,startlon;
    double distance,alldistance;

    PolylineOptions rectOptions;
    boolean DrawWalk=false;
    boolean isGPSOn=false;



    boolean sensorChk=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dbHelper=new dbHelper(this);


        marker = new MarkerOptions();
        geocoder = new Geocoder(this);
        list = null;
        locationA = new Location("pointA");
        locationB = new Location("pointB");
        rectOptions = new PolylineOptions().width(10).color(Color.RED);

        distanceView = (TextView) findViewById(R.id.textView5);
        speedView = (TextView) findViewById(R.id.textView4);
        addrView = (TextView) findViewById(R.id.textView3);
        lon_latView = (TextView) findViewById(R.id.textView2);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_end = (Button) findViewById(R.id.btn_end);
        Time = (TextView) findViewById(R.id.textView6);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        btn_stats=(Button)findViewById(R.id.btn_stats);

        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                time_sec = (int) (((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000));
                avrSpeed = String.format("%.1f", ((alldistance) / (time_sec)) * 3.6);
                if(avrSpeed.equals("NaN"))
                {
                    avrSpeed="0.0";
                }
                view();

            }
        });
        btn_start.setOnClickListener(new View.OnClickListener() { // 시작버튼
            @Override
            public void onClick(View v) {
                DrawWalk = true;

                init();
                chronometer.start();
                startlat = latitude;
                startlon = longitude;
                makeText(MapsActivity.this, "걸음시작 위도: " + startlat + "경도: " + startlon, Toast.LENGTH_SHORT).show();
                btn_start.setVisibility(View.GONE);
                btn_end.setVisibility(View.VISIBLE);
            }
        });
        btn_end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawWalk = false;

                chronometer.stop();
                makeText(MapsActivity.this, "걸음종료", Toast.LENGTH_SHORT).show();
                btn_start.setVisibility(View.VISIBLE);
                btn_end.setVisibility(View.GONE);
            }
        });

        btn_stats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MapsActivity.this, StatsActivity.class);
                intent.putExtra("SPEED",mySpeed);
                intent.putExtra("TIME",time_sec);
                intent.putExtra("DISTANCE",alldistance);
                startActivity(intent);
            }
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000*2); // 위치 업데이트 주기
        locationRequest.setFastestInterval(1000*1); // 위치를 더 빨리 얻을 수있는 경우 (즉, 다른 앱이 위치 서비스를 사용하고있는 경우)를 의미합니다.
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        init();
        view();

    }


    private boolean chkGpsService() { //GSP On//Off 확인 함수

        String gps = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        Log.d(gps, "GPS On Off Chk");

        if (!(gps.matches(".*gps.*") && gps.matches(".*network.*"))) {

            // GPS OFF 일때 Dialog 표시
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("무선 네트워크 사용, GPS 위성 사용을 모두 체크하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // GPS설정 화면으로 이동
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MapsActivity.this, "GPS(위치)를 설정 하셔야 합니다.!!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }).create().show();
            return false;

        } else {
            return true;
        }
    }

    private void checkUserPermission(){

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
            else
            {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }else{
            permissionIsGranted=true;
        }

    }

    public void view(){
        lon_latView.setText("\n위도 : " + longitude + "\n경도 : " + latitude
                + "\n고도 : " + altitude+"\n정확도 : "+accuracy);

        speedView.setText("\n현재 속도 : " + (Speed) + " km/h, 최고 속도 : "
                + (MaxSpeed) + " km/h");

        distanceView.setText("이동거리: "+strdistance+" m");
        Time.setText("평균속도: "+avrSpeed+" Km/h");
    }

    public void init() {
        if(!isGPSOn) {
            isGPSOn=chkGpsService();
        }
        checkUserPermission();
        //checkLocationPermission();
        time_sec = 0;
        alldistance = 0;
        maxSpeed = mySpeed = 0;
        chronometer.setBase(SystemClock.elapsedRealtime());
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return;
        }
        mMap.setMyLocationEnabled(true);
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

        Log.d("test", "onLocationChanged, location:" + location);
        longitude = location.getLongitude(); //경도
        latitude = location.getLatitude();   //위도
        altitude = location.getAltitude();   //고도
        accuracy=location.getAccuracy(); //정확도
        bearing=location.getBearing();
       // Log.d("Location","bearing : "+bearing+" long : "+longitude+" lat : "+latitude);
        if (location != null) {

            mySpeed = location.getSpeed();
            if (mySpeed > maxSpeed) {
                maxSpeed = mySpeed;
                MaxSpeed=String.format("%.1f",mySpeed*3.6);
            }
            Speed=String.format("%.1f",mySpeed*3.6);
        }


        try {// 위도 경도 - > 주소변환
            list = geocoder.getFromLocation(
                    latitude, // 위도
                    longitude, // 경도
                    10); // 얻어올 값의 개수
            Address adr = list.get(0);
            addr = adr.getAdminArea() + " " + adr.getLocality() + " " + adr.getThoroughfare() + " " + adr.getFeatureName();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
        }
        if (list != null) {
            if (list.size() == 0) {
                addrView.setText("해당되는 주소 정보는 없습니다");
            } else {
                addrView.setText("주소: " + addr);
                //  text.setText(list.get(0).toString());
            }
        }

        if(DrawWalk) {
            strdistance=String.format("%.1f",getDistance());
        }

//
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);// 현재위치 표시 아이콘

            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener(){
                @Override
                public boolean onMyLocationButtonClick(){ // 아이콘 클릭
                   if(sensorChk) {
                        sensorChk=false;
                    }
                    else{
                        sensorChk=true;
                    }
                    makeText(MapsActivity.this, ""+sensorChk, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        } else {
            // Show rationale and request permission.
        }
        // Add a marker in Sydney and move the camera
      //  Toast.makeText(this, "lat: "+String.valueOf(lat) +" long: "+String.valueOf(log), Toast.LENGTH_SHORT).show();
      //  LatLng sydney = new LatLng(latitude,longitude);
       // mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setRotateGesturesEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.setTrafficEnabled(true);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_LOCATION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("onStart", "Start : " +permissionIsGranted);
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onResume", "Resume : " +permissionIsGranted);

       // if(permissionIsGranted){
            if(googleApiClient.isConnected())
            {
                if(!isGPSOn) {
                    isGPSOn=chkGpsService();
                }
                requestLocationUpdates();

      //      }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("onPause", "Pause : " +permissionIsGranted);
        if(permissionIsGranted)
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("onStop", "Stop : " +permissionIsGranted);
        if(permissionIsGranted)
            googleApiClient.disconnect();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("onRestart", "Restart : " +permissionIsGranted);
    }

}
