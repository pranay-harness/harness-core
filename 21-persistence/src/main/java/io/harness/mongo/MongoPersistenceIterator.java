package io.harness.mongo;

import static io.harness.iterator.PersistenceIterator.ProcessMode.PUMP;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentIterable;
import lombok.Builder;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Builder
public class MongoPersistenceIterator<T extends PersistentIterable> implements PersistenceIterator<T> {
  private static final Logger logger = LoggerFactory.getLogger(MongoPersistenceIterator.class);
  private static final FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().returnNew(false);
  private static final Duration QUERY_TIME = ofMillis(200);

  @Inject private final HPersistence persistence;

  public interface Handler<T> { void handle(T entity); }

  private Class<T> clazz;
  private String fieldName;
  private Duration targetInterval;
  private Duration maximumDaleyForCheck;
  private Duration acceptableDelay;
  private Handler<T> handler;
  private ExecutorService executorService;
  private Semaphore semaphore;
  private boolean redistribute;

  private long movingAvg(long current, long sample) {
    return (15 * current + sample) / 16;
  }

  @Override
  @SuppressWarnings("PMD")
  public void process(ProcessMode mode) {
    long movingAverage = 0;
    long previous = 0;
    while (true) {
      try {
        // make sure we did not hit the limit
        semaphore.acquire();

        long base = currentTimeMillis();
        if (redistribute && previous != 0) {
          base = movingAvg(previous + movingAverage, base);
          movingAverage = movingAvg(movingAverage, base - previous);
        }

        previous = base;

        T entity = null;
        try {
          entity = next(base);
        } finally {
          semaphore.release();
        }

        if (entity != null) {
          // Make sure that if the object is updated we reset the scheduler for it
          entity.updateNextIteration(fieldName, null);

          T finalEntity = entity;
          synchronized (finalEntity) {
            executorService.submit(() -> processEntity(finalEntity));
            // it might take some time until the submitted task is actually triggered.
            // lets wait for awhile until for this to happen
            finalEntity.wait(10000);
          }
          continue;
        }

        if (mode == PUMP) {
          break;
        }

        final T first = persistence.createQuery(clazz).order(Sort.ascending(fieldName)).project(fieldName, true).get();

        Duration sleepInterval = maximumDaleyForCheck == null ? targetInterval : maximumDaleyForCheck;

        if (first != null && first.obtainNextIteration(fieldName) != null) {
          final Duration nextEntity = ofMillis(Math.max(0, first.obtainNextIteration(fieldName) - currentTimeMillis()));
          if (nextEntity.compareTo(maximumDaleyForCheck) < 0) {
            sleepInterval = nextEntity;
          }
        }
        Thread.sleep(sleepInterval.toMillis());
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        break;
      } catch (Throwable exception) {
        logger.error("Exception occurred while processing iterator", exception);
        sleep(ofSeconds(1));
      }
    }
  }

  public T next(long base) {
    final Query<T> query = persistence.createQuery(clazz).order(Sort.ascending(fieldName));
    query.or(query.criteria(fieldName).lessThan(currentTimeMillis()), query.criteria(fieldName).doesNotExist());

    final UpdateOperations<T> updateOperations =
        persistence.createUpdateOperations(clazz).set(fieldName, base + targetInterval.toMillis());

    return persistence.findAndModifySystemData(query, updateOperations, findAndModifyOptions);
  }

  void processEntity(T entity) {
    try {
      synchronized (entity) {
        semaphore.acquire();
        entity.notify();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    long delay =
        entity.obtainNextIteration(fieldName) == null ? 0 : currentTimeMillis() - entity.obtainNextIteration(fieldName);
    if (delay < acceptableDelay.toMillis()) {
      logger.info("Working on entity {}.{} with delay {}", clazz.getCanonicalName(), entity.getUuid(), delay);
    } else {
      logger.error("Entity {}.{} was delayed {} which is more than the acceptable {}", clazz.getCanonicalName(),
          entity.getUuid(), delay, acceptableDelay.toMillis());
    }

    try {
      handler.handle(entity);
    } finally {
      semaphore.release();
    }
  }
}
