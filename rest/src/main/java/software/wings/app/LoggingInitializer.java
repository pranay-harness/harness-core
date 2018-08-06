package software.wings.app;

import io.harness.eraro.MessageManager;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLoggerImplFactory;
import software.wings.exception.WingsException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by peeyushaggarwal on 6/16/16.
 */
public class LoggingInitializer {
  private static boolean initialized;

  /**
   * Initialize logging.
   */
  public static void initializeLogging() {
    if (!initialized) {
      MorphiaLoggerFactory.registerLogger(SLF4JLoggerImplFactory.class);

      try (InputStream in = WingsModule.class.getResourceAsStream(WingsModule.RESPONSE_MESSAGE_FILE)) {
        MessageManager.getInstance().addMessages(in);
      } catch (IOException exception) {
        throw new WingsException(exception);
      }

      initialized = true;
    }
  }
}
