package software.wings.lock;

import org.junit.BeforeClass;
import org.junit.Test;

import software.wings.dl.MongoConnectionFactory;
import software.wings.dl.WingsMongoPersistence;

public class TestPersistentLocker {
  static PersistentLocker persistentLocker;

  @BeforeClass
  public static void setup() {
    MongoConnectionFactory factory = new MongoConnectionFactory();
    factory.setDb("test");
    factory.setHost("localhost");
    factory.setPort(27017);

    WingsMongoPersistence wingsPersistence = new WingsMongoPersistence(factory.getDatastore());
    persistentLocker = new PersistentLocker(wingsPersistence);
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
