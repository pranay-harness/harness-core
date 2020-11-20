package io.harness.iterator;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.iterator.PersistenceIterator.ProcessMode.LOOP;
import static io.harness.iterator.PersistenceIterator.ProcessMode.PUMP;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTestBase;
import io.harness.category.element.StressTests;
import io.harness.category.element.UnitTests;
import io.harness.iterator.TestRegularIterableEntity.RegularIterableEntityKeys;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.rule.Owner;
import io.harness.threading.Morpheus;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PersistenceRegularIteratorTest extends PersistenceTestBase {
  @Inject private HPersistence persistence;
  @Inject private MorphiaPersistenceProvider<TestRegularIterableEntity> persistenceProvider;
  @Inject private QueueController queueController;
  private ExecutorService executorService = ThreadPool.create(4, 15, 1, TimeUnit.SECONDS);

  static class TestHandler implements Handler<TestRegularIterableEntity> {
    @Override
    public void handle(TestRegularIterableEntity entity) {
      Morpheus.sleep(ofSeconds(1));
      log.info("Handle {}", entity.getUuid());
    }
  }

  public MongoPersistenceIterator<TestRegularIterableEntity, MorphiaFilterExpander<TestRegularIterableEntity>> iterator(
      PersistenceIterator.ProcessMode mode) {
    MongoPersistenceIterator<TestRegularIterableEntity, MorphiaFilterExpander<TestRegularIterableEntity>> iterator =
        MongoPersistenceIterator.<TestRegularIterableEntity, MorphiaFilterExpander<TestRegularIterableEntity>>builder()
            .mode(mode)
            .clazz(TestRegularIterableEntity.class)
            .fieldName(RegularIterableEntityKeys.nextIteration)
            .targetInterval(ofSeconds(10))
            .acceptableNoAlertDelay(ofSeconds(1))
            .maximumDelayForCheck(ofSeconds(5))
            .acceptableExecutionTime(ofMillis(10))
            .executorService(executorService)
            .semaphore(new Semaphore(10))
            .handler(new TestHandler())
            .schedulingType(REGULAR)
            .redistribute(true)
            .persistenceProvider(persistenceProvider)
            .build();
    on(iterator).set("queueController", queueController);
    return iterator;
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPumpWithEmptyCollection() {
    PersistenceIterator<TestRegularIterableEntity> pumpIterator = iterator(PUMP);
    assertThatCode(() -> { pumpIterator.process(); }).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testProcessEntity() {
    MongoPersistenceIterator<TestRegularIterableEntity, MorphiaFilterExpander<TestRegularIterableEntity>> pumpIterator =
        iterator(PUMP);

    TestRegularIterableEntity entity =
        TestRegularIterableEntity.builder().uuid(generateUuid()).nextIteration(currentTimeMillis() + 1000).build();

    assertThatCode(() -> { pumpIterator.processEntity(entity); }).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testLoopWithEmptyCollection() throws IOException {
    PersistenceIterator<TestRegularIterableEntity> loopIterator = iterator(LOOP);

    assertThatCode(() -> {
      Future<?> future1 = executorService.submit(() -> loopIterator.process());
      Morpheus.sleep(ofMillis(300));
      future1.cancel(true);
    })
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(StressTests.class)
  public void testPumpWakeup() throws IOException {
    testWakeup(iterator(PUMP));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(StressTests.class)
  public void testLoopWakeup() throws IOException {
    testWakeup(iterator(LOOP));
  }

  private void testWakeup(PersistenceIterator<TestRegularIterableEntity> iterator) {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      TestRegularIterableEntity entity =
          TestRegularIterableEntity.builder().uuid(generateUuid()).nextIteration(currentTimeMillis() + 1000).build();
      persistence.save(entity);

      Future<?> future1 = executorService.submit(() -> iterator.process());

      Morpheus.sleep(ofSeconds(2));

      iterator.wakeup();

      Morpheus.sleep(ofSeconds(1));
      future1.cancel(true);

      TestRegularIterableEntity updatedEntity = persistence.get(TestRegularIterableEntity.class, entity.getUuid());

      assertThat(updatedEntity.getNextIteration()).isGreaterThan(entity.getNextIteration());
    }
  }
}
