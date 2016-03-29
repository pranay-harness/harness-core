package software.wings.lock;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import software.wings.app.MainConfiguration;
import software.wings.dl.MongoConfig;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;

public class TestPersistentLocker {
  static PersistentLocker persistentLocker;

  @BeforeClass
  public static void setup() {
    MongoConfig factory = new MongoConfig();
    factory.setDb("test");
    factory.setHost("localhost");
    factory.setPort(27017);
    MainConfiguration mainConfiguration = new MainConfiguration();
    mainConfiguration.setMongoConnectionFactory(factory);

    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MainConfiguration.class).toInstance(mainConfiguration);
        bind(WingsPersistence.class).to(WingsMongoPersistence.class);
      }
    });
    persistentLocker = injector.getInstance(PersistentLocker.class);
  }

  @Test
  public void testAcquireLock() {
    String uuid = "" + System.currentTimeMillis();
    System.out.println("uuid : " + uuid);
    boolean acquired = persistentLocker.acquireLock("abc", uuid);
    System.out.println("acquired : " + acquired);

    boolean acquired2 = persistentLocker.acquireLock("abc", uuid);
    System.out.println("acquired2 : " + acquired2);

    //		boolean released = persistentLocker.releaseLock("abc", uuid);
    //		System.out.println("released : " + released);

    //		boolean acquired3 = persistentLocker.acquireLock("abc", uuid);
    //		System.out.println("acquired3 : " + acquired3);
  }
}
