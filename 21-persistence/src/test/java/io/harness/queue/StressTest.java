package io.harness.queue;

import static io.harness.queue.QueueConsumer.Filter.ALL;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.StressTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import io.harness.threading.Poller;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.time.Duration;

@Slf4j
public class StressTest extends PersistenceTest {
  private static final int COUNT = 1000000;
  @Inject private HPersistence persistence;
  @Inject private QueueConsumer<TestTopicQueuableObject> topicConsumer;
  @Inject private QueuePublisher<TestTopicQueuableObject> topicPublisher;
  @Inject private QueueConsumer<TestNoTopicQueuableObject> noTopicConsumer;
  @Inject private QueuePublisher<TestNoTopicQueuableObject> noToipcPublisher;

  @Test
  @Owner(developers = GEORGE)
  @Category(StressTests.class)
  @RealMongo
  public void topicPerformance() throws IOException {
    assertThatCode(() -> {
      persistence.ensureIndex(TestTopicQueuableObject.class);

      try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
        for (int i = 1; i <= COUNT; ++i) {
          final TestTopicQueuableObject queuableObject = new TestTopicQueuableObject(i);
          queuableObject.setTopic("dummy");

          persistence.save(queuableObject);

          if (i % 10000 == 0) {
            logger.info("Previous topic records added: {}, still in queue {}", i, topicConsumer.count(ALL));
          }
        }
        for (int i = 1; i <= COUNT; ++i) {
          topicPublisher.send(new TestTopicQueuableObject(i));
          if (i % 10000 == 0) {
            logger.info("Correct topic records added: {} , still in the queue {}", i, topicConsumer.count(ALL));
          }
        }

        final long start = topicConsumer.count(ALL);
        try {
          Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> {
            final long count = topicConsumer.count(ALL);
            // logger.info("Intermittent queue count: {}", count);
            return count == 0;
          });
        } catch (Exception ignore) {
          // do nothing
        }

        final long diff = start - topicConsumer.count(ALL);
        logger.info("Items handled for 10s: {}", diff);
      }
    })
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(StressTests.class)
  @RealMongo
  public void noTopicPerformance() throws IOException {
    assertThatCode(() -> {
      persistence.ensureIndex(TestNoTopicQueuableObject.class);

      try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
        for (int i = 1; i <= COUNT; ++i) {
          final TestNoTopicQueuableObject queuableObject = new TestNoTopicQueuableObject(i);
          queuableObject.setTopic("dummy");
          noToipcPublisher.send(queuableObject);

          if (i % 10000 == 0) {
            logger.info("Previous records added: {}, still in queue {}", i, noTopicConsumer.count(ALL));
          }
        }
        for (int i = 1; i <= COUNT; ++i) {
          noToipcPublisher.send(new TestNoTopicQueuableObject(i));
          if (i % 10000 == 0) {
            logger.info("Correct records added: {} , still in the queue {}", i, noTopicConsumer.count(ALL));
          }
        }

        final long start = noTopicConsumer.count(ALL);
        try {
          Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> {
            final long count = noTopicConsumer.count(ALL);
            // logger.info("Intermittent queue count: {}", count);
            return count == 0;
          });
        } catch (Exception ignore) {
          // do nothing
        }

        final long diff = start - noTopicConsumer.count(ALL);
        logger.info("Items handled for 10s: {}", diff);
      }
    })
        .doesNotThrowAnyException();
  }
}
