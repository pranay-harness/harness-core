package io.harness.mongo.iterator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.iterator.PersistenceIterator.ProcessMode.PUMP;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistentIrregularIterable;
import io.harness.iterator.PersistentIterable;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.maintenance.MaintenanceController;
import io.harness.mongo.DelayLogContext;
import io.harness.mongo.EntityLogContext;
import io.harness.mongo.ProcessTimeLogContext;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FilterOperator;
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

  public enum SchedulingType { REGULAR, IRREGULAR, IRREGULAR_SKIP_MISSED }

  private ProcessMode mode;
  private Class<T> clazz;
  private String fieldName;
  private Duration targetInterval;
  private Duration maximumDelayForCheck;
  private Duration acceptableNoAlertDelay;
  private Duration acceptableExecutionTime;
  private Duration throttleInterval;
  private Handler<T> handler;
  private FilterExpander<T> filterExpander;
  private ExecutorService executorService;
  private Semaphore semaphore;
  private boolean redistribute;
  private SchedulingType schedulingType;

  private long movingAvg(long current, long sample) {
    return (15 * current + sample) / 16;
  }

  private boolean shouldProcess() {
    return !MaintenanceController.getMaintenanceFilename() && queueController.isPrimary();
  }

  @Override
  public synchronized void wakeup() {
    switch (mode) {
      case PUMP:
        executorService.submit(this ::process);
        break;
      case LOOP:
        notifyAll();
        break;
      default:
        unhandled(mode);
    }
  }

  @Override
  // The theory is that ERROR type exception are unrecoverable, that is not exactly true.
  @SuppressWarnings({"PMD", "squid:S1181"})
  public void process() {
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
        long throttled = base + (throttleInterval == null ? 0 : throttleInterval.toMillis());
        // redistribution make sense only for regular iteration
        if (redistribute && schedulingType == REGULAR && previous != 0) {
          base = movingAvg(previous + movingAverage, base);
          movingAverage = movingAvg(movingAverage, base - previous);
        }

        previous = base;

        T entity = null;
        try {
          entity = next(base, throttled);
        } finally {
          semaphore.release();
        }

        if (entity != null) {
          // Make sure that if the object is updated we reset the scheduler for it
          if (schedulingType != REGULAR) {
            Long nextIteration = entity.obtainNextIteration(fieldName);

            List<Long> nextIterations =
                ((PersistentIrregularIterable) entity)
                    .recalculateNextIterations(fieldName, schedulingType == IRREGULAR_SKIP_MISSED, throttled);
            if (isNotEmpty(nextIterations)) {
              UpdateOperations<T> operations = persistence.createUpdateOperations(clazz).set(fieldName, nextIterations);
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

        Query<T> query = createQuery().project(fieldName, true);

        T next = query.get();

        long sleepMillis = calculateSleepDuration(next).toMillis();
        // Do not sleep with 0, it is actually infinite sleep
        if (sleepMillis > 0) {
          synchronized (this) {
            wait(sleepMillis);
          }
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

  public Duration calculateSleepDuration(T next) {
    if (next == null) {
      return maximumDelayForCheck == null ? targetInterval : maximumDelayForCheck;
    }

    Long nextIteration = next.obtainNextIteration(fieldName);
    if (nextIteration == null) {
      return ZERO;
    }

    Duration nextEntity = ofMillis(nextIteration - currentTimeMillis());
    if (maximumDelayForCheck == null || nextEntity.compareTo(maximumDelayForCheck) < 0) {
      return nextEntity;
    }

    return maximumDelayForCheck;
  }

  public T next(long base, long throttled) {
    long now = currentTimeMillis();

    Query<T> query = createQuery(now);

    UpdateOperations<T> updateOperations = persistence.createUpdateOperations(clazz);
    switch (schedulingType) {
      case REGULAR:
        updateOperations.set(fieldName, base + targetInterval.toMillis());
        break;
      case IRREGULAR:
        updateOperations.removeFirst(fieldName);
        break;
      case IRREGULAR_SKIP_MISSED:
        updateOperations.removeAll(fieldName, new BasicDBObject(FilterOperator.LESS_THAN_OR_EQUAL.val(), throttled));
        break;
      default:
        unhandled(schedulingType);
    }

    return persistence.findAndModifySystemData(query, updateOperations, HPersistence.returnOldOptions);
  }

  public Query<T> createQuery() {
    Query<T> query = persistence.createQuery(clazz).order(Sort.ascending(fieldName));
    if (filterExpander != null) {
      filterExpander.filter(query);
    }
    return query;
  }

  public Query<T> createQuery(long now) {
    Query<T> query = createQuery();
    if (filterExpander == null) {
      query.or(query.criteria(fieldName).lessThan(now), query.criteria(fieldName).doesNotExist());
    } else {
      query.and(query.or(query.criteria(fieldName).lessThan(now), query.criteria(fieldName).doesNotExist()));
    }
    return query;
  }

  // We are aware that the entity will be different object every time the method is
  // called. This is exactly what we want.
  // The theory is that ERROR type exception are unrecoverable, that is not exactly true.
  @SuppressWarnings({"squid:S2445", "PMD", "squid:S1181"})
  @VisibleForTesting
  public void processEntity(T entity) {
    try (EntityLogContext ignore = new EntityLogContext(entity, OVERRIDE_ERROR)) {
      try {
        semaphore.acquire();
      } catch (InterruptedException e) {
        logger.info("Working on entity was interrupted");
        Thread.currentThread().interrupt();
        return;
      }

      long startTime = currentTimeMillis();

      try {
        synchronized (entity) {
          entity.notify();
        }
        Long nextIteration = entity.obtainNextIteration(fieldName);
        if (schedulingType == REGULAR) {
          ((PersistentRegularIterable) entity).updateNextIteration(fieldName, null);
        }

        long delay = nextIteration == null ? 0 : startTime - nextIteration;

        try (DelayLogContext ignore2 = new DelayLogContext(delay, OVERRIDE_ERROR)) {
          if (delay < acceptableNoAlertDelay.toMillis()) {
            logger.info("Working on entity");
          } else {
            logger.error(
                "Working on entity but the delay is more than the acceptable {}", acceptableNoAlertDelay.toMillis());
          }
        }

        try {
          handler.handle(entity);
        } catch (RuntimeException exception) {
          logger.error("Catch and handle all exceptions in the entity handler", exception);
        }
      } catch (Throwable exception) {
        logger.error("Exception while processing entity", exception);
      } finally {
        semaphore.release();

        long processTime = currentTimeMillis() - startTime;
        try (ProcessTimeLogContext ignore2 = new ProcessTimeLogContext(processTime, OVERRIDE_ERROR)) {
          if (acceptableExecutionTime == null || processTime <= acceptableExecutionTime.toMillis()) {
            logger.info("Done with entity");
          } else {
            logger.error("Done with entity but took too long acceptable {}", acceptableExecutionTime.toMillis());
          }
        } catch (Throwable exception) {
          logger.error("Exception while recording the processing of entity", exception);
        }
      }
    }
  }
}