package com.free.media.mediacodec.encoder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.free.media.mediacodec.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class VideoEncoderActivity extends Activity {
    
    private static final String TAG = "VideoEncoderActivity";
    private static final int ENCODED_VIDEO_WIDTH = 1920;
    private static final int ENCODED_VIDEO_HEIGHT = 1080;
    
    private static final int REQUEST_CODE_CAPTURE_PERM = 1234;
    
    private static final String VIDEO_OUT_PATH = "/sdcard/Movies/";
    private static final String VIDEO_FILE_PATH =
            "/sdcard/Movies/06_H.264_Main_1280_720_23.976_14.5Mbps.mp4";
    
    private SurfaceView mSurfaceView;
    private Surface mInputSurface;
    //start playback and record
    private Button mPlayButton;
    //pause play back, still record
    private Button mPauseButton;
    //Stop playback and stop record
    private Button mStopButton;
    
    private MediaCodec mCodec;
    private MediaMuxer mMuxer;
    private MediaPlayer mPlayer;
    private MediaFormat mVideoFormat;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    
    private static final int STATUS_INVALID = -1;
    private static final int STATUS_PLAYING = 0;
    private static final int STATUS_PAUSED = 1;
    private static final int STATUS_AUTO_PAUSED = 2;
    //Player state
    private int mState = STATUS_INVALID;
    
    private static final int CODEC_STATUS_NOT_STARTED = 0;
    private static final int CODEC_STATUS_STARTED = 1;
    //Encoder state
    private int mCodecState = CODEC_STATUS_NOT_STARTED;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_encoder);
        createAndConfigureAVCCodec();
        mMediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        initViews();
        
    }
    
    @Override
    protected void onPause() {
        if(mState == STATUS_PLAYING) {
            mState = STATUS_AUTO_PAUSED;
            mPlayer.pause();
        }
        
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if(mState == STATUS_AUTO_PAUSED) {
            mState = STATUS_PLAYING;
            mPlayer.start();
        }
        
    }
    
    private void initViews() {
        mPauseButton = findViewById(R.id.buttonPause);
        mPlayButton = findViewById(R.id.buttonPlay);
        mStopButton = findViewById(R.id.buttonStop);
        mSurfaceView = findViewById(R.id.surfaceView);
    
        mSurfaceView.getHolder().addCallback(new MySurfaceHolderCallback());
    
        MyClickListener listener = new MyClickListener();
        mPlayButton.setOnClickListener(listener);
        mPauseButton.setOnClickListener(listener);
        mStopButton.setOnClickListener(listener);
    }
    
    private boolean initAndStartMediaPlayer() {
        mPlayer = new MediaPlayer();
        mMuxer = createAVCMuxer(mVideoFormat);
        Log.d(TAG, "initAndStartMediaPlayer mMuxer:" + mMuxer);
        if(mMuxer == null) {
            Toast.makeText(getApplicationContext(),
                    "create Muxer fail", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            mPlayer.setDataSource(VIDEO_FILE_PATH);
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mPlayer.start();
                }
            });
            mPlayer.prepareAsync();
            mPlayer.setSurface(mSurfaceView.getHolder().getSurface());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    "Set data source fail", Toast.LENGTH_SHORT).show();
            mPlayer.release();
            mPlayer = null;
            return false;
        }
        return true;
    }
    
    private void createAndConfigureAVCCodec() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                        ENCODED_VIDEO_WIDTH, ENCODED_VIDEO_HEIGHT);
                int frameRate = 30; // 30 fps
    
                //
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000); //6Mbps
                format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
               format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames
                mVideoFormat = format;
                mCodec.configure(format,
                        null,
                        null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                mInputSurface = mCodec.createInputSurface();
                //mCodec.setInputSurface(mSurfaceView.getHolder().getSurface());
                MyCodecCallback codecCallback = new MyCodecCallback();
                mCodec.setCallback(codecCallback);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void stopRecord() {
        if(mState == STATUS_INVALID) {
            Log.d(TAG, "do nothing in invalid state");
            return;
        }
        if(mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
        if(mCodec != null) {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
        }
        if(mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        mState = STATUS_INVALID;
        finish();
    }
    
    
    private class MyClickListener implements View.OnClickListener {
    
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.buttonPause:
                    if(mState == STATUS_PLAYING) {
                        mPlayer.pause();
                        mState = STATUS_PAUSED;
                    }
                   
                    break;
                case R.id.buttonPlay:
                    Intent permissionIntent = mMediaProjectionManager.createScreenCaptureIntent();
                    Log.d(TAG, "permissionIntent:" + permissionIntent);
                    startActivityForResult(permissionIntent, REQUEST_CODE_CAPTURE_PERM);
                    break;
                case R.id.buttonStop:
                    stopRecord();
                    break;
            }
        
        }
    }
    
    private class MySurfaceHolderCallback implements SurfaceHolder.Callback {
    
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if(mCodecState == CODEC_STATUS_NOT_STARTED) {
                mCodecState = CODEC_STATUS_STARTED;
                mCodec.start();
            }else {
                Log.d(TAG, "Codec already started");
            }
            if(mState == STATUS_PLAYING) {
                mPlayer.setSurface(holder.getSurface());
            }
            
            
        }
    
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if(mState == STATUS_PLAYING) {
                mPlayer.setSurface(holder.getSurface());
            }
        }
    
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        
        }
    }
    
    
    private class MyCodecCallback extends MediaCodec.Callback {
        private BufferedInputStream mInputStream;
        //private MediaMuxer mMediaMuxer;
        private int mMediaMuxerId;
        private MediaCodec mCodec;
        
        public MyCodecCallback() {
        }
        
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            int size = queueInputBuffer(
                    codec, index, mInputStream);
            if (size <= 0) {
                long presentationTimeUs = System.nanoTime();
                codec.queueInputBuffer(
                        index,
                        0 /* offset */,
                        0 /* size */,
                        presentationTimeUs /* timeUs */,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                Log.v(TAG, "queued input EOS.");
            }
            
        }
        
        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index,
                                            MediaCodec.BufferInfo info) {
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.v(TAG, "INFO_TRY_AGAIN_LATER");
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
            } else {
                boolean result = dequeueOutputBuffer(codec, index,
                        info, mMuxer, mMediaMuxerId);
                //Log.v(TAG, "dequeueOutputBuffer:" + result);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "dequeued mEncodedDataFolder EOS.");
                    //Now should stop muxer, release muxer, stop codec and release codec
                    release(codec);
                }
            }
        }
        
        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.w(TAG, "onError");
            
        }
        
        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.w(TAG, "onOutputFormatChanged:" + format);
            if (mMuxer == null) {
                Log.e(TAG, "muxer is null!!");
            } else {
                mMediaMuxerId = mMuxer.addTrack(format);
                Log.d(TAG, "start muxer, outputformat:" + format);
                mMuxer.start();
            }
            
        }
        
        private void release(MediaCodec codec) {
            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
                mMuxer = null;
            }
            if (codec != null) {
                codec.stop();
                codec.release();
                codec = null;
            }
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mInputStream = null;
            }
        }
    }
    
    /**
     * Read data from input stream and queue it to codec
     *
     * @param codec       MediaCodec instance
     * @param index       input buffer index
     * @param inputStream input file which is used to encode
     * @return the bytes queued to codec
     */
    private int queueInputBuffer(
            MediaCodec codec, int index,
            InputStream inputStream) {
        ByteBuffer buffer = codec.getInputBuffer(index);
        buffer.clear();
        int size = buffer.limit();
        byte[] content = new byte[size];
        int result = 0;
        try {
            result = inputStream.read(content);
            if (result == -1) {
                Log.d(TAG, " no more data");
                return 0;
            }
            buffer.put(content);
            long presentationTimeUs = System.nanoTime();
            codec.queueInputBuffer(index, 0 /* offset */, result,
                    presentationTimeUs /* timeUs */, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        Log.d(TAG, "queue input buffer size:" + result);
        return result;
    }
    
    /**
     * Read the output buffer from codec and write it to file by MediaMuxer
     *
     * @param codec   MediaCodec instance
     * @param index   output buffer index
     * @param info    codec buffer info
     * @param muxer   MediaMuxer
     * @param muxerId MediaMuxer id
     * @return true if write file success other wise return false
     */
    private boolean dequeueOutputBuffer(
            MediaCodec codec,
            int index, MediaCodec.BufferInfo info,
            MediaMuxer muxer, int muxerId) {
        boolean success = false;
        if (muxer != null) {
            ByteBuffer buffer = codec.getOutputBuffer(index);
            muxer.writeSampleData(muxerId, buffer, info);
            success = true;
        }
        codec.releaseOutputBuffer(index, false /* render */);
        return success;
    }
    
    private MediaMuxer createAVCMuxer(MediaFormat format) {
        MediaMuxer muxer = null;
        String suffix = "";
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (MediaFormat.MIMETYPE_VIDEO_AVC.equals(mime)) {
            suffix = ".mp4";
        } else  {
            Log.d(TAG, "createAVCMuxer no handled for " + mime);
        }
        String filePath = VIDEO_OUT_PATH + "/" + System.currentTimeMillis() + "_" +
                format.getInteger(MediaFormat.KEY_WIDTH) + "_" +
                format.getInteger(MediaFormat.KEY_HEIGHT) + "_" +
                format.getInteger(MediaFormat.KEY_FRAME_RATE)+ "_" +
                mime.replace('/', '_') +
                suffix;
        Log.d(TAG, "createAVCMuxer file:" + filePath);
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            muxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            Log.i(TAG, "couldn't create muxer: " + e);
        }
        return muxer;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (REQUEST_CODE_CAPTURE_PERM == requestCode) {
            Log.d(TAG, "resultCode:" + resultCode);
            if(resultCode == RESULT_OK) {
                mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
                Log.d(TAG, "mMediaProjection:" + mMediaProjection);
                startRecording();
            }else {
                Toast.makeText(getApplicationContext(), "permission denied",
                        Toast.LENGTH_SHORT).show();
            }
           
        }
    }
    
    private void startRecording() {
        startPlayback();
        // Get the display size and density.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;
        // Start the video input.
        mMediaProjection.createVirtualDisplay("Recording Display", screenWidth,
                screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR/* flags */, mInputSurface,
                null /* callback */, null /* handler */);
    }
    
    private void startPlayback() {
        if(mState == STATUS_INVALID) {
            boolean status = initAndStartMediaPlayer();
            if(status == false) {
                Log.e(TAG, "init player fail");
                return;
            }
            mState = STATUS_PLAYING;
        }else if(mState == STATUS_PLAYING) {
            Log.d(TAG, "already playing");
            return;
        }else if(mState == STATUS_PAUSED) {
            mPlayer.start();
            return;
        }
    }
}
