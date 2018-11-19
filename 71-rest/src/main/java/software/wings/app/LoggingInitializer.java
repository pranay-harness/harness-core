package software.wings.app;

import io.harness.eraro.MessageManager;
import io.harness.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;

public class LoggingInitializer {
  private static boolean initialized;

  /**
   * Initialize logging.
   */
  public static void initializeLogging() {
    if (!initialized) {
      try (InputStream in = WingsModule.class.getResourceAsStream(WingsModule.RESPONSE_MESSAGE_FILE)) {
        MessageManager.getInstance().addMessages(in);
      } catch (IOException exception) {
        throw new WingsException(exception);
      }

      initialized = true;
    }
  }
}
