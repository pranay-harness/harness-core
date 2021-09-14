/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.threading;

import io.harness.exception.InterruptedRuntimeException;

import java.time.Duration;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Morpheus {
  /**
   * sleep without throwing InterruptedException.
   *
   * @param delay sleep interval in millis.
   */
  // TODO: this is dangerous method. It should not be used in loops
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
