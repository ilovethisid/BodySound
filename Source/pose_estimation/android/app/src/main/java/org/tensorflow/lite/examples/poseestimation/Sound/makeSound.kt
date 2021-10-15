package org.tensorflow.lite.examples.poseestimation.Sound

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioTrack.MODE_STREAM
import android.media.AudioTrack.WRITE_NON_BLOCKING
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sin
import android.os.Handler
import android.os.HandlerThread
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.tensorflow.lite.examples.poseestimation.data.Note
import java.lang.Thread
class makeSound(){

    companion object {
        private const val sampleRate = 44100
    }
    private var soundStop = false
    private var playState = 0
    private var angle: Double = 0.0
    private var audioTrack: AudioTrack? = null
    private var note = TuneNote
    private var synthFrequency = note.C4
    private var minSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT)
    private var buffer = ShortArray(minSize)
    private var genToneThread: HandlerThread? = null
    private var genToneHandler: Handler? = null

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setAudioTrack(): AudioTrack?{
        if(audioTrack == null){
            audioTrack = AudioTrack.Builder().setTransferMode(MODE_STREAM)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormate.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minSize)
                .build()
        }
        return audioTrack
    }
    private fun getNoteFrequencies():Double{
        return synthFrequency
    }

    private fun oscillator(x:Int, frequencies:Double):Double{
        return sin(Math.PI * x * frequencies)
    }

    private fun generateTone(){
        for (i in buffer.indices) {
            val angularFrequency: Double =
                (Math.PI).toFloat() * synthFrequency / sampleRate
            buffer[i] = (Short.MAX_VALUE * oscillator(1,angle).toFloat()).toInt().toShort()
            angle += angularFrequency
        }
    }
    private fun setNoteFrequency(){

    }
}
fun resume(){
    genTonThread = HandlerThread("genToneThread").apply {start()}
    genTonThreadHandler = Handler(imageReaderThread!!.looper)
}
fun close(){

}
private fun stopgenToneThread(){
    genToneThread?.quitSafely()
    try{
        genTonThread?.join()
        genToneThread = null
        genToneHandler = null
    } catch (e: InterruptedException){
        Log.d(TAG,e.message.toString())
    }
}
private fun playSound(){
    generateTone()
    val player = setAudioTrack()

    player?.write(buffer,0,buffer.size,WRITE_NON_BLOCKING)
    player?.play()

}