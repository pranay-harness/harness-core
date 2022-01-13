/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
