package io.harness.threading;

import io.harness.exception.InterruptedRuntimeException;

import java.time.Duration;

public class Morpheus {
  /**
   * sleep without throwing InterruptedException.
   *
   * @param delay sleep interval in millis.
   */
  public static void quietSleep(Duration delay) {
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException exception) {
      // Ignore
    }
  }

  /**
   * Sleep with runtime exception.
   *
   * @param delay the delay
   */
  public static void sleep(Duration delay) {
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new InterruptedRuntimeException(exception);
    }
  }
}
