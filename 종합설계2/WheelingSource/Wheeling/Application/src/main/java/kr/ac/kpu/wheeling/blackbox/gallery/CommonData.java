package kr.ac.kpu.wheeling.blackbox.gallery;

/**
 * Created by JSH on 2017-05-11.
 */

public class CommonData {
    public long mVideoId;
    public String mVideoFilePath;
    public String mVideoTitle;
    public long mVideoSize;
    public long mVideoDuration;
    public String mVideoAddDate;
    public int mVideoWidth;
    public int mVideoHeight;

    public CommonData(){
        mVideoWidth = 0;
        mVideoHeight = 0;
        mVideoId = 0;
        mVideoSize = 0;
        mVideoFilePath = null;
        mVideoTitle = null;
        mVideoDuration = 0;
        mVideoAddDate = null;
    }
}
