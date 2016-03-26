package software.wings.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
/**
 *  This class is used initialize all the resources such as Mongo DB Connection Pool, Service registry etc.
 *
 *
 * @author Rishi
 *
 */
public class WingsBootstrap {
  public static <T> T lookup(Class<T> cls) {
    return guiceInjector.getInstance(cls);
  }

  public static MainConfiguration getConfig() {
    return lookup(MainConfiguration.class);
  }

  private static final Logger logger = LoggerFactory.getLogger(WingsBootstrap.class);

  /**
   * @param injector
   */
  public static void initialize(Injector injector) {
    guiceInjector = injector;
  }

  private static Injector guiceInjector;
}
