package io.harness.queue;

import static io.harness.queue.Queue.Filter.ALL;
import static java.time.Duration.ofMillis;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.mongo.MongoQueue;
import io.harness.persistence.HPersistence;
import io.harness.rule.BypassRuleMixin.Bypass;
import io.harness.rule.RealMongo;
import io.harness.threading.Puller;
import io.harness.version.VersionInfoManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;

@Slf4j
public class StressTest extends PersistenceTest {
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  private MongoQueue<TestQueuableObject> queue;

  @Before
  public void setup() throws UnknownHostException {
    queue = new MongoQueue<>(TestQueuableObject.class);
    on(queue).set("persistence", persistence);
    on(queue).set("versionInfoManager", versionInfoManager);
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  @Bypass
  public void shouldGetWithNegativeWait() throws IOException {
    persistence.ensureIndex(TestQueuableObject.class);

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      for (int i = 0; i < 10000; ++i) {
        queue.send(new TestQueuableObject(i));
        if (i % 100 == 0) {
          logger.info("{} , {}", i, queue.count(ALL));
        }
      }

      final long start = queue.count(ALL);
      try {
        Puller.pullFor(Duration.ofSeconds(10), ofMillis(100), () -> {
          final long count = queue.count(ALL);
          logger.info("Queue count: {}", count);
          return count == 0;
        });
      } catch (Exception ignore) {
        // do nothing
      }

      final long diff = start - queue.count(ALL);
      logger.info("Items handled for 10s: {}", diff);
    }
  }
}
