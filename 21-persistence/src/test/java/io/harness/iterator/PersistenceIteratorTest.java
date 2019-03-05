package io.harness.iterator;

import static io.harness.iterator.PersistenceIterator.ProcessMode.LOOP;
import static io.harness.iterator.PersistenceIterator.ProcessMode.PUMP;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.rule.BypassRuleMixin.Bypass;
import io.harness.threading.Morpheus;
import io.harness.threading.ThreadPool;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PersistenceIteratorTest extends PersistenceTest {
  private static final Logger logger = LoggerFactory.getLogger(PersistenceIteratorTest.class);

  @Inject private HPersistence persistence;
  @Inject private QueueController queueController;
  private ExecutorService executorService = ThreadPool.create(4, 15, 1, TimeUnit.SECONDS);

  PersistenceIterator<IterableEntity> iterator;

  class TestHandler implements Handler<IterableEntity> {
    @Override
    public void handle(IterableEntity entity) {
      Morpheus.sleep(ofSeconds(1));
      logger.info("Handle {}", entity.getUuid());
    }
  }

  @Before
  public void setup() {
    iterator = MongoPersistenceIterator.<IterableEntity>builder()
                   .clazz(IterableEntity.class)
                   .fieldName(IterableEntity.NEXT_ITERATION_KEY)
                   .targetInterval(ofSeconds(10))
                   .acceptableDelay(ofSeconds(1))
                   .executorService(executorService)
                   .semaphore(new Semaphore(10))
                   .handler(new TestHandler())
                   .redistribute(true)
                   .build();
    on(iterator).set("persistence", persistence);
    on(iterator).set("queueController", queueController);
  }

  @Test
  public void testPumpWithEmptyCollection() {
    iterator.process(PUMP);
  }

  @Test
  public void testLoopWithEmptyCollection() throws IOException {
    final Future<?> future1 = executorService.submit(() -> iterator.process(LOOP));
    Morpheus.sleep(ofMillis(300));
    future1.cancel(true);
  }

  @Test
  @Bypass
  public void testNextReturnsJustAdded() {
    for (int i = 0; i < 10; i++) {
      persistence.save(IterableEntity.builder().build());
    }

    final Future<?> future1 = executorService.submit(() -> iterator.process(LOOP));
    //    final Future<?> future2 = executorService.submit(() -> iterator.process());
    //    final Future<?> future3 = executorService.submit(() -> iterator.process());

    Morpheus.sleep(ofSeconds(3));
    future1.cancel(true);
    //    future2.cancel(true);
    //    future3.cancel(true);
  }
}
