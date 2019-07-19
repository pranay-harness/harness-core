package io.harness.iterator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.iterator.PersistenceIterator.ProcessMode.LOOP;
import static java.time.Duration.ofSeconds;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.iterator.CronIterableEntity.CronIterableEntityKeys;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.rule.BypassRuleMixin.Bypass;
import io.harness.threading.Morpheus;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PersistenceCronIteratorTest extends PersistenceTest {
  @Inject private HPersistence persistence;
  @Inject private QueueController queueController;
  private ExecutorService executorService = ThreadPool.create(4, 15, 1, TimeUnit.SECONDS);

  PersistenceIterator<CronIterableEntity> iterator;

  class TestHandler implements Handler<CronIterableEntity> {
    @Override
    public void handle(CronIterableEntity entity) {
      Morpheus.sleep(ofSeconds(1));
      logger.info("Handle {}", entity.getUuid());
    }
  }

  @Before
  public void setup() {
    iterator = MongoPersistenceIterator.<CronIterableEntity>builder()
                   .clazz(CronIterableEntity.class)
                   .fieldName(CronIterableEntityKeys.nextIterations)
                   .targetInterval(ofSeconds(10))
                   .acceptableDelay(ofSeconds(1))
                   .maximumDelayForCheck(ofSeconds(1))
                   .executorService(executorService)
                   .semaphore(new Semaphore(10))
                   .handler(new TestHandler())
                   .regular(false)
                   .redistribute(true)
                   .build();
    on(iterator).set("persistence", persistence);
    on(iterator).set("queueController", queueController);
  }

  @Test
  @Category(UnitTests.class)
  @Bypass
  public void testNextReturnsJustAdded() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      for (int i = 1; i <= 10; i++) {
        final CronIterableEntity iterableEntity =
            CronIterableEntity.builder().uuid(generateUuid()).expression(String.format("*/%d * * * * ? *", i)).build();
        persistence.save(iterableEntity);
      }

      final Future<?> future1 = executorService.submit(() -> iterator.process(LOOP));

      Morpheus.sleep(ofSeconds(300));
      future1.cancel(true);
    }
  }
}