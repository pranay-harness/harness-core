package software.wings.app;

import com.google.inject.Injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used initialize all the resources such as Mongo DB ConnectionPool, Serviceregistry etc.
 *
 * @author Rishi
 */
public class WingsBootstrap {
  private static final Logger logger = LoggerFactory.getLogger(WingsBootstrap.class);
  private static Injector guiceInjector;

  public static MainConfiguration getConfig() {
    return lookup(MainConfiguration.class);
  }

  public static <T> T lookup(Class<T> cls) {
    return guiceInjector.getInstance(cls);
  }

  public static void initialize(Injector injector) {
    guiceInjector = injector;
  }
}
