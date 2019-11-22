package io.harness.queue;

import static io.harness.queue.QueueConsumer.Filter.ALL;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.StressTests;
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
  @Inject private QueueConsumer<TestVersionedQueuableObject> versionedConsumer;
  @Inject private QueuePublisher<TestVersionedQueuableObject> versionedPublisher;
  @Inject private QueueConsumer<TestUnversionedQueuableObject> unversionedConsumer;
  @Inject private QueuePublisher<TestUnversionedQueuableObject> unversionedPublisher;

  @Test
  @Owner(developers = GEORGE)
  @Category(StressTests.class)
  @RealMongo
  public void versionedPerformance() throws IOException {
    assertThatCode(() -> {
      persistence.ensureIndex(TestVersionedQueuableObject.class);

      try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
        for (int i = 1; i <= COUNT; ++i) {
          final TestVersionedQueuableObject queuableObject = new TestVersionedQueuableObject(i);
          queuableObject.setVersion("dummy");

          persistence.save(queuableObject);

          if (i % 10000 == 0) {
            logger.info("Previous version records added: {}, still in queue {}", i, versionedConsumer.count(ALL));
          }
        }
        for (int i = 1; i <= COUNT; ++i) {
          versionedPublisher.send(new TestVersionedQueuableObject(i));
          if (i % 10000 == 0) {
            logger.info("Correct version records added: {} , still in the queue {}", i, versionedConsumer.count(ALL));
          }
        }

        final long start = versionedConsumer.count(ALL);
        try {
          Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> {
            final long count = versionedConsumer.count(ALL);
            // logger.info("Intermittent queue count: {}", count);
            return count == 0;
          });
        } catch (Exception ignore) {
          // do nothing
        }

        final long diff = start - versionedConsumer.count(ALL);
        logger.info("Items handled for 10s: {}", diff);
      }
    })
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = GEORGE)
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
          unversionedPublisher.send(queuableObject);

          if (i % 10000 == 0) {
            logger.info("Previous version records added: {}, still in queue {}", i, unversionedConsumer.count(ALL));
          }
        }
        for (int i = 1; i <= COUNT; ++i) {
          unversionedPublisher.send(new TestUnversionedQueuableObject(i));
          if (i % 10000 == 0) {
            logger.info("Correct version records added: {} , still in the queue {}", i, unversionedConsumer.count(ALL));
          }
        }

        final long start = unversionedConsumer.count(ALL);
        try {
          Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> {
            final long count = unversionedConsumer.count(ALL);
            // logger.info("Intermittent queue count: {}", count);
            return count == 0;
          });
        } catch (Exception ignore) {
          // do nothing
        }

        final long diff = start - unversionedConsumer.count(ALL);
        logger.info("Items handled for 10s: {}", diff);
      }
    })
        .doesNotThrowAnyException();
  }
}
