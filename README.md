# Kotlin Voice Control

Simple interface for voice control using microphone and Google Cloud Speech API.

## Usage

1) Save Google Cloud API service account credentials file to local disk.
2) Set the credentials file path to GOOGLE_APPLICATION_CREDENTIALS environment variable 
3) Execute the following code:

        println("Opening speech recognition...")
        val speech = Speech("en-US", 0.0005, { text ->
            println("Recognised speech: $text")
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
        speech.close()
        println("Closed speech recognition.")

        println("Exit.")