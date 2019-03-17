package io.harness.waiter;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.rule.BypassRuleMixin.Bypass;
import io.harness.rule.RealMongo;
import io.harness.threading.Concurrent;
import io.harness.threading.Morpheus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StressTest extends OrchestrationTest {
  private static final Logger logger = LoggerFactory.getLogger(StressTest.class);

  @Inject private HPersistence persistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Test
  @Category(UnitTests.class)
  @RealMongo
  @Bypass
  public void stress() throws IOException {
    persistence.ensureIndex(NotifyEvent.class);
    persistence.ensureIndex(WaitQueue.class);
    persistence.ensureIndex(WaitInstance.class);
    persistence.ensureIndex(NotifyResponse.class);

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      Concurrent.test(1, n -> {
        final Random random = new Random();
        int i = 1;

        List<String> vector = new ArrayList<>();

        long time = 0;
        while (i < 10000) {
          final int ids = random.nextInt(5) + 1;
          if (i / 100 != (i + ids) / 100) {
            final long waits =
                persistence.createQuery(WaitInstance.class).filter("status", ExecutionStatus.NEW).count();
            long waitQueues = persistence.createQuery(WaitQueue.class).count();
            long notifyQueues = persistence.createQuery(NotifyEvent.class).count();
            logger.info("{}: i = {}, avg: {}, waits: {}, queues: {}, events: {}", n, (i / 100 + 1) * 100, time / i,
                waits, waitQueues, notifyQueues);
          }
          i += ids;
          final String[] correlationIds = new String[ids];
          for (int id = 0; id < ids; id++) {
            final String uuid = generateUuid();
            correlationIds[id] = uuid;
            vector.add(uuid);
          }
          waitNotifyEngine.waitForAll(null, correlationIds);

          while (vector.size() > 0) {
            int index = random.nextInt(vector.size());
            time -= System.currentTimeMillis();
            waitNotifyEngine.notify(vector.get(index), null);
            time += System.currentTimeMillis();

            final int last = vector.size() - 1;
            vector.set(index, vector.get(last));
            vector.remove(last);
          }
        }
      });

      while (true) {
        final long waits = persistence.createQuery(WaitInstance.class).filter("status", ExecutionStatus.NEW).count();
        long waitQueues = persistence.createQuery(WaitQueue.class).count();
        long notifyQueues = persistence.createQuery(NotifyEvent.class).count();
        logger.info("waits: {}, queues: {}, events: {}", waits, waitQueues, notifyQueues);

        if (notifyQueues == 0) {
          break;
        }
        Morpheus.sleep(Duration.ofSeconds(1));
      }
    }
  }
}
