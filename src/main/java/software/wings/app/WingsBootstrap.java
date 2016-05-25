package software.wings.app;

import com.google.inject.Injector;

/**
 * Used initialize all the resources such as Mongo DB ConnectionPool, Serviceregistry etc.
 *
 * @author Rishi
 */
public class WingsBootstrap {
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
