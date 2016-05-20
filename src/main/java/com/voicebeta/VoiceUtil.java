package com.voicebeta;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.voicebeta.util.RecordUtil;
import com.voicebeta.util.ThreadPoolManager;
import com.voicebeta.view.BubbleLinearLayout;

import java.io.IOException;
import java.util.UUID;

public class VoiceUtil{

    public static VoiceUtil getInstance(){
        if(null == instance){
            instance = new VoiceUtil();
        }
        return instance;
    }

    /**
     * 将外部的布局view传进来
     * */
    public void initLayoutView(View view,Context context){
        m_obj_layoutView = view;
        m_obj_context = context;


        initView();

        initRunnable();

        init();
    }

    /**
     * 设置录音的按钮
     * */
    public void setRecordBtn(View view,ViewAction lister){
        m_obj_record_image_btn = view;
        m_obj_event = lister;

        setLister();
    }

    /**
     * 获取录制的音频路径
     * */
    public String getAudioPath(){
        return mRecordPath;
    }









    /****************************************  以下为私有方法和变量 ***************************************************/

//
    private View m_obj_record_image_btn;

    //    录制过程中的光圈动画
    private ImageView mRecordLight_1;
    private ImageView mRecordLight_2;
    private ImageView mRecordLight_3;

    private Animation mRecordLight_1_Animation;
    private Animation mRecordLight_2_Animation;
    private Animation mRecordLight_3_Animation;


    //录音状态
    private static final int RECORD_NO = 0; // 不在录音
    private static final int RECORD_ING = 1; // 正在录音
    private static final int RECORD_ED = 2; // 完成录音
    private int mRecord_State = 0; // 录音的状态


    private static final String PATH = Environment.getExternalStorageDirectory().getPath()+"/record/";// 录音存储路径
    private String mRecordPath;// 录音的存储名称

    private int mMAXVolume;// 最大音量高度
    private int mMINVolume;// 最小音量高度

    //
    private Runnable m_obj_down_runnable;
    private Runnable m_obj_up_runnable;

    private float mRecord_Time;// 录音的时间
    private double mRecord_Volume;// 麦克风获取的音量值

    private static final int MAX_TIME = 60;// 最长录音时间
    private static final int MIN_TIME = 2;// 最短录音时间
    private ProgressBar mVoiceProgressBar;
    private TextView mRecordTime;


    private ImageView mRecordVolume;


    private RecordUtil mRecordUtil;

    private MediaPlayer mMediaPlayer;
    //    录音动画
    private RelativeLayout m_obj_recording_anim;

    private View m_obj_layoutView;
    private Context m_obj_context;

    private ViewAction m_obj_event;


    private static VoiceUtil instance;



    private VoiceUtil(){
    }
    private void initView(){
        m_obj_recording_anim = (RelativeLayout) m_obj_layoutView.findViewById(R.id.id_recording_anim);
        //m_obj_play.setOnClickListener(this);

        mRecordLight_1 = (ImageView) m_obj_layoutView.findViewById(R.id.voice_recordinglight_1);
        mRecordLight_2 = (ImageView) m_obj_layoutView.findViewById(R.id.voice_recordinglight_2);
        mRecordLight_3 = (ImageView) m_obj_layoutView.findViewById(R.id.voice_recordinglight_3);

        mVoiceProgressBar = (ProgressBar) m_obj_layoutView.findViewById(R.id.voice_record_progressbar);
        mRecordTime = (TextView) m_obj_layoutView.findViewById(R.id.voice_record_time);
        mRecordVolume = (ImageView) m_obj_layoutView.findViewById(R.id.voice_recording_volume);
    }


