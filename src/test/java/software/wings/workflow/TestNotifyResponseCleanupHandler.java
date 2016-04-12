/**
 *
 */
package software.wings.workflow;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import software.wings.app.MainConfiguration;
import software.wings.dl.MongoConfig;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.waitNotify.NotifyResponseCleanupHandler;

/**
 * @author Rishi
 *
 */
public class TestNotifyResponseCleanupHandler {
  private static NotifyResponseCleanupHandler notifyResponseCleanupHandler;

  @BeforeClass
  public static void setup() {
    MongoConfig factory = new MongoConfig();
    factory.setDb("test");
    factory.setHost("localhost");
    factory.setPort(27017);
    final MainConfiguration mainConfiguration = new MainConfiguration();
    mainConfiguration.setMongoConnectionFactory(factory);

    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MainConfiguration.class).toInstance(mainConfiguration);
        bind(WingsPersistence.class).to(WingsMongoPersistence.class);
      }
    });

    notifyResponseCleanupHandler = injector.getInstance(NotifyResponseCleanupHandler.class);
  }

  @Test
  public void testCleanup() throws InterruptedException {
    notifyResponseCleanupHandler.run();
    Thread.sleep(10000);
  }
}
