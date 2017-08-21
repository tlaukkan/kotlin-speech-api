[![Release](https://jitpack.io/v/tlaukkan/kotlin-speech-api.svg)]
(https://jitpack.io/#tlaukkan/kotlin-speech-api)

# Kotlin Speech API

Simple interface for speech using microphone and Google Cloud API.

## Usage

1) Save Google Cloud API service account credentials file to local disk.
2) Set the credentials file path to GOOGLE_APPLICATION_CREDENTIALS environment variable 
3) Execute the following code:

        println("Opening speech recognition...")
        val speaker = Speaker()
        val listener = Listener("en-US", 0.0005, { text ->
            println("Recognised speech: $text")
            speaker.say(text)
            if (text.equals("exit")) {
                exit = true
            }
        })
        println("Opened speech recognition.")

        println("Say exit to quit program.")
        while(!exit) {
            Thread.sleep(100)
        }

        println("Closing speech recognition...")
        listener.close()
        println("Closed speech recognition.")

        println("Exit.")
        