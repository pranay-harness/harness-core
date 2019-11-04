package io.harness.queue;

import static io.harness.queue.Queue.Filter.ALL;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.rule.OwnerRule.Owner;
import io.harness.rule.RealMongo;
import io.harness.threading.Poller;
import io.harness.version.VersionInfoManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.time.Duration;

@Slf4j
public class StressTest extends PersistenceTest {
  private static final int COUNT = 1000000;
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private Queue<TestVersionedQueuableObject> versionedQueue;
  @Inject private Queue<TestUnversionedQueuableObject> unversionedQueue;

  @Test
  @Owner(emails = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  @Ignore("Bypass this test, it is not for running regularly")
  public void versionedPerformance() throws IOException {
    assertThatCode(() -> {
      persistence.ensureIndex(TestVersionedQueuableObject.class);

      try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
        for (int i = 1; i <= COUNT; ++i) {
          final TestVersionedQueuableObject queuableObject = new TestVersionedQueuableObject(i);
          queuableObject.setVersion("dummy");

          persistence.save(queuableObject);

          if (i % 10000 == 0) {
            logger.info("Previous version records added: {}, still in queue {}", i, versionedQueue.count(ALL));
          }
        }
        for (int i = 1; i <= COUNT; ++i) {
          versionedQueue.send(new TestVersionedQueuableObject(i));
          if (i % 10000 == 0) {
            logger.info("Correct version records added: {} , still in the queue {}", i, versionedQueue.count(ALL));
          }
        }

        final long start = versionedQueue.count(ALL);
        try {
          Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> {
            final long count = versionedQueue.count(ALL);
            // logger.info("Intermittent queue count: {}", count);
            return count == 0;
          });
        } catch (Exception ignore) {
          // do nothing
        }

        final long diff = start - versionedQueue.count(ALL);
        logger.info("Items handled for 10s: {}", diff);
      }
    })
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(emails = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  @Ignore("Bypass this test, it is not for running regularly")
  public void unversionedPerformance() throws IOException {
    assertThatCode(() -> {
      persistence.ensureIndex(TestUnversionedQueuableObject.class);

      try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
        for (int i = 1; i <= COUNT; ++i) {
          final TestUnversionedQueuableObject queuableObject = new TestUnversionedQueuableObject(i);
          queuableObject.setVersion("dummy");
          unversionedQueue.send(queuableObject);

          if (i % 10000 == 0) {
            logger.info("Previous version records added: {}, still in queue {}", i, unversionedQueue.count(ALL));
          }
        }
        for (int i = 1; i <= COUNT; ++i) {
          unversionedQueue.send(new TestUnversionedQueuableObject(i));
          if (i % 10000 == 0) {
            logger.info("Correct version records added: {} , still in the queue {}", i, unversionedQueue.count(ALL));
          }
        }

        final long start = unversionedQueue.count(ALL);
        try {
          Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> {
            final long count = unversionedQueue.count(ALL);
            // logger.info("Intermittent queue count: {}", count);
            return count == 0;
          });
        } catch (Exception ignore) {
          // do nothing
        }

        final long diff = start - unversionedQueue.count(ALL);
        logger.info("Items handled for 10s: {}", diff);
      }
    })
        .doesNotThrowAnyException();
  }
}
