package software.wings.lock;

import org.junit.Test;
import software.wings.WingsBaseTest;

import javax.inject.Inject;

/**
 * The Class PersistentLockerTest.
 */
public class PersistentLockerTest extends WingsBaseTest {
  @Inject private PersistentLocker persistentLocker;

  /**
   * Test acquire lock.
   */
  @Test
  public void testAcquireLock() {
    String uuid = "" + System.currentTimeMillis();
    System.out.println("uuid : " + uuid);
    boolean acquired = persistentLocker.acquireLock("abc", uuid);
    System.out.println("acquired : " + acquired);

    boolean acquired2 = persistentLocker.acquireLock("abc", uuid);
    System.out.println("acquired2 : " + acquired2);

    // boolean released = persistentLocker.releaseLock("abc", uuid);
    // System.out.println("released : " + released);

    // boolean acquired3 = persistentLocker.acquireLock("abc", uuid);
    // System.out.println("acquired3 : " + acquired3);
  }
}
