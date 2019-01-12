package com.free.media.mediacodec.encoder;

import com.free.media.mediacodec.utils.MyLogger;

import java.io.IOException;
import java.io.InputStream;

import static com.free.media.mediacodec.utils.Utils.byte2Int;
import static com.free.media.mediacodec.utils.Utils.toInt;
import static com.free.media.mediacodec.utils.Utils.toShort;

public class WavHeader {
    private static final String TAG = "WavHeader";
    private static final int WAVE_HEADER_LEN = 44;
    //header 44 bytes
    private byte[] mChunkId = {'R', 'I', 'F', 'F'};//4 bytes "RIFF"
    private int mChunkSize;//4 bytes
    private byte[] mFormat = {'W', 'A', 'V', 'E'};//4 bytes "WAVE"
    private byte[] mSubChunk1Id = {'f', 'm', 't', ' '};//4 bytes "fmt"
    private int mSubChunk1Size;//4 bytes
    private short mAudioFormat;//2 bytes
    private short mNumChannels;//2 bytes
    private int mSampleRate;//4 bytes
    private int mBitRate;// sample_rate * channels * bit_per_sample / 8
    private short mBlockAlign;
    private short mBitsPerSample;//(8,16,32)
    private byte[] mSubChunk2Id = { 'd', 'a', 't', 'a' };
    private int mSubChunk2Size;
    //Header length in byte
    private  int mLength = 0;
    