    private void initRunnable(){
        m_obj_down_runnable = new Runnable() {
            @Override
            public void run() {

                mRecord_Time = 0;
                while (mRecord_State == RECORD_ING) {
                    // 大于最大录音时间则停止录音
                    if (mRecord_Time >= MAX_TIME) {
                        mRecordHandler.sendEmptyMessage(0);
                    } else {
                        try {
                            // 每隔200毫秒就获取声音音量并更新界面显示
                            Thread.sleep(200);
                            mRecord_Time += 0.2;
                            if (mRecord_State == RECORD_ING) {
                                mRecord_Volume = mRecordUtil
                                        .getAmplitude();
                                mRecordHandler
                                        .sendEmptyMessage(1);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        m_obj_up_runnable = new Runnable() {
            @Override
            public void run() {

                if (mRecord_Time <= MIN_TIME) {
                    // 显示提醒
                    Toast.makeText(m_obj_context, "录音时间过短",
                            Toast.LENGTH_SHORT).show();
                    // 修改录音状态
                    mRecord_State = RECORD_NO;
                    // 修改录音时间
                    mRecord_Time = 0;
                    // 修改显示界面
                    mRecordTime.setText("0″");
                    // 修改录音声音界面
                    ViewGroup.LayoutParams params = mRecordVolume
                            .getLayoutParams();
                    params.height = 0;
                    mRecordVolume.setLayoutParams(params);
                } else {
                    // 录音成功,则显示录音成功后的界面

                }
            }
        };
    }
    private void setLister(){
        m_obj_record_image_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
// 开始录音
                    case MotionEvent.ACTION_DOWN:
                        if (null != m_obj_event) {
                            m_obj_event.down();
                        }
                        if (mRecord_State != RECORD_ING) {
                            m_obj_recording_anim.setVisibility(View.VISIBLE);
                            mRecord_State = RECORD_ING;
                            startRecordLightAnimation();

                            mRecordPath = PATH + UUID.randomUUID().toString()
                                    + ".amr";

                            // 实例化录音工具类
                            mRecordUtil = new RecordUtil(mRecordPath);
                            try {
                                // 开始录音
                                mRecordUtil.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //开始修改话筒上的动画
                            //TODO here
                            ThreadPoolManager.getInstance().executeTask(m_obj_down_runnable);


                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (null != m_obj_event) {
                            m_obj_event.up();
                        }
                        if (mRecord_State == RECORD_ING) {
                            mRecord_State = RECORD_ED;
                            m_obj_recording_anim.setVisibility(View.GONE);

                            stopRecordLightAnimation();


                            try {
                                // 停止录音
                                mRecordUtil.stop();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ThreadPoolManager.getInstance().executeTask(m_obj_up_runnable);
                        }
                        break;
                }
                return false;
            }
        });
    }


    /**
     * 用来控制动画效果（光圈）
     */
    Handler mRecordLightHandler = new Handler() {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (mRecord_State == RECORD_ING) {
                        mRecordLight_1.setVisibility(View.VISIBLE);
                        mRecordLight_1_Animation = AnimationUtils.loadAnimation(
                                m_obj_context, R.anim.voice_anim);
                        mRecordLight_1.setAnimation(mRecordLight_1_Animation);
                        mRecordLight_1_Animation.startNow();
                    }
                    break;

                case 1:
                    if (mRecord_State == RECORD_ING) {
                        mRecordLight_2.setVisibility(View.VISIBLE);
                        mRecordLight_2_Animation = AnimationUtils.loadAnimation(
                                m_obj_context, R.anim.voice_anim);
                        mRecordLight_2.setAnimation(mRecordLight_2_Animation);
                        mRecordLight_2_Animation.startNow();
                    }
                    break;
                case 2:
                    if (mRecord_State == RECORD_ING) {
                        mRecordLight_3.setVisibility(View.VISIBLE);
                        mRecordLight_3_Animation = AnimationUtils.loadAnimation(
                                m_obj_context, R.anim.voice_anim);
                        mRecordLight_3.setAnimation(mRecordLight_3_Animation);
                        mRecordLight_3_Animation.startNow();
                    }
                    break;
                case 3:
                    if (mRecordLight_1_Animation != null) {
                        mRecordLight_1.clearAnimation();
                        mRecordLight_1_Animation.cancel();
                        mRecordLight_1.setVisibility(View.GONE);

                    }
                    if (mRecordLight_2_Animation != null) {
                        mRecordLight_2.clearAnimation();
                        mRecordLight_2_Animation.cancel();
                        mRecordLight_2.setVisibility(View.GONE);
                    }
                    if (mRecordLight_3_Animation != null) {
                        mRecordLight_3.clearAnimation();
                        mRecordLight_3_Animation.cancel();
                        mRecordLight_3.setVisibility(View.GONE);
                    }

                    break;
            }
        }
    };

    /**
     * 录音话筒上的动画
     * */
    Handler mRecordHandler = new Handler() {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (mRecord_State == RECORD_ING) {
                        // 停止动画效果
                        stopRecordLightAnimation();
                        // 修改录音状态
                        mRecord_State = RECORD_ED;
                        try {
                            // 停止录音
                            mRecordUtil.stop();
                            // 初始化录音音量
                            mRecord_Volume = 0;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // 根据录音修改界面显示内容

                    }
                    break;

                case 1:
                    // 根据录音时间显示进度条
                    mVoiceProgressBar.setProgress((int) mRecord_Time);
                    // 显示录音时间
                    mRecordTime.setText((int) mRecord_Time + "″");
                    // 根据录音声音大小显示效果
                    ViewGroup.LayoutParams params = mRecordVolume.getLayoutParams();
                    Log.i("test","mRecord_Volume = " + mRecord_Volume);
                    if (mRecord_Volume < 200.0) {
                        params.height = mMINVolume;
                    } else if (mRecord_Volume > 200.0 && mRecord_Volume < 400) {
                        params.height = mMINVolume * 2;
                    } else if (mRecord_Volume > 400.0 && mRecord_Volume < 800) {
                        params.height = mMINVolume * 3;
                    } else if (mRecord_Volume > 800.0 && mRecord_Volume < 1600) {
                        params.height = mMINVolume * 4;
                    } else if (mRecord_Volume > 1600.0 && mRecord_Volume < 3200) {
                        params.height = mMINVolume * 5;
                    } else if (mRecord_Volume > 3200.0 && mRecord_Volume < 5000) {
                        params.height = mMINVolume * 6;
                    } else if (mRecord_Volume > 5000.0 && mRecord_Volume < 7000) {
                        params.height = mMINVolume * 7;
                    } else if (mRecord_Volume > 7000.0 && mRecord_Volume < 10000.0) {
                        params.height = mMINVolume * 8;
                    } else if (mRecord_Volume > 10000.0 && mRecord_Volume < 14000.0) {
                        params.height = mMINVolume * 9;
                    } else if (mRecord_Volume > 14000.0 && mRecord_Volume < 17000.0) {
                        params.height = mMINVolume * 10;
                    } else if (mRecord_Volume > 17000.0 && mRecord_Volume < 20000.0) {
                        params.height = mMINVolume * 11;
                    } else if (mRecord_Volume > 20000.0 && mRecord_Volume < 24000.0) {
                        params.height = mMINVolume * 12;
                    } else if (mRecord_Volume > 24000.0 && mRecord_Volume < 28000.0) {
                        params.height = mMINVolume * 13;
                    } else if (mRecord_Volume > 28000.0) {
                        params.height = mMAXVolume;
                    }
                    mRecordVolume.setLayoutParams(params);
                    break;
            }
        }

    };


    /**
     * 开始动画效果
     */
    private void startRecordLightAnimation() {
        mRecordLightHandler.sendEmptyMessageDelayed(0, 0);
        mRecordLightHandler.sendEmptyMessageDelayed(1, 1000);
        mRecordLightHandler.sendEmptyMessageDelayed(2, 2000);
    }

    /**
     * 停止动画效果
     */
    private void stopRecordLightAnimation() {
        mRecordLightHandler.sendEmptyMessage(3);
    }

//    播放音频
    private void playAudio(){
        mMediaPlayer = new MediaPlayer();

            // 添加录音的路径
        try {
            mMediaPlayer.setDataSource(mRecordPath);
            // 准备
            mMediaPlayer.prepare();
            // 播放
            mMediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void init() {
        // 设置当前的最小声音和最大声音值
        mMINVolume = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4.5f, m_obj_context.getResources()
                        .getDisplayMetrics());
        mMAXVolume = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 65f, m_obj_context.getResources()
                        .getDisplayMetrics());
    }


    /**
     * View down和up的事件
     * */
    public interface ViewAction{
        void down();
        void up();
    }


}
