package io.harness.limits.checker;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import com.mongodb.MongoCommandException;
import io.harness.eraro.mongo.MongoError;
import io.harness.limits.Action;
import io.harness.limits.Counter;
import io.harness.limits.lib.StaticLimit;
import io.harness.limits.lib.StaticLimitChecker;
import io.harness.persistence.ReadPref;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * MongoDB backed implementation of static limit checker
 */
@ParametersAreNonnullByDefault
public class MongoStaticLimitChecker implements StaticLimitChecker {
  private static final Logger log = LoggerFactory.getLogger(MongoStaticLimitChecker.class);

  private final StaticLimit limit;
  private final WingsPersistence persistence;
  private final String key;

  public MongoStaticLimitChecker(StaticLimit limit, WingsPersistence persistence, Action action) {
    Preconditions.checkArgument(limit.getCount() >= 0, "limits can only be non-negative");
    this.persistence = persistence;
    this.limit = limit;
    this.key = action.key();
  }

  /**
   * @deprecated use {@link #MongoStaticLimitChecker(StaticLimit, WingsPersistence, Action)} instead.
   */
  @Deprecated
  @VisibleForTesting
  MongoStaticLimitChecker(StaticLimit limit, WingsPersistence persistence, String key) {
    Preconditions.checkArgument(limit.getCount() >= 0, "limits can only be non-negative");
    this.persistence = persistence;
    this.limit = limit;
    this.key = key;
  }

  @Override
  public StaticLimit getLimit() {
    return limit;
  }

  @Override
  public boolean checkAndConsume() {
    // don't touch the database if the limit is zero
    if (limit.getCount() == 0) {
      return false;
    }

    Counter counter = incrementAndGet(key, 1);
    return counter.getValue() <= limit.getCount();
  }

  /**
   * This is the opposite of {@link #checkAndConsume()}.
   * Check and consume consumes a permit, this decrements the used permit count by one.
   *
   * @return - whether a decrement was done or not. So, if there are no used permits, then there is nothing to decrement
   * and this'll return false.
   */
  public boolean decrement() {
    // don't touch the database if the limit is zero
    if (limit.getCount() == 0) {
      return false;
    }

    UpdateResponse response = decrementAndGet(key, -1);
    Counter counter = response.counter;

    if (response.changed && counter.getValue() < 0) {
      log.info("Illegal State: count should never go below zero. Resetting value to zero. Key: {}", key);
      Query<Counter> query = persistence.createQuery(Counter.class).field("key").equal(key);
      UpdateOperations<Counter> update = persistence.createUpdateOperations(Counter.class).set("value", 0);
      persistence.update(query, update);

      return false;
    }

    return response.changed;
  }

  private @Nonnull Counter incrementAndGet(String keyToIncrement, int change) {
    Preconditions.checkArgument(change > 0, "use decrementAndGet for -ve change");

    try {
      Query<Counter> q = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL)
                             .createQuery(Counter.class)
                             .field("key")
                             .equal(keyToIncrement)
                             .field("value")
                             .lessThan(limit.getCount());

      UpdateOperations<Counter> updateOp = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL)
                                               .createUpdateOperations(Counter.class)
                                               .inc("value", change);

      FindAndModifyOptions optns = new FindAndModifyOptions();
      optns.returnNew(true);
      optns.upsert(true);

      return persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).findAndModify(q, updateOp, optns);
    } catch (MongoCommandException e) {
      if (e.getErrorCode() == MongoError.DUPLICATE_KEY.getErrorCode()) {
        log.info(
            "Duplicate key exception while trying to increment counter. Can happen when counter is already at max, and it tries to upsert");
        return new Counter(key, limit.getCount() + 1);
      } else {
        throw e;
      }
    }
  }

  private UpdateResponse decrementAndGet(String keyToDecrement, int change) {
    Preconditions.checkArgument(change < 0, "use incrementAndGet for +ve change");
    Query<Counter> q = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL)
                           .createQuery(Counter.class)
                           .field("key")
                           .equal(keyToDecrement)
                           .field("value")
                           .greaterThan(0);

    UpdateOperations<Counter> updateOp = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL)
                                             .createUpdateOperations(Counter.class)
                                             .inc("value", change);

    FindAndModifyOptions optns = new FindAndModifyOptions();
    optns.returnNew(true);
    Counter counter = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).findAndModify(q, updateOp, optns);

    if (counter == null) {
      log.info("new counter is null. "
              + "Can happen when counter is at 0, or key is absent. Key should always be present. Key: {}",
          key);
      return new UpdateResponse(new Counter(key, -1), false);
    }

    return new UpdateResponse(counter, true);
  }

  @Value
  @AllArgsConstructor
  static class UpdateResponse {
    Counter counter;
    boolean changed;
  }

  public String getKey() {
    return key;
  }
}
