package io.harness.logging;

import io.harness.eraro.MessageManager;
import io.harness.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class LoggingInitializer {
  public static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static boolean initialized;

  /**
   * Initialize logging.
   */
  public static void initializeLogging() {
    log.info("start initializeLogging");
    if (!initialized) {
      log.info("reset values initializeLogging");
      try (InputStream in = LoggingInitializer.class.getResourceAsStream(RESPONSE_MESSAGE_FILE)) {
        MessageManager.getInstance().addMessages(in);
      } catch (IOException exception) {
        throw new WingsException(exception);
      }

      initialized = true;
    }
    //LoggingTriggerPolicy.resetRollingPolicy();
  }
}
