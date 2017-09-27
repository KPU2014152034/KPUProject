package kr.ac.kpu.wheeling.view;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import kr.ac.kpu.wheeling.R;
import kr.ac.kpu.wheeling.helper.SQLiteHandler;
import kr.ac.kpu.wheeling.object.TrackObject;

public class TrackView extends Fragment implements OnMapReadyCallback, View.OnClickListener {
    private static final String tag = "TrackView";
    private GoogleMap mMap;
    private MapView mapView;

    private PolylineOptions polylineOptions;
    private SQLiteHandler sqLiteHandler;
    private ArrayList<TrackObject> list, listbid;
    private Button btn_test1;
    private int time_sec = 0;
    private int listsize = 0;
    Chronometer chronometer;
    TextView addrView, speedView, distanceView, avrTime, alt_view, maxspeedView, timeView;
    String filePath;
    int fileBid;
    long time=0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list = new ArrayList<TrackObject>();
        listbid = new ArrayList<TrackObject>();
        Intent intent=getActivity().getIntent();
        filePath=intent.getStringExtra("FilePath");
        fileBid=intent.getIntExtra("FileBid",0);

    }

    public interface OnDataSend{
        public void onTimeSend(int time);
    }

    public void onTimsSend(int time){
            dataSend.onTimeSend(time);
    }

    private OnDataSend dataSend;

    public static TrackView newInstance(Long time) {
        TrackView fragment=new TrackView();
        Bundle args=new Bundle(1);
        args.putLong("LTime",time);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_track_view, null);
        distanceView = (TextView) view.findViewById(R.id.distanceView_view);
        mapView = (MapView) view.findViewById(R.id.mapView2);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);
        speedView = (TextView) view.findViewById(R.id.speedView_view);
        addrView = (TextView) view.findViewById(R.id.addrView_view);
        avrTime = (TextView) view.findViewById(R.id.avrspeedView_view);
        alt_view = (TextView) view.findViewById(R.id.altView_view);
        maxspeedView = (TextView) view.findViewById(R.id.maxSpeedView_view);
        chronometer = (Chronometer) view.findViewById(R.id.chronometer_tracker_view);
        timeView = (TextView) view.findViewById(R.id.timeView_view);
        btn_test1 = (Button) view.findViewById(R.id.btn_test1);
        sqLiteHandler = new SQLiteHandler(getActivity());
        polylineOptions = new PolylineOptions().width(10).color(Color.RED);
        list = sqLiteHandler.getTracking_info(fileBid);

        btn_test1.setOnClickListener(this);



        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                time = (SystemClock.elapsedRealtime() - chronometer.getBase());
                time_sec = (int) (((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000)); // 1
               // Toast.makeText(getActivity(),""+time , Toast.LENGTH_SHORT).show();
                Date date = new Date(time);
                DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                String dateformat = formatter.format(date);
                timeView.setText(dateformat);
                listbid = sqLiteHandler.getTracking_info(fileBid);
                listsize=listbid.size()-1;
               /* polylineOptions.add(new LatLng(listbid.get(time_sec).getLat(), listbid.get(time_sec).getLon()));
                mMap.addPolyline(polylineOptions);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(listbid.get(time_sec).getLat(), listbid.get(time_sec).getLon())));*/
                addPolyLine();
                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                view(time_sec);
                if (time_sec >= listsize) {
                    chronometer.stop();
                }

            }
        });
        viewww();
        return view;
    }

    public void addPolyLine(){
        polylineOptions.add(new LatLng(listbid.get(time_sec).getLat(),listbid.get(time_sec).getLon()));
        mMap.addPolyline(polylineOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(listbid.get(time_sec).getLat(),listbid.get(time_sec).getLon())));

    }

    @Override
    public void onClick(View v) {
        TrackObject trackObject = new TrackObject();
        listsize = list.size() - 1;
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        mMap.addPolyline(polylineOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(list.get(0).getLat(), list.get(0).getLon())));
    }


    public void viewww() {

        timeView.setText("00:00:00");
        maxspeedView.setText("최고 속도 : 0.0  km/h ");
        speedView.setText("현재속도 : 0.0  km/h ");
        distanceView.setText("이동거리: 0.0  m ");
        avrTime.setText("평균속도: 0.0  Km/h ");
        addrView.setText(" 주소 : ");
        alt_view.setText(" 고도 : 0.0");
       chronometer.start();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        dataSend=(OnDataSend)activity;
    }

    public void view(int cnt) {
        //chronometer.setText("00");
        maxspeedView.setText(" 최고 속도 : " + String.format("%.1f", (list.get(cnt).getMaxspeed())) + " km/h");
        speedView.setText(" 현재속도 : " + String.format("%.1f", (list.get(cnt).getSpeed())) + " km/h");
        distanceView.setText(" 이동거리: " + String.format("%.1f", (list.get(cnt).getDistance())) + " m");
        avrTime.setText(" 평균속도 : " + String.format("%.1f", (list.get(cnt).getAvrspeed())) + " Km/h");
        addrView.setText(" 주소 :" + list.get(cnt).getAddress());
        alt_view.setText(" 고도 : " + list.get(cnt).getAlt());
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
    public void onStart() {
        super.onStart();
        mapView.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }
}
