package software.wings.app;

import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLoggerImplFactory;

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
      initialized = true;
    }
  }
}
