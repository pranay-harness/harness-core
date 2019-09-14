package io.harness.iterator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.iterator.PersistenceIterator.ProcessMode.LOOP;
import static io.harness.iterator.PersistenceIterator.ProcessMode.PUMP;
import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.iterator.RegularIterableEntity.RegularIterableEntityKeys;
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
public class PersistenceRegularIteratorTest extends PersistenceTest {
  @Inject private HPersistence persistence;
  @Inject private QueueController queueController;
  private ExecutorService executorService = ThreadPool.create(4, 15, 1, TimeUnit.SECONDS);

  PersistenceIterator<RegularIterableEntity> iterator;

  class TestHandler implements Handler<RegularIterableEntity> {
    @Override
    public void handle(RegularIterableEntity entity) {
      Morpheus.sleep(ofSeconds(1));
      logger.info("Handle {}", entity.getUuid());
    }
  }

  @Before
  public void setup() {
    iterator = MongoPersistenceIterator.<RegularIterableEntity>builder()
                   .clazz(RegularIterableEntity.class)
                   .fieldName(RegularIterableEntityKeys.nextIteration)
                   .targetInterval(ofSeconds(10))
                   .acceptableNoAlertDelay(ofSeconds(1))
                   .maximumDelayForCheck(ofSeconds(5))
                   .executorService(executorService)
                   .semaphore(new Semaphore(10))
                   .handler(new TestHandler())
                   .schedulingType(REGULAR)
                   .redistribute(true)
                   .build();
    on(iterator).set("persistence", persistence);
    on(iterator).set("queueController", queueController);
  }

  @Test
  @Category(UnitTests.class)
  public void testPumpWithEmptyCollection() {
    iterator.process(PUMP);
  }

  @Test
  @Category(UnitTests.class)
  public void testLoopWithEmptyCollection() throws IOException {
    final Future<?> future1 = executorService.submit(() -> iterator.process(LOOP));
    Morpheus.sleep(ofMillis(300));
    future1.cancel(true);
  }

  @Test
  @Category(UnitTests.class)
  @Bypass
  public void testWakeup() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      final RegularIterableEntity entity =
          RegularIterableEntity.builder().uuid(generateUuid()).nextIteration(currentTimeMillis() + 1000).build();
      persistence.save(entity);

      final Future<?> future1 = executorService.submit(() -> iterator.process(LOOP));

      Morpheus.sleep(ofSeconds(2));

      iterator.wakeup();

      Morpheus.sleep(ofSeconds(1));
      future1.cancel(true);

      final RegularIterableEntity updatedEntity = persistence.get(RegularIterableEntity.class, entity.getUuid());

      assertThat(updatedEntity.getNextIteration()).isGreaterThan(entity.getNextIteration());
    }
  }

  @Test
  @Category(UnitTests.class)
  @Bypass
  public void testNextReturnsJustAdded() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      for (int i = 0; i < 10; i++) {
        persistence.save(RegularIterableEntity.builder().build());
      }

      final Future<?> future1 = executorService.submit(() -> iterator.process(LOOP));
      //    final Future<?> future2 = executorService.submit(() -> iterator.process());
      //    final Future<?> future3 = executorService.submit(() -> iterator.process());

      Morpheus.sleep(ofSeconds(300));
      future1.cancel(true);
      //    future2.cancel(true);
      //    future3.cancel(true);
    }
  }
}