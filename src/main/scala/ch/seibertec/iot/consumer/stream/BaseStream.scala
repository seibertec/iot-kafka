package ch.seibertec.iot.consumer.stream

import java.util.Properties

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.scala.StreamsBuilder
import org.slf4j.Logger
import java.time.Duration

trait BaseStream {

  protected def build(builder: StreamsBuilder)

  def start(streamingConfig: Properties) = {

    // Create a new builder
    val builder: StreamsBuilder = new StreamsBuilder

    // Invoke the custom build function
    build(builder)

    // Create a configured stream using the provided stream builder and stream config
    val ks: KafkaStreams = new KafkaStreams(builder.build(), streamingConfig)

    // Set the uncaught exception handler for properly shutting down in case of errors
    ks.setUncaughtExceptionHandler(
      (_: Thread, e: Throwable) =>
        try {
          e.printStackTrace()
          val closed: Unit = ks.close(Duration.ofSeconds(10))
        } catch {
          case e: Exception =>
            println("Stream threw exception", e)
        } finally {
          println("Exiting application ..")
          System.exit(-1)
      }
    )

    // Set state handler for catching transitions to ERROR in order to die properly
    ks.setStateListener(
      (newState: KafkaStreams.State, oldState: KafkaStreams.State) => {
        println(s"CHANGING STATE from <$oldState> to <$newState>")
        if (newState == KafkaStreams.State.ERROR) {
          println("Stream enters ERROR state, forcing application shutdown")
          System.exit(-1)
        }
      })

    // Set the shutdown hook in order to close down this stream properly
    sys.ShutdownHookThread {
      println("Shutting down stream")
      ks.close(Duration.ofSeconds(10))
    }

    // Finally start the stream
    println(s"Starting stream ${getClass.getSimpleName}")
    ks.start()
  }

}
