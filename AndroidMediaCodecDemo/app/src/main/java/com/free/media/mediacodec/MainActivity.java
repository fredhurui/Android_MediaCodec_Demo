package com.free.media.mediacodec;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.free.media.mediacodec.encoder.AudioEncoderActivity;
import com.free.media.mediacodec.encoder.VideoEncoderActivity;
import com.free.media.mediacodec.encoder.VideoTranscodeActivity;

public class MainActivity extends Activity {;
    private static final String TAG = "MediaCodecDemo";
    
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static int REQUEST_PERMISSION_CODE = 1;

    private Button mButtonAudioEncoder;
    private Button mVideoEncoder;
    private Button mVideoTransCodeBtn;
    
    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.buttonAudioEncoder:
                    gotoAudioEncoder();
                    break;
                case R.id.buttonVideoEncoder:
                    gotoVideoEncoder();
                    break;
                case R.id.buttonVideoTranscode:
                    gotoVideoTranscodeActivity();
                default:
                    break;
            }
        
        }
    }
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        int state = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (state != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(PERMISSIONS_STORAGE,
                    REQUEST_PERMISSION_CODE);
        }else {
            setClickListener();
        }
     
    }
    
    private void initViews() {
        mButtonAudioEncoder = findViewById(R.id.buttonAudioEncoder);
        mVideoEncoder = findViewById(R.id.buttonVideoEncoder);
        mVideoTransCodeBtn = findViewById(R.id.buttonVideoTranscode);
       
    }
    
    private void setClickListener() {
        MyOnClickListener listener = new MyOnClickListener();
        mButtonAudioEncoder.setOnClickListener(listener);
        mVideoEncoder.setOnClickListener(listener);
        mVideoTransCodeBtn.setOnClickListener(listener);
    }
    
    
    private void gotoAudioEncoder() {
        Intent intent = new Intent(this, AudioEncoderActivity.class);
        startActivity(intent);
    }
    
    private void gotoVideoEncoder() {
        Intent intent = new Intent(this, VideoEncoderActivity.class);
        startActivity(intent);
    }
    
    private void gotoVideoTranscodeActivity() {
        Intent intent = new Intent(this, VideoTranscodeActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION_CODE) {
            if(grantResults.length == 2 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                setClickListener();
            
            }else {
                finish();
            
            }
        }else {
        
        }
    }

}
