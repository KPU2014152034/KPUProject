package kr.ac.kpu.wheeling.blackbox;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by JSH on 2017-05-18.
 */

public class ScreenInfo {
    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;

    public ScreenInfo(){
        SCREEN_WIDTH = 0;
        SCREEN_HEIGHT = 0;
    }
    /**
     * 소프트키를 포함한 화면 전체해상도를 가져온다.
     * @param context
     */
    public static void setScreenInfo(Context context){
        if(Build.VERSION.SDK_INT>=14)
        {
            Display display = ((WindowManager) context.getSystemService(context.WINDOW_SERVICE)).getDefaultDisplay();
            Point realSize = new Point();
            try {
                Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            SCREEN_WIDTH=realSize.x;
            SCREEN_HEIGHT=realSize.y;
        }else{
            DisplayMetrics dmath=context.getResources().getDisplayMetrics();	// 화면의 가로,세로 길이를 구할 때 사용합니다.
            SCREEN_WIDTH=dmath.widthPixels;
            SCREEN_HEIGHT=dmath.heightPixels;
        }
    }


    /**
     * 소프트키를 미포함한 화면 전체해상도를 가져온다.
     * @param context
     */
    public static void setNoSoftKeyScreenInfo(Context context){
        DisplayMetrics dmath=context.getResources().getDisplayMetrics();	// 화면의 가로,세로 길이를 구할 때 사용합니다.
        SCREEN_WIDTH=dmath.widthPixels;
        SCREEN_HEIGHT=dmath.heightPixels;
    }


    /**
     * 소프트키 존재 여부를 가져온다.
     * @param context
     */
    public static boolean isScreenSoftKey(Context context) {
        boolean isKey = false;
        if (Build.VERSION.SDK_INT >= 14) {
            boolean hasMenuKey = ViewConfiguration.get(context)
                    .hasPermanentMenuKey();
            boolean hasBackKey = KeyCharacterMap
                    .deviceHasKey(KeyEvent.KEYCODE_BACK);

            if (!hasMenuKey && !hasBackKey) {
                isKey = true;
            } else {
                isKey = false;
            }
        } else {
            isKey = false;
        }
        return isKey;
    }

    @Override
    public String toString(){
        return "[SCREEN_WIDTH: " + SCREEN_WIDTH + ", SCREEN_HEIGHT: " + SCREEN_HEIGHT + "]";
    }
    //출처: http://tlshenm.tistory.com/21 [No Job Of Star]
}
