package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.iterator.PersistenceIterator.ProcessMode.PUMP;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistentIrregularIterable;
import io.harness.iterator.PersistentIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.maintenance.MaintenanceController;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Builder
@Slf4j
public class MongoPersistenceIterator<T extends PersistentIterable> implements PersistenceIterator<T> {
  private static final Duration QUERY_TIME = ofMillis(200);

  @Inject private final HPersistence persistence;
  @Inject private final QueueController queueController;

  public interface Handler<T> { void handle(T entity); }
  public interface FilterExpander<T> { void filter(Query<T> query); }

  private Class<T> clazz;
  private String fieldName;
  private Duration targetInterval;
  private Duration maximumDelayForCheck;
  private Duration acceptableDelay;
  private Handler<T> handler;
  private FilterExpander<T> filterExpander;
  private ExecutorService executorService;
  private Semaphore semaphore;
  private boolean redistribute;
  private boolean regular;

  private long movingAvg(long current, long sample) {
    return (15 * current + sample) / 16;
  }

  private boolean shouldProcess() {
    return !MaintenanceController.isMaintenance() && queueController.isPrimary();
  }

  @Override
  public synchronized void wakeup() {
    notify();
  }

  @Override
  @SuppressWarnings("PMD")
  public void process(ProcessMode mode) {
    long movingAverage = 0;
    long previous = 0;
    while (true) {
      if (!shouldProcess()) {
        if (mode == PUMP) {
          return;
        }
        sleep(ofSeconds(1));
        continue;
      }
      try {
        // make sure we did not hit the limit
        semaphore.acquire();

        long base = currentTimeMillis();
        // redistribution make sense only for regular iteration
        if (redistribute && regular && previous != 0) {
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
          if (!regular) {
            final Long nextIteration = entity.obtainNextIteration(fieldName);

            final List<Long> nextIterations =
                ((PersistentIrregularIterable) entity).recalculateNextIterations(fieldName);
            if (isNotEmpty(nextIterations)) {
              final UpdateOperations<T> operations =
                  persistence.createUpdateOperations(clazz).set(fieldName, nextIterations);
              persistence.update(entity, operations);
            }

            if (nextIteration == null) {
              continue;
            }
          }

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

        final Query<T> query = persistence.createQuery(clazz).order(Sort.ascending(fieldName)).project(fieldName, true);
        if (filterExpander != null) {
          filterExpander.filter(query);
        }

        final T first = query.get();

        Duration sleepInterval = maximumDelayForCheck == null ? targetInterval : maximumDelayForCheck;

        if (first != null) {
          final Long nextIteration = first.obtainNextIteration(fieldName);
          if (nextIteration == null) {
            continue;
          }
          final Duration nextEntity = ofMillis(Math.max(0, nextIteration - currentTimeMillis()));
          if (nextEntity.compareTo(maximumDelayForCheck) < 0) {
            sleepInterval = nextEntity;
          }
        }
        synchronized (this) {
          wait(sleepInterval.toMillis());
        }
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
    if (filterExpander != null) {
      filterExpander.filter(query);
    }

    UpdateOperations<T> updateOperations = regular
        ? persistence.createUpdateOperations(clazz).set(fieldName, base + targetInterval.toMillis())
        : persistence.createUpdateOperations(clazz).removeFirst(fieldName);

    return persistence.findAndModifySystemData(query, updateOperations, HPersistence.returnOldOptions);
  }

  void processEntity(T entity) {
    try {
      synchronized (entity) {
        semaphore.acquire();
        entity.notify();
      }
    } catch (InterruptedException e) {
      logger.info("Working on entity {}.{} was interrupted", clazz.getCanonicalName(), entity.getUuid());
      Thread.currentThread().interrupt();
      return;
    }

    final Long nextIteration = entity.obtainNextIteration(fieldName);
    if (regular) {
      ((PersistentRegularIterable) entity).updateNextIteration(fieldName, null);
    }

    long delay = nextIteration == null ? 0 : currentTimeMillis() - nextIteration;
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
