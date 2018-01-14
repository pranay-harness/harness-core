package software.wings.lock;

import static java.lang.String.format;

import com.deftlabs.lock.mongo.DistributedLock;
import lombok.Builder;
import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.eclipse.jgit.util.time.ProposedTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

@Builder
public class AcquiredLock implements Closeable {
  private static Logger logger = LoggerFactory.getLogger(AcquiredLock.class);

  public static final MonotonicSystemClock monotonicSystemClock = new MonotonicSystemClock();

  private DistributedLock lock;
  private long startTimestamp;

  public static long monotonicTimestamp() {
    try (ProposedTimestamp timestamp = monotonicSystemClock.propose()) {
      return timestamp.millis();
    }
  }

  @Override
  public void close() {
    // Check if procedure took longer than its timeout. This is as bad as not having lock at first place.
    // Any lock that attempts to grab the lock after its timeout will be able to grab it. Resulting in
    // working in parallel with the current process.
    if (monotonicTimestamp() - startTimestamp > lock.getOptions().getInactiveLockTimeout()) {
      logger.error(format("The distributed lock %s was not released on time. THIS IS VERY BAD!!!", lock.getName()));

      // At this point the situation is already troublesome. After the timeout expired the current
      // process potentially overlapped with some other process working at the same time.
      // Lets not make the things even worse with releasing potentially someones else lock.
      // NOTE: letting the lock as is, is not a problem. It being timeout is as good as releasing it.
      return;
    }

    if (!lock.isLocked()) {
      logger.error("attempt to release lock that is not currently locked", new Exception(""));
      return;
    }

    try {
      lock.unlock();
    } catch (RuntimeException ex) {
      logger.warn("releaseLock failed for key: " + lock.getName(), ex);
    }
  }
}
