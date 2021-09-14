/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.govern;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class Switch {
  /**
   * Reports error for unhandled value in a switch statement.
   *
   * @param value the switch value that was not handled.
   */
  public static void unhandled(Object value) {
    log.error("Unhandled switch value {}: {}\n{}", value.getClass().getCanonicalName(), value, new Exception(""));
  }

  /**
   * No operation function. Use to indicate intention to do nothing for particular switch case.
   */
  public static void noop() {
    // Noop method
  }
}
