package io.harness.logging;

import io.harness.eraro.MessageManager;
import io.harness.exception.WingsException;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class LoggingInitializer {
  public static final String RESPONSE_MESSAGE_FILE = "/response_messages.properties";

  private static boolean initialized;

  /**
   * Initialize logging.
   */
  public static void initializeLogging() {
    if (!initialized) {
      try (InputStream in = LoggingInitializer.class.getResourceAsStream(RESPONSE_MESSAGE_FILE)) {
        MessageManager.getInstance().addMessages(in);
      } catch (IOException exception) {
        throw new WingsException(exception);
      }

      initialized = true;
    }
  }
}
