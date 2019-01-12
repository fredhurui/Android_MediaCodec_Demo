package com.free.media.mediacodec.decoder;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;

import com.free.media.mediacodec.utils.MyLogger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoDecoderThread extends Thread {
    private static final String VIDEO = "video/";
    private static final String TAG = "VideoDecoderThread";
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private BufferedInputStream mBufferedInputStream;

    private boolean mEosReceived;
    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mMaxBufferLen = 0;
    byte[] mFrameData = null;
    private boolean mFirstTimeToDecode = true;
    private static final int FRAME_RATE = 30;
    private static final int CODEC_TIME_OUT = 10 * 1000;
    private int mFrameFileCount = 0;
    private List<File> mH264FileList = null;

    public boolean init(Surface surface, String filePath) {
        mEosReceived = false;
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(filePath);

            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);

                String mime = format.getString(MediaFormat.KEY_MIME);

                if (mime.startsWith(VIDEO)) {
                    mExtractor.selectTrack(i);
                    mDecoder = MediaCodec.createDecoderByType(mime);
                    try {
                        MyLogger.d(TAG, "format : " + format);
                        //int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                        //MyLogger.d(TAG, "colorFormat : " + colorFormat);
                        mDecoder.configure(format, surface, null, 0 /* Decoder */);
                        MediaFormat outputFormat = mDecoder.getOutputFormat();
                        Bundle para = new Bundle();
                        para.putInt("intel.decodedorder", 1);
                        mDecoder.setParameters(para);
                        try {
                            int colorFormat = outputFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                            MyLogger.d(TAG, "colorFormat : " + colorFormat);
                        } catch (Exception e) {
                            MyLogger.d(TAG, "get color format fail");
                        }
                    } catch (IllegalStateException e) {
                        MyLogger.e(TAG, "codec '" + mime + "' failed configuration. " + e);
                        return false;
                    }
                    mDecoder.start();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Init codec with a h264 frame list
     *
     * @param surface
     * @param filePath a folder which contains many h264 frame, name is xx_1.h264, xx_2.h264
     * @return
     */
    public boolean initCodec(Surface surface, String filePath,
                             int width, int height) {
        mEosReceived = false;
        mVideoWidth = width;
        mVideoHeight = height;
        mMaxBufferLen = mVideoWidth * mVideoHeight * 3 / 2;
        mFrameData = new byte[mMaxBufferLen];
        File file = new File(filePath);
        if(file == null ||file.exists() == false) {
            MyLogger.e(TAG, "file do not exist");
            return false;
        }
        mH264FileList =  Arrays.asList(file.listFiles());
        //mH264FileList =  file.listFiles();
        if(mH264FileList.size() <= 0) {
            MyLogger.e(TAG, "folder is empty");
            return false;
        }
        String mime = "video/avc";
        try {
            Collections.sort(mH264FileList, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    MyLogger.d(TAG, "nameLhs: " + lhs.getName());
                    MyLogger.d(TAG, "rhs: " + rhs.getName());
                    int index_ = lhs.getName().indexOf('_');
                    int indexDot = lhs.getName().indexOf('.');
                    MyLogger.d(TAG, "1:index_: " + index_);
                    MyLogger.d(TAG, "1:indexDot: " + indexDot);
                    String nameLhs = lhs.getName().substring(index_ + 1, indexDot);
                    index_ = rhs.getName().indexOf('_');
                    indexDot = rhs.getName().indexOf('.');
                    MyLogger.d(TAG, "2:index_: " + index_);
                    MyLogger.d(TAG, "2:indexDot: " + indexDot);
                    String nameRhs = rhs.getName().substring(index_ + 1, indexDot);
                    MyLogger.d(TAG, "nameLhs: " + nameLhs);
                    MyLogger.d(TAG, "nameRhs: " + nameRhs);
                    Integer name1 = Integer.parseInt(nameLhs);
                    Integer name2 = Integer.parseInt(nameRhs);
                    MyLogger.d(TAG, "name1: " + name1);
                    MyLogger.d(TAG, "name2: " + name2);
                    if(name1 > name2) {
                        return 1;
                    }else if(name1 == name2) {
                        return 0;
                    }else{
                        return -1;
                    }
                }
            });

            MyLogger.e(TAG, "open file 1 : " + mH264FileList.get(0).getAbsolutePath());
            int fileIndex = 0;
            /*for(int i = 0; i< mH264FileList.size(); i++ ){
                if(mH264FileList.get(i).getAbsolutePath().contains("carplalydump_1.h264")){
                    MyLogger.e(TAG, "open file 1 : " + mH264FileList.get(i).getAbsolutePath());
                    fileIndex = i;
                    break;
                }
            }*/
            mBufferedInputStream = new BufferedInputStream(new FileInputStream(
                    mH264FileList.get(0)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            MyLogger.e(TAG, "open file failed:" + filePath);
        }
        try {
            mDecoder = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            MediaFormat format = MediaFormat.createVideoFormat(mime,
                    +mVideoWidth,
                    mVideoHeight);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            MyLogger.d(TAG, "format : " + format);
            //int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
            //MyLogger.d(TAG, "colorFormat : " + colorFormat);
            mDecoder.configure(format, surface, null, 0 /* Decoder */);
            MediaFormat outputFormat = mDecoder.getOutputFormat();
            try {
                int colorFormat = outputFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                MyLogger.d(TAG, "colorFormat : " + colorFormat);
            } catch (Exception e) {
                MyLogger.d(TAG, "get color format fail");
            }

        } catch (IllegalStateException e) {
            MyLogger.e(TAG, "codec '" + mime + "' failed configuration. " + e);
            return false;
        }

        mDecoder.start();

        return true;
    }

    @Override
    public void run() {
        BufferInfo info = new BufferInfo();
        if (mDecoder == null) {
            MyLogger.d(TAG, "mDecoder is null");
            return;
        }
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();

        boolean isInput = true;
        mFirstTimeToDecode = true;
        Long startWhen = new Long(0);
        Integer frameIndex = new Integer(0);
        MyLogger.d(TAG, "first:" + mFirstTimeToDecode);
        while (!mEosReceived) {
            if (isInput) {
                isInput = feedData(isInput, inputBuffers, frameIndex);
                ++frameIndex;
            }
            MyLogger.d(TAG, "frameIndex:" + frameIndex);

            startWhen = readData(info, startWhen);
            MyLogger.d(TAG, "first:" + mFirstTimeToDecode);
            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                MyLogger.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }
        mDecoder.stop();
        mDecoder.release();
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
        if (mBufferedInputStream != null) {
            try {
                mBufferedInputStream.close();
                mBufferedInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Feed data to decoder to decode
     *
     * @param isInput
     * @param inputBuffers
     * @param frameIndex
     * @return
     */
    private boolean feedData(boolean isInput, ByteBuffer[] inputBuffers, Integer frameIndex) {
        int inputIndex = mDecoder.dequeueInputBuffer(CODEC_TIME_OUT);
        if (inputIndex >= 0) {
            // fill inputBuffers[inputBufferIndex] with valid data
            ByteBuffer inputBuffer = inputBuffers[inputIndex];
            if (mExtractor != null) {
                int sampleSize = mExtractor.readSampleData(inputBuffer, 0);

                if (mExtractor.advance() && sampleSize > 0) {
                    mDecoder.queueInputBuffer(inputIndex, 0,
                            sampleSize, mExtractor.getSampleTime(), 0);

                } else {
                    MyLogger.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mDecoder.queueInputBuffer(inputIndex, 0, 0,
                            0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isInput = false;
                }
            } else if (mBufferedInputStream != null) {
                try {
                    mBufferedInputStream = new BufferedInputStream(new FileInputStream(
                            mH264FileList.get(frameIndex)));
                    int size = mBufferedInputStream.read(mFrameData,
                            0, mMaxBufferLen);
                    if (size > 0) {
                        long ptsUsec = computePresentationTime(frameIndex);
                        inputBuffer.clear();
                        inputBuffer.put(mFrameData);
                        inputBuffer.flip();
                        MyLogger.d(TAG, "ptsUsec:" + ptsUsec);
                        mDecoder.queueInputBuffer(inputIndex, 0, size,
                                0, 0);
                    } else {
                        MyLogger.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        mDecoder.queueInputBuffer(inputIndex, 0, 0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isInput = false;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return isInput;
    }

    /**
     * Read decoded data from decoder
     *
     * @param info
     * @param startWhen
     * @return true if it is first frame, otherwise return false
     */
    private long readData(BufferInfo info, Long startWhen) {

        int outIndex = mDecoder.dequeueOutputBuffer(info, CODEC_TIME_OUT);
        switch (outIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                MyLogger.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                mDecoder.getOutputBuffers();
                break;

            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                MyLogger.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " +
                        mDecoder.getOutputFormat());
                break;

            case MediaCodec.INFO_TRY_AGAIN_LATER:
                MyLogger.d(TAG, "INFO_TRY_AGAIN_LATER");
                break;

            default:
                MyLogger.d(TAG, "First time default:" + mFirstTimeToDecode);
                if (mFirstTimeToDecode) {
                    startWhen = System.currentTimeMillis();
                    MyLogger.d(TAG, "First time startWhen : " + startWhen);
                    mFirstTimeToDecode = false;
                }
                try {
                    long sleepTime = (info.presentationTimeUs / 1000) -
                            (System.currentTimeMillis() - startWhen);
                    MyLogger.d(TAG, "info.presentationTimeUs : " +
                            (info.presentationTimeUs / 1000) +
                            " playTime: " + (System.currentTimeMillis() - startWhen) +
                            " sleepTime : " + sleepTime);

                    if (sleepTime > 0)
                        Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
                break;
        }
        return startWhen;
    }

    public void close() {
        mEosReceived = true;
    }

    /**
     * 1368     * Generates the presentation time for frame N, in microseconds.
     * 1369
     */
    private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }
}
