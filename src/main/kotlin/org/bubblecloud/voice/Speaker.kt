package org.bubblecloud.voice

import java.io.File
import marytts.LocalMaryInterface
import marytts.util.data.audio.MaryAudioUtils
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

class Speaker {

    val mary: LocalMaryInterface

    init {
        mary = LocalMaryInterface()
    }

    fun say(text: String) {
        val speechDirectory = File("./speech")
        if (!speechDirectory.exists()) {
            speechDirectory.mkdir()
        }

        val key = text.replace(" ", "_").replace("/", "_").replace("\\", "_").replace(".", "_").replace(",", "_").replace("!", "_").replace(";", "_").replace(":","_").replace("?", "_")
        val path = "${speechDirectory.absolutePath}/${key}.wav"

        if (!File(path).exists()) {
            var audio = mary.generateAudio(text)
            val samples = MaryAudioUtils.getSamplesAsDoubleArray(audio!!)
            MaryAudioUtils.writeWavFile(samples, path, audio.format)
        }

        play(path)
    }

    private fun play(filename: String) {
        val strFilename = filename
        val soundFile = File(strFilename)
        val audioStream = AudioSystem.getAudioInputStream(soundFile)
        val audioFormat = audioStream!!.format
        val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
        val sourceLine = AudioSystem.getLine(info) as SourceDataLine
        sourceLine.open(audioFormat)
        sourceLine.start()

        var count = 0
        val buffer = ByteArray(4096)
        while (count != -1) {
            count = audioStream!!.read(buffer, 0, buffer.size)
            if (count >= 0) {
                sourceLine!!.write(buffer, 0, count)
            }
        }

        sourceLine.drain()
        sourceLine.close()
    }
}