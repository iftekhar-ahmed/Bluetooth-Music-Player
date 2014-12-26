package com.apptitive.btmusicplayer.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.util.Log;

import com.apptitive.btmusicplayer.transport.AudioStreamThread;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Iftekhar on 25/12/2014.
 */
public class AudioDecoder extends AsyncTask<Void, byte[], Void> {

    private int inputBufIndex;
    private Boolean doStop = false;

    private FileDescriptor mAudioFileDescriptor;
    private MediaExtractor extractor;
    private MediaCodec codec;
    private AudioTrack audioTrack;
    private AudioStreamThread mAudioStreamThread;

    private void decodeLoop() throws IOException {

        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        // extractor gets information about the stream
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(mAudioFileDescriptor);
        } catch (Exception e) {
            return;
        }

        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);

        // the actual decoder
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        Log.i("sample rate", "" + sampleRate);

        // create our AudioTrack instance
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        // start playing, we will feed you later
        audioTrack.play();
        extractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 50;
        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !doStop) {
            noOutputCounter++;
            if (!sawInputEOS) {
                inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    // can throw illegal state exception (???)
                    codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
            }
            int outputBufIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);
            if (outputBufIndex >= 0) {
                if (info.size > 0) {
                    noOutputCounter = 0;
                }
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];
                byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                mAudioStreamThread.write(chunk);
                audioTrack.write(chunk, 0, chunk.length);
                codec.releaseOutputBuffer(outputBufIndex, false);
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("At", "saw output EOS.");
                sawOutputEOS = true;
            }
        }

        Log.d("At", "stopping...");

        relaxResources(true);

        doStop = true;
    }

    private void relaxResources(Boolean release) {
        if (codec != null) {
            if (release) {
                codec.stop();
                codec.release();
                codec = null;
            }
        }
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
    }

    public AudioDecoder(FileDescriptor audioFileDescriptor, AudioStreamThread audioStreamThread) {
        mAudioFileDescriptor = audioFileDescriptor;
        mAudioStreamThread = audioStreamThread;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            decodeLoop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
