package io.harness.govern;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Switch {
  protected static @Setter Logger logger = LoggerFactory.getLogger(Switch.class);

  /**
   * Reports error for unhandled value in a switch statement.
   *
   * @param value the switch value that was not handled.
   */
  public static void unhandled(Object value) {
    logger.error("Unhandled switch value {}: {}\n{}", value.getClass().getCanonicalName(), value, new Exception(""));
  }

  /**
   * No operation function. Use to indicate intention to do nothing for particular switch case.
   */
  public static void noop() {}
}
