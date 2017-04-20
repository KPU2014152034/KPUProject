package com.example.limhj.fuesed1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class StatsActivity extends AppCompatActivity {

    TextView stats_time, stats_distance, stats_Speed;
    double Speed, Distance;
    String strDistance;
    int time_sec;
    Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        intent=getIntent();

        Speed = intent.getExtras().getDouble("SPEED",0.0);
        Distance = intent.getExtras().getDouble("DISTANCE",0.0);
        time_sec = intent.getExtras().getInt("TIME",0);

        strDistance=String.format("%.1f",Distance);

        stats_time = (TextView) findViewById(R.id.stats_recent_time);
        stats_distance = (TextView) findViewById(R.id.stats_recent_distance);
        stats_Speed = (TextView) findViewById(R.id.stats_recent_avg_speed);
        stats_Speed.setText(String.valueOf(Speed));
        stats_time.setText(String.valueOf(time_sec));
        stats_distance.setText(strDistance+" m");

    }

}
