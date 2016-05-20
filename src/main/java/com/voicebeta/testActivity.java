package com.voicebeta;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.voicebeta.util.MediaManager;
import com.voicebeta.view.BubbleLinearLayout;

import java.io.IOException;
import java.util.Map;

/**
 * Created by weizhi on 2016/5/20.
 */
public class testActivity extends Activity implements View.OnClickListener{

    private ImageButton m_obj_record_image_btn;
    private View m_obj_View;

    private MediaPlayer mMediaPlayer;

    LayoutInflater layoutInflater = null;


//    点击录制按钮
    private FrameLayout m_obj_record_btn;


    //    点击播放
    private BubbleLinearLayout m_obj_play;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutInflater = LayoutInflater.from(this);
        m_obj_View = layoutInflater.inflate(R.layout.activity_main, null);
        setContentView(m_obj_View);



        m_obj_record_image_btn = (ImageButton) findViewById(R.id.id_id_record_btn_press_btn);
        m_obj_record_btn = (FrameLayout) findViewById(R.id.id_record_btn_press);
        m_obj_play = (BubbleLinearLayout) findViewById(R.id.id_play);
        m_obj_play.setOnClickListener(this);


        VoiceUtil.getInstance().initLayoutView(m_obj_View, testActivity.this);
        VoiceUtil.getInstance().setRecordBtn(m_obj_record_image_btn, new VoiceUtil.ViewAction() {
            @Override
            public void down() {
                //TODO here
                showBubbleAudio(false);
            }

            @Override
            public void up() {
                //TODO here
                showBubbleAudio(true);
            }
        });

    }


        @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.id_play:
                //playAudio();
                MediaManager.playSound(VoiceUtil.getInstance().getAudioPath(),new MediaPlayer.OnCompletionListener(){

                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        //播放结束
                        //比如可以在这里播放结束后删除音频文件
                    }
                });
                break;
            default:break;
        }
    }


    private void showBubbleAudio(boolean flag){
        if(flag){
            m_obj_play.setVisibility(View.VISIBLE);
        }else{
            m_obj_play.setVisibility(View.GONE);
        }
    }
}
