package com.free.media.mediacodec.decoder;

import android.app.Activity;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.free.media.mediacodec.utils.MyLogger;


public class VideoDecoderActivity extends Activity implements SurfaceHolder.Callback {
    private VideoDecoderThread mVideoDecoder;
    private static final String TAG = "VideoDecoderActivity";

    private static final String FILE_PATH = Environment.getExternalStorageDirectory() +
            "/dump/dump/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyLogger.d(TAG, "onCreate");
        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(this);
        setContentView(surfaceView);
        dumpEncoderInfo();
        dumpDecoderInfo();

        mVideoDecoder = new VideoDecoderThread();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mVideoDecoder != null) {
            if (mVideoDecoder.initCodec(holder.getSurface(), FILE_PATH,
                    1920, 1080)) {
                mVideoDecoder.start();

            } else {
                mVideoDecoder = null;
            }

        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mVideoDecoder != null) {
            mVideoDecoder.close();
        }
    }



    private void dumpEncoderInfo() {
        int numCodecs = MediaCodecList.getCodecCount();
        MyLogger.d(TAG, "Dump encoder info");
        MyLogger.d(TAG, "##################");
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            dumpCodecInfo(codecInfo);
        }
        MyLogger.d(TAG, "##################");
    }

    private void dumpDecoderInfo() {
        int numCodecs = MediaCodecList.getCodecCount();
        MyLogger.d(TAG, "Dump decoder info");
        MyLogger.d(TAG, "*********************");
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()) {
                continue;
            }
            dumpCodecInfo(codecInfo);
        }
        MyLogger.d(TAG, "*********************");
    }

    private void dumpCodecInfo(MediaCodecInfo codecInfo) {
        if(codecInfo == null) {
            return;
        }
        String name = codecInfo.getName();
        String types[] = codecInfo.getSupportedTypes();
        StringBuilder sb = new StringBuilder("codecInfo:{");
        for( String type : types) {
            sb.append("type:" + type + "[");
            MediaCodecInfo.CodecCapabilities capabilities =
                    codecInfo.getCapabilitiesForType(type);
            int [] colorFormats = capabilities.colorFormats;
            sb.append("colorFormat:(");
            for (int colorFormat : colorFormats) {
                sb.append(colorFormat);
                sb.append(",");
            }
            sb.append(")");
            MediaCodecInfo.CodecProfileLevel[] profileLevels = capabilities.profileLevels;
            sb.append("CodecProfileLevel:(" );
            for (MediaCodecInfo.CodecProfileLevel profileLevel : profileLevels) {
                sb.append("<");
                sb.append(profileLevel.profile);
                sb.append(",");
                sb.append(profileLevel.level);
                sb.append(">");
                sb.append(",");
            }
            sb.append(")");
            sb.append("]");
        }
        sb.append("}");
        MyLogger.d(TAG, sb.toString());
    }

}
