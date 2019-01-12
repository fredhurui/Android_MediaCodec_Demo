package com.free.media.mediacodec.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.free.media.mediacodec.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class VideoTranscodeActivity extends Activity {
    
    private static final String TAG = "VideoTranscodeActivity";
    
    private static final int ENCODED_VIDEO_WIDTH = 1080;
    private static final int ENCODED_VIDEO_HEIGHT = /*720*/ 960;//just test the clop function
    private static final String VIDEO_OUT_PATH = "/sdcard/Movies/";
    private static final String VIDEO_FILE_PATH =
            "/sdcard/Movies/10_H.264_Baseline_1920_1080_60_946kbps.mp4";
    
    private MediaCodec mEncoder;
    //For encoder to write encoded data to file
    private MediaMuxer mMuxer;
    private MediaFormat mEncoderVideoFormat;
    private Surface mEncoderInputSurface;
    private MyEncoderCodecCallback mEncoderCallback;
    
    private MediaCodec mDecoder;
    private MediaFormat mDecoderMediaFormat;
    private MediaExtractor mDecoderExtractor;
    private MyDecoderCodecCallback mDecoderCallback ;
    
    private MyHandler mMainHandler;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_transcode);
        mMainHandler = new MyHandler(getMainLooper());
        initViews();
    }
    
    private void initViews() {
        Button from1080To720Btn = findViewById(R.id.button1080pto720p);
    
        MyClickListener listener = new MyClickListener();
        from1080To720Btn.setOnClickListener(listener);
        
    }
    
    private class MyClickListener implements View.OnClickListener {
    
        @Override
        public void onClick(View v) {
            
            switch (v.getId()) {
                case R.id.button1080pto720p:
                    showMessage("start transcode");
                    handleTranscodeFrom1080To720();
                    break;
            }
        
        }
    }
    
    private void handleTranscodeFrom1080To720() {
        createAndConfigureAVCCodec();
        mMuxer = createAVCMuxer(mEncoderVideoFormat);
        startCodec();
    }
    
    
    private class MyEncoderCodecCallback extends MediaCodec.Callback {
        private static final String TAG = "MyEncoderCodecCallback";
        private BufferedInputStream mInputStream;
        private int mMediaMuxerId;
        
        public MyEncoderCodecCallback() {
        }
        
        public void closeEncoder(MediaCodec codec) {
            Log.d(TAG, "Encoder EOS.");
            //Now should stop muxer, release muxer, stop codec and release codec
            release(codec);
            showMessage("transcode done");
        }
        
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            Log.d(TAG, "onInputBufferAvailable");
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
            //Log.d(TAG, "onOutputBufferAvailable");
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
                    Log.d(TAG, "Encoder EOS.");
                    //Now should stop muxer, release muxer, stop codec and release codec
                    release(codec);
                    showMessage("transcode done");
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
    
    
    private class MyDecoderCodecCallback extends MediaCodec.Callback {
        private static final String TAG = "MyDecoderCodecCallback";
        
        private MediaExtractor mMediaExtractor;
        private boolean mEOS = false;
        
        public MyDecoderCodecCallback(MediaExtractor extractor) {
            mMediaExtractor = extractor;
        }
        
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            //Log.d(TAG, "onInputBufferAvailable");
            if(mEOS == true) {
                Log.v(TAG, "Strange why come here, since EOS already queued.");
                //return;
            }
            int size = queueInputBuffer(
                    codec, index, mMediaExtractor);
            if (mMediaExtractor.advance() == false) {
                mEOS = true;
                codec.queueInputBuffer(
                        index,
                        0 /* offset */,
                        0 /* size */,
                        mMediaExtractor.getSampleTime() /* timeUs */,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                Log.v(TAG, "queued input EOS.");
            }
            //go to next sample
            //mMediaExtractor.advance();
            
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
                //render to surface which will route to encoder input
                codec.releaseOutputBuffer(index, true /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "EOS for decoder.");
                    //Now should release codec
                    release(codec);
                    //Need to tell the encoder to feed EOS and close muxer, codec
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mEncoderCallback.closeEncoder(mEncoder);
                        }
                    }, 1000);
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
        }
        
        private void release(MediaCodec codec) {
            if (codec != null) {
                codec.stop();
                codec.release();
                codec = null;
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
     * Read data from extractor and queue it to codec
     *
     * @param codec       MediaCodec instance
     * @param index       input buffer index
     * @param extractor   extractor to read the sample data
     * @return the bytes queued to codec
     */
    private int queueInputBuffer(
            MediaCodec codec, int index,
            MediaExtractor extractor) {
        ByteBuffer buffer = codec.getInputBuffer(index);
        buffer.clear();
        int result = 0;
        result = extractor.readSampleData(buffer, 0);
        if (result == -1) {
            Log.d(TAG, " no more data");
            return 0;
        }
        long presentationTimeUs = extractor.getSampleTime();
        codec.queueInputBuffer(index, 0 /* offset */, result,
                presentationTimeUs /* timeUs */, 0);
    
        //Log.d(TAG, "queue input buffer size:" + result);
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
    
    private void createAndConfigureAVCCodec() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                mDecoderExtractor = new MediaExtractor();
                mDecoderExtractor.setDataSource(VIDEO_FILE_PATH);
                //extract the video track
                for (int i = 0; i < mDecoderExtractor.getTrackCount(); i++) {
                    if(mDecoderExtractor.getTrackFormat(i).
                            getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                        mDecoderMediaFormat = mDecoderExtractor.getTrackFormat(i);
                        mDecoderExtractor.selectTrack(i);
                    }
                }
                if(mDecoderMediaFormat == null) {
                    Log.e(TAG, "no video track in source file");
                    return;
                }
                Log.d(TAG, "mDecoderMediaFormat = " + mDecoderMediaFormat);
                // Create an encoder format that matches the input format.  (Might be able to just
                // re-use the format used to generate the video, since we want it to be the same.)
                mEncoderVideoFormat = MediaFormat.createVideoFormat(
                        MediaFormat.MIMETYPE_VIDEO_AVC, ENCODED_VIDEO_WIDTH, ENCODED_VIDEO_HEIGHT);
                mEncoderVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mEncoderVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE,
                        mDecoderMediaFormat.getInteger(MediaFormat.KEY_BIT_RATE));
                mEncoderVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,
                        mDecoderMediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE));
                mEncoderVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
                        1);
                mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
               
               
                mEncoderCallback = new MyEncoderCodecCallback();
                mEncoder.setCallback(mEncoderCallback);
                mEncoder.configure(mEncoderVideoFormat,
                        null,
                        null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                //create a surface to receive the data from decoder
                //it must be called after configured
                mEncoderInputSurface = mEncoder.createInputSurface();
                
                mDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                mDecoderCallback =
                        new MyDecoderCodecCallback(mDecoderExtractor);
                mDecoder.setCallback(mDecoderCallback);
                mDecoder.configure(mDecoderMediaFormat, mEncoderInputSurface, null, 0);
               
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("create codec fail");
            }
        }
    }
    
    private void startCodec() {
        mEncoder.start();
        mDecoder.start();
    }
    
    private void showMessage(String message) {
        Log.d(TAG, "show message:" + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private static class MyHandler extends Handler {
        
        public MyHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
    
}
