package kr.ac.kpu.wheeling.view;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import kr.ac.kpu.wheeling.R;

public class ViewMainActivity extends AppCompatActivity implements TrackView.OnDataSend {
    FrameLayout frameLayout;
    int time=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_main);
        frameLayout=(FrameLayout)findViewById(R.id.container_view);

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container_view,ViewActivity.newInstance(time))
                    .commit();
        }

    }

    @Override
    public void onTimeSend(int time) {
        getFragmentManager().beginTransaction()
                .replace(R.id.container_view,ViewActivity.newInstance(time))
                .commit();
    }
}
