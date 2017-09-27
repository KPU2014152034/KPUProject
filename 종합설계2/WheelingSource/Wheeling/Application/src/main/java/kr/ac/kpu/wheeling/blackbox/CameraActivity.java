/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.ac.kpu.wheeling.blackbox;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import kr.ac.kpu.wheeling.R;
import kr.ac.kpu.wheeling.tracker.TrackerFragment;

public class CameraActivity extends AppCompatActivity implements TrackerFragment.CustomOnClickListener {
    Button button;
    FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameLayout=(FrameLayout)findViewById(R.id.container);
        // button=(Button)findViewById(R.id.button2);
        /*if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(kr.ac.kpu.wheeling.R.id.contents, TrackerFragment.newInstance())
                    .commit();
        }*/
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container,Camera2VideoFragment.newInstance())
                    .commit();
        }

    }


    @Override
    public void onClicked(View v) {
        Camera2VideoFragment camera2VideoFragment=(Camera2VideoFragment)getFragmentManager().findFragmentById(R.id.container);
        switch (v.getId())
        {
            case R.id.btn_start:
                camera2VideoFragment.startRecordingVideo();
                break;
            case R.id.btn_end:
                camera2VideoFragment.stopRecordingVideo();
                break;
        }
    }
/*
    public void switchFragment() {
        Fragment fr;

        if (isFragmentB) {
            fr = new TrackerFragment();
        } else {
            fr = new Camera2VideoFragment() ;
        }

        isFragmentB = (isFragmentB) ? false : true ;

        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.container, fr);
        fragmentTransaction.commit();
    }
*/

}
