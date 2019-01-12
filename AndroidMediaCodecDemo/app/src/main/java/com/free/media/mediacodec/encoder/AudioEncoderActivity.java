package com.free.media.mediacodec.encoder;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.free.media.mediacodec.R;
import com.free.media.mediacodec.utils.MyLogger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Provide button
 *
 */
public class AudioEncoderActivity extends Activity implements OnClickListener {
    private static final String TAG = "AudioEncoderActivity";
    private final static boolean VERBOSE = true;
    private Button mAACLCButton;
    private Button mAACELDButton;
    
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static int REQUEST_PERMISSION_CODE = 1;
    private Resources mResource;
    private static final long kTimeoutUs = 100;
    private static final int kSampleRates[] = {96000, 88200, 64000, 48000, 44100, 32000, 24000,
            22050, 16000, 12000, 11025, 8000};
    private static final int kSampleRateIndex = 8;
    private static final int kBitRates[] = {64000, 128000, 192000};
    private static final int kBItRateIndex = 2;
    private static final int kChannelCnt = 2;
    
    private String mEncodedDataFolder;//set it /sdcard/Music
    private int mPcmDataResId = 0;
    private ProgressDialog mProgressDialog = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_encoder);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            int state = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (state != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE,
                        REQUEST_PERMISSION_CODE);
            }
        }
        initView();
        mResource = getResources();
        File dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC);
        mEncodedDataFolder = dir.getAbsolutePath();
    }
    
    private void initView() {
        mAACLCButton = findViewById(R.id.btn_aac_lc_encode);
        mAACLCButton.setOnClickListener(this);
        mAACELDButton = findViewById(R.id.btn_aac_eld_encode);
        mAACELDButton.setOnClickListener(this);
        mProgressDialog = new ProgressDialog(this);
        //Drawable d = getResources().getDrawable(R.drawable.progress);
        //mProgressDialog.setProgressDrawable(d);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle("Encoding");//Title
        mProgressDialog.setMessage("Encoding, wating...");//set message
        mProgressDialog.setCancelable(false);
       // mProgressDialog.setProgressStyle(ProgressDiaMyLogger.STYLE_HORIZONTAL);//设置对话框的进度条的风格
        mProgressDialog.setIndeterminate(true);
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_aac_lc_encode:
                mProgressDialog.show();
                v.setClickable(false);
                startAACLCEncoders();
                v.setClickable(true);
                break;
            case R.id.btn_aac_eld_encode:
                mProgressDialog.show();
                v.setClickable(false);
                //mProgressBar.
                startAACELDEncoders();
                v.setClickable(true);
                break;
            default:
                break;
        }
    }
    
    
    private void startAACLCEncoders() {
        MyLogger.d(TAG, "AAC LC encode started");
        MediaCodec encoder = createAndConfigureAACEncoderByProfile(
                MediaCodecInfo.CodecProfileLevel.AACObjectLC,
                R.raw.pcm_1ch_24khz_16bit);
        if (encoder != null) {
            encoder.start();
        }else {
            mProgressDialog.dismiss();
        }
        MyLogger.d(TAG, "AAC LC encode started");
    }
    
    private void startAACELDEncoders() {
        MyLogger.d(TAG, "AAC ELD encode started");
        MediaCodec encoder = createAndConfigureAACEncoderByProfile(
                MediaCodecInfo.CodecProfileLevel.AACObjectELD, R.raw.pcm_1ch_24khz_16bit);
        if (encoder != null) {
            encoder.start();
        }else {
            mProgressDialog.dismiss();
        }
        MyLogger.d(TAG, "AAC ELD encode done");
    }
    
    private MediaCodec createAndConfigureAACEncoderByProfile(int profile, int waveResId) {
    
        InputStream is = openWAVFile(waveResId);
        WavHeader header= parseWAVHeader(is);
        
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        format.setInteger(
                MediaFormat.KEY_AAC_PROFILE, profile);
        format.setInteger(
                MediaFormat.KEY_SAMPLE_RATE, kSampleRates[kSampleRateIndex]);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, header.getNumChannels());
        format.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[kBItRateIndex]);
        MediaCodec encoder = createEncoderByMime(MediaFormat.MIMETYPE_AUDIO_AAC);
        
        encoder.setCallback(new MyCodecCallback(is, format));
        if (encoder != null) {
            encoder.configure(
                    format,
                    null /* surface */,
                    null /* crypto */,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        }
        return encoder;
    }
    
    /**
     * @param mime The mime type
     * @return A MediaCodec instance for encode
     */
    private MediaCodec createEncoderByMime(String mime) {
        if (mime == null) {
            return null;
        }
        MediaCodec codec = null;
        try {
            codec = MediaCodec.createEncoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
            
        }
        return codec;
    }
    
    private MediaMuxer createAACMuxer(MediaFormat format) {
        MediaMuxer muxer = null;
        if (TextUtils.isEmpty(mEncodedDataFolder)) {
            MyLogger.w(TAG, "mEncodedDataFolder is empty.");
            return null;
        }
        String suffix = "";
        if (MediaFormat.MIMETYPE_AUDIO_AAC.equals(format.getString(MediaFormat.KEY_MIME))) {
            suffix = "_" + format.getInteger(MediaFormat.KEY_AAC_PROFILE) + ".m4a";
        } else if (MediaFormat.MIMETYPE_AUDIO_OPUS.equals(format.getString(MediaFormat.KEY_MIME))) {
            suffix = ".opus";
        }
        String filePath = mEncodedDataFolder + "/" + System.currentTimeMillis() + "_" +
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE) + "_" +
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) + suffix;
        MyLogger.d(TAG, "createAACMuxer file:" + filePath);
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            muxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            MyLogger.i(TAG, "couldn't create muxer: " + e);
        }
        return muxer;
    }
    
    private InputStream openWAVFile(int resId) {
        InputStream is = null;
        is = getResources().openRawResource(resId);
        return is;
    }
    
    private WavHeader parseWAVHeader(InputStream is) {
        WavHeader wavHeader = new WavHeader(is);
        MyLogger.d(TAG, "wavHeader:" + wavHeader);
        return wavHeader;
    }
    
    private static interface OnCompletionCallback {
        public void onEncodeComplete();
    };
    
    private class MyCodecCallback extends MediaCodec.Callback {
        
        private BufferedInputStream mInputStream;
        private MediaMuxer mMediaMuxer;
        private int mMediaMuxerId;
        private long mStartTime = 0;
        //for output to muxer
        private long mLastTimeStap = 0;
        
        public MyCodecCallback(InputStream wavStream, MediaFormat format) {
            if (wavStream != null) {
                mInputStream = new BufferedInputStream(wavStream);
            }
            mMediaMuxer = createAACMuxer(format);
        }
        
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            if(mStartTime == 0) {
                mStartTime = System.nanoTime();
            }
            int size = queueInputBuffer(
                    codec, index, mInputStream);
            if (size <= 0) {
                //queue EOS
                long presentationTimeUs = (System.nanoTime() - mStartTime) / 1000;
                codec.queueInputBuffer(
                        index,
                        0 /* offset */,
                        0 /* size */,
                        presentationTimeUs /* timeUs */,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                MyLogger.v(TAG, "queued input EOS.");
            }
            
        }
        
        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index,
                                            MediaCodec.BufferInfo info) {
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                MyLogger.v(TAG, "INFO_TRY_AGAIN_LATER");
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MyLogger.v(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                MyLogger.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
            } else {
                boolean result = dequeueOutputBuffer(codec, index,
                        info, mMediaMuxer, mMediaMuxerId);
                MyLogger.v(TAG, "dequeueOutputBuffer:" + result);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    MyLogger.d(TAG, "dequeued mEncodedDataFolder EOS.");
                    //Now should stop muxer, release muxer, stop codec and release codec
                    release(codec);
                    Toast.makeText(getApplicationContext(),
                            " encode done", Toast.LENGTH_SHORT).show();
                    mProgressDialog.dismiss();
                }
            }
        }
        
        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            MyLogger.w(TAG, "onError");
            
        }
        
        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            MyLogger.w(TAG, "onOutputFormatChanged:" + format);
            if (mMediaMuxer == null) {
                MyLogger.e(TAG, "muxer is null!!");
            } else {
                mMediaMuxerId = mMediaMuxer.addTrack(format);
                MyLogger.d(TAG, "outputformat:" + format);
                mMediaMuxer.start();
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
                    MyLogger.d(TAG, " no more data");
                    return 0;
                }
                buffer.put(content);
                long presentationTimeUs = (System.nanoTime() - mStartTime) / 1000;
                MyLogger.d(TAG, "presentationTimeUs : "  + presentationTimeUs);
                codec.queueInputBuffer(index, 0 /* offset */, result,
                        presentationTimeUs /* timeUs */, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        
            MyLogger.d(TAG, "queue input buffer size:" + result);
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
            if(mLastTimeStap > info.presentationTimeUs) {
                MyLogger.w(TAG, "ignore old data");
                codec.releaseOutputBuffer(index, false /* render */);
                return false;
            }
            mLastTimeStap = info.presentationTimeUs;
            boolean success = false;
            if (muxer != null) {
                ByteBuffer buffer = codec.getOutputBuffer(index);
                MyLogger.d(TAG, "info.presentationTimeUs:" + info.presentationTimeUs);
                muxer.writeSampleData(muxerId, buffer, info);
                success = true;
            }
            codec.releaseOutputBuffer(index, false /* render */);
            return success;
        }
        
        private void release(MediaCodec codec) {
            if (mMediaMuxer != null) {
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
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
}
