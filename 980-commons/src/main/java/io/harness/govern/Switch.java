/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
