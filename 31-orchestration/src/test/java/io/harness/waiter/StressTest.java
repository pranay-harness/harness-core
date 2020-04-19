package io.harness.waiter;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.waiter.TestNotifyEventListener.TEST_PUBLISHER;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.StressTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import io.harness.threading.Concurrent;
import io.harness.threading.Morpheus;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StressTest extends OrchestrationTest {
  private static final SecureRandom random = new SecureRandom();

  @Inject private HPersistence persistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NotifyResponseCleaner notifyResponseCleaner;

  @Test
  @Owner(developers = GEORGE)
  @Category(StressTests.class)
  @RealMongo
  public void stress() throws IOException {
    persistence.ensureIndexForTesting(NotifyEvent.class);
    persistence.ensureIndexForTesting(WaitInstance.class);
    persistence.ensureIndexForTesting(NotifyResponse.class);

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      Concurrent.test(1, n -> {
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
          waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, null, correlationIds);

          while (vector.size() > 0) {
            int index = random.nextInt(vector.size());
            time -= System.currentTimeMillis();
            waitNotifyEngine.doneWith(vector.get(index), null);
            time += System.currentTimeMillis();

            final int last = vector.size() - 1;
            vector.set(index, vector.get(last));
            vector.remove(last);
          }
        }
      });

      while (true) {
        notifyResponseCleaner.execute();
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
