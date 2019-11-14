package io.harness.waiter;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.rule.OwnerRule.Owner;
import io.harness.rule.RealMongo;
import io.harness.threading.Concurrent;
import io.harness.threading.Morpheus;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class StressTest extends OrchestrationTest {
  @Inject private HPersistence persistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @RealMongo
  @Ignore("Bypass this test, it is not for running regularly")
  public void stress() throws IOException {
    persistence.ensureIndex(NotifyEvent.class);
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
            final long waits = persistence.createQuery(WaitInstance.class).count();
            long notifyQueues = persistence.createQuery(NotifyEvent.class).count();
            logger.info(
                "{}: i = {}, avg: {}, waits: {}, events: {}", n, (i / 100 + 1) * 100, time / i, waits, notifyQueues);
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
        final long waits = persistence.createQuery(WaitInstance.class).count();
        long notifyQueues = persistence.createQuery(NotifyEvent.class).count();
        logger.info("waits: {}, events: {}", waits, notifyQueues);

        if (notifyQueues == 0) {
          break;
        }
        Morpheus.sleep(Duration.ofSeconds(1));
      }
    }
  }
}
