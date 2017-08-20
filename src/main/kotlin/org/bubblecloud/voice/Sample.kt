/*
  Copyright 2017, Google Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package org.bubblecloud.voice

object Sample {

    var exit = false

    @Throws(Exception::class)
    @JvmStatic fun main(args: Array<String>) {
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
    }

}