    public WavHeader(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.read(mChunkId, 0, 4);
            mLength += 4;
            MyLogger.d(TAG, "mChunkId[0]:" + Integer.toHexString(byte2Int(mChunkId[0])));
            MyLogger.d(TAG, "mChunkId[1]:" + Integer.toHexString(byte2Int(mChunkId[1])));
            MyLogger.d(TAG, "mChunkId[2]:" + Integer.toHexString(byte2Int(mChunkId[2])));
            MyLogger.d(TAG, "mChunkId[3]:" + Integer.toHexString(byte2Int(mChunkId[3])));
            
            byte[] chunkSize = new byte[4];
            mLength += 4;
            inputStream.read(chunkSize, 0, 4);
            mChunkSize = toInt(chunkSize);
            MyLogger.d(TAG, "chunkSize[0]:" + Integer.toHexString(byte2Int(chunkSize[0])));
            MyLogger.d(TAG, "chunkSize[1]:" + Integer.toHexString(byte2Int(chunkSize[1])));
            MyLogger.d(TAG, "chunkSize[2]:" + Integer.toHexString(byte2Int(chunkSize[2])));
            MyLogger.d(TAG, "chunkSize[3]:" + Integer.toHexString(byte2Int(chunkSize[3])));
            
            inputStream.read(mFormat, 0, 4);
            mLength += 4;
            MyLogger.d(TAG, "mFormat[0]:" + Integer.toHexString(byte2Int(mFormat[0])));
            MyLogger.d(TAG, "mFormat[1]:" + Integer.toHexString(byte2Int(mFormat[1])));
            MyLogger.d(TAG, "mFormat[2]:" + Integer.toHexString(byte2Int(mFormat[2])));
            MyLogger.d(TAG, "mFormat[3]:" + Integer.toHexString(byte2Int(mFormat[3])));
            
            inputStream.read(mSubChunk1Id, 0, 4);
            mLength += 4;
            MyLogger.d(TAG, "mSubChunk1Id[0]:" + Integer.toHexString(byte2Int(mSubChunk1Id[0])));
            MyLogger.d(TAG, "mSubChunk1Id[1]:" + Integer.toHexString(byte2Int(mSubChunk1Id[1])));
            MyLogger.d(TAG, "mSubChunk1Id[2]:" + Integer.toHexString(byte2Int(mSubChunk1Id[2])));
            MyLogger.d(TAG, "mSubChunk1Id[3]:" + Integer.toHexString(byte2Int(mSubChunk1Id[3])));
            
            byte[] subChunk1Size = new byte[4];
            inputStream.read(chunkSize, 0, 4);
            mLength += 4;
            mSubChunk1Size = toInt(chunkSize);
            MyLogger.d(TAG, "subChunk1Size[0]:" + Integer.toHexString(byte2Int(chunkSize[0])));
            MyLogger.d(TAG, "subChunk1Size[1]:" + Integer.toHexString(byte2Int(chunkSize[1])));
            MyLogger.d(TAG, "subChunk1Size[2]:" + Integer.toHexString(byte2Int(chunkSize[2])));
            MyLogger.d(TAG, "subChunk1Size[3]:" + Integer.toHexString(byte2Int(chunkSize[3])));
            
            byte[] audioFormat = new byte[2];
            inputStream.read(audioFormat, 0, 2);
            mLength += 2;
            mAudioFormat = toShort(audioFormat);
            MyLogger.d(TAG, "audioFormat[0]:" + Integer.toHexString(byte2Int(audioFormat[0])));
            MyLogger.d(TAG, "audioFormat[1]:" + Integer.toHexString(byte2Int(audioFormat[1])));
    
            byte[] numChannels = new byte[2];
            inputStream.read(numChannels, 0, 2);
            mLength += 2;
            mNumChannels = toShort(numChannels);
            MyLogger.d(TAG, "numChannels[0]:" + Integer.toHexString(byte2Int(numChannels[0])));
            MyLogger.d(TAG, "numChannels[1]:" + Integer.toHexString(byte2Int(numChannels[1])));
    
            byte[] sampleRate = new byte[4];
            inputStream.read(sampleRate, 0, 4);
            mLength += 4;
            MyLogger.d(TAG, "sampleRate[0]:" + Integer.toHexString(byte2Int(sampleRate[0])));
            MyLogger.d(TAG, "sampleRate[1]:" + Integer.toHexString(byte2Int(sampleRate[1])));
            MyLogger.d(TAG, "sampleRate[2]:" + Integer.toHexString(byte2Int(sampleRate[2])));
            MyLogger.d(TAG, "sampleRate[3]:" + Integer.toHexString(byte2Int(sampleRate[3])));
            mSampleRate = toInt(sampleRate);
            MyLogger.d(TAG, "mSampleRate:" + mSampleRate);
    
            byte[] bitRate = new byte[4];
            inputStream.read(bitRate, 0, 4);
            mLength += 4;
            MyLogger.d(TAG, "bitRate[0]:" + Integer.toHexString(byte2Int(bitRate[0])));
            MyLogger.d(TAG, "bitRate[1]:" + Integer.toHexString(byte2Int(bitRate[1])));
            MyLogger.d(TAG, "bitRate[2]:" + Integer.toHexString(byte2Int(bitRate[2])));
            MyLogger.d(TAG, "bitRate[3]:" + Integer.toHexString(byte2Int(bitRate[3])));
            //sample_rate * channels * bit_per_sample / 8
            mBitRate = toInt(bitRate);
            MyLogger.d(TAG, "mBitRate:" + mBitRate);
    
            byte[] blockAlign = new byte[2];
            inputStream.read(blockAlign, 0, 2);
            mLength += 2;
            mBlockAlign = toShort(blockAlign);
    
            MyLogger.d(TAG, "blockAlign[0]:" + Integer.toHexString(byte2Int(blockAlign[0])));
            MyLogger.d(TAG, "blockAlign[1]:" + Integer.toHexString(byte2Int(blockAlign[1])));
    
            byte[] bitsPerSample = new byte[2];
            inputStream.read(bitsPerSample, 0, 2);
            mLength += 2;
    
            MyLogger.d(TAG, "bitsPerSample[0]:" + Integer.toHexString(byte2Int(bitsPerSample[0])));
            MyLogger.d(TAG, "bitsPerSample[1]:" + Integer.toHexString(byte2Int(bitsPerSample[1])));
            mBitsPerSample = toShort(bitsPerSample);
    
            byte[] subChunk2Id = new byte[4];
            inputStream.read(subChunk2Id, 0, 4);
            mLength += 4;
            MyLogger.d(TAG, "subChunk2Id[0]:" + Integer.toHexString(byte2Int(subChunk2Id[0])));
            MyLogger.d(TAG, "subChunk2Id[1]:" + Integer.toHexString(byte2Int(subChunk2Id[1])));
            MyLogger.d(TAG, "subChunk2Id[2]:" + Integer.toHexString(byte2Int(subChunk2Id[2])));
            MyLogger.d(TAG, "subChunk2Id[3]:" + Integer.toHexString(byte2Int(subChunk2Id[3])));
    
            byte[] subChunk2Size = new byte[4];
            inputStream.read(subChunk2Size, 0, 4);
            mLength += 4;
            MyLogger.d(TAG, "subChunk2Size[0]:" + Integer.toHexString(byte2Int(subChunk2Size[0])));
            MyLogger.d(TAG, "subChunk2Size[1]:" + Integer.toHexString(byte2Int(subChunk2Size[1])));
            MyLogger.d(TAG, "subChunk2Size[2]:" + Integer.toHexString(byte2Int(subChunk2Size[2])));
            MyLogger.d(TAG, "subChunk2Size[3]:" + Integer.toHexString(byte2Int(subChunk2Size[3])));
            mSubChunk2Size = toInt(subChunk2Size);
    
            MyLogger.d(TAG, "mLength:" + mLength);
            
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int length() {
        return mLength;
    }
    
    public boolean isWave() {
        return mFormat[0] == 'W' && mFormat[1] == 'A' &&
                mFormat[2] == 'V' && mFormat[3] == 'E';
    }
    
    public int pcmDataLength() {
        if (isPCM()) {
            return mSubChunk2Size;
        }else {
            return 0;
        }
    }
    
    public boolean isPCM() {
        return mAudioFormat == 1;// 1 means PCM
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChunkId:");
        for (int i = 0; i < mChunkId.length; i++) {
            sb.append((char) mChunkId[i]);
        }
        sb.append(";");
        sb.append("ChunkSize:");
        sb.append(mChunkSize);
        sb.append(";");
        sb.append("Format:");
        for (int i = 0; i < mFormat.length; i++) {
            sb.append((char) mFormat[i]);
        }
        sb.append(";");
        sb.append("SubChunk1Id:");
        for (int i = 0; i < mSubChunk1Id.length; i++) {
            sb.append((char) mSubChunk1Id[i]);
        }
        sb.append(";");
        sb.append("mSubChunk1Size:");
        sb.append(mSubChunk1Size);
        sb.append(";");
        sb.append("mAudioFormat:");
        sb.append(mAudioFormat);
        sb.append(";");
        sb.append("mNumChannels:");
        sb.append(mNumChannels);
        sb.append(";");
        sb.append("mSampleRate:");
        sb.append(mSampleRate);
        sb.append(";");
        sb.append("mBitRate:");
        sb.append(mBitRate);
        sb.append(";");
        sb.append("mBlockAlign:");
        sb.append(mBlockAlign);
        sb.append(";");
        sb.append("mBitsPerSample:");
        sb.append(mBitsPerSample);
        sb.append(";");
        sb.append("mSubChunk2Id:");
        for (int i = 0; i < mSubChunk2Id.length; i++) {
            sb.append((char) mSubChunk2Id[i]);
        }
        sb.append(";");
        sb.append("mSubChunk2Size:");
        sb.append(mSubChunk2Size);
        return sb.toString();
    }
    
   
    
    public short getAudioFormat() {
        return mAudioFormat;
    }
    
    public short getNumChannels() {
        return mNumChannels;
    }
    
    public int getSampleRate() {
        return mSampleRate;
    }
    
    public int getBitRate() {
        return mBitRate;
    }
    
    public short getBitsPerSample() {
        return mBitsPerSample;
    }
    
    public int getSubChunk2Size() {
        return mSubChunk2Size;
    }
}
