package org.bubblecloud.voice

import com.google.api.gax.rpc.ApiStreamObserver
import com.google.cloud.speech.v1.*
import com.google.common.util.concurrent.SettableFuture
import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class Listener(val languageCode:String, val speechRmsThreshold: Double, val onSpeechToText: (text: String) -> Unit) {

    val speech = SpeechClient.create()!!
    var buffer = ByteArray(4096)
    var lastBuffer = ByteArray(4096)
    var lastCount = 0
    var shorts = ShortArray(buffer.size / 2)

    val recognitionConfig = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setLanguageCode(languageCode)
            .setSampleRateHertz(16000)
            .setMaxAlternatives(1)
            .build()!!
    val streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .build()!!

    val audioFormat = AudioFormat(16000f, 16, 1, true, false)
    val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
    val microphone = AudioSystem.getLine(dataLineInfo) as TargetDataLine

    var exit = false
    var listening = false
    var microphoneThread: Thread = Thread.currentThread()
    var requestObserver: ApiStreamObserver<StreamingRecognizeRequest>? = null
    var responseObserver: ResponseApiStreamingObserver<StreamingRecognizeResponse>? = null

    var sound = false
    var silenceStartedMillis: Long? = System.currentTimeMillis()
    var soundStartedMillis: Long? = null

    init {
        microphone.open(audioFormat)

        microphone.start()
        microphoneThread = Thread(Runnable {
            while (!exit) {
                val count = microphone.read(buffer, 0, buffer.size)

                ByteBuffer.wrap(buffer, 0, count).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

                var rms = 0.0
                for (i in 0..(count/2) - 1) {
                    val normal = shorts[i] / 32768f
                    rms += normal * normal
                }
                rms = Math.sqrt(rms / shorts.size)
                //println("Listening, rms is " + rms)

                if (rms < speechRmsThreshold) {
                    soundStartedMillis = null
                    if (silenceStartedMillis == null) {
                        silenceStartedMillis = System.currentTimeMillis()
                    }
                    if (sound && System.currentTimeMillis() - silenceStartedMillis!! > 500) {
                        sound = false
                        println("Stop recognizing!!!")
                        stop()
                    }
                    //println("Silence for ${System.currentTimeMillis() - silenceStartedMillis!!} ms.")
                } else {
                    silenceStartedMillis = null
                    if (soundStartedMillis == null) {
                        soundStartedMillis = System.currentTimeMillis()
                    }
                    if (!sound) {
                        sound = true
                        println("Start recognizing!!!")
                        start()
                        // Send also last round buffer.
                        requestObserver!!.onNext(StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(ByteString.copyFrom(lastBuffer, 0, lastCount))
                                .build())
                    }
                    //println("Sound for ${System.currentTimeMillis() - soundStartedMillis!!} ms.")
                }

                if (count > 0) {
                    if (listening) {
                        requestObserver!!.onNext(StreamingRecognizeRequest.newBuilder()
                                .setAudioContent(ByteString.copyFrom(buffer, 0, count))
                                .build())
                    } else {
                        // Save last count.
                        lastCount = count
                        // Switch buffers.
                        val tempBuffer = lastBuffer
                        lastBuffer = buffer
                        buffer = tempBuffer
                    }
                }
            }
        })

        microphoneThread.isDaemon = true
        microphoneThread.start()
    }

    @Synchronized private fun start() {
        if (listening) {
            return
        }
        listening = true
        responseObserver  = ResponseApiStreamingObserver<StreamingRecognizeResponse>()
        requestObserver = speech.streamingRecognizeCallable().bidiStreamingCall(responseObserver)
        requestObserver!!.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingConfig).build())

    }

    @Synchronized private fun stop() {
        if (!listening) {
            return
        }

        listening = false
        requestObserver!!.onCompleted()
        val responses = responseObserver!!.future().get()
        for (response in responses) {
            for (result in response.resultsList) {
                for (alternative in result.alternativesList) {
                    onSpeechToText(alternative.transcript)
                }
            }
        }
    }

    @Synchronized fun close() {
        exit = true
        microphoneThread.join()
        speech.close()
    }


    class ResponseApiStreamingObserver<T> : ApiStreamObserver<T> {
        private val future = SettableFuture.create<List<T>>()
        private val messages = java.util.ArrayList<T>()

        override fun onNext(message: T) {
            messages.add(message)
        }

        override fun onError(t: Throwable) {
            future.setException(t)
        }

        override fun onCompleted() {
            future.set(messages)
        }

        fun future(): SettableFuture<List<T>> {
            return future
        }
    }
}