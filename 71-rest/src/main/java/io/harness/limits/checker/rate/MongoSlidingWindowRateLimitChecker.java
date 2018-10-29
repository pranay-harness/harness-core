package io.harness.limits.checker.rate;

import com.mongodb.BasicDBObject;
import io.harness.limits.Action;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.lib.RateLimitChecker;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import lombok.Getter;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;

import java.time.Instant;

/**
 * Mongo backed implementation for rate limits.
 * Uses sliding window algorithm.
 *
 * You shouldn't need to directly create an instance of this, but rather use {@link
 * io.harness.limits.LimitCheckerFactory} to get a rate limiter based on action.
 *
 * See <pre>MongoRateLimitCheckerIntegrationTest</pre> for expected behaviour.
 */
public class MongoSlidingWindowRateLimitChecker implements RateLimitChecker {
  @Getter private final RateLimit limit;
  private final WingsPersistence persistence;
  private final String key;
  @Getter private final Action action;

  public MongoSlidingWindowRateLimitChecker(RateLimit limit, WingsPersistence persistence, Action action) {
    this.limit = limit;
    this.persistence = persistence;
    this.action = action;
    this.key = action.key();
  }

  @Override
  public boolean checkAndConsume() {
    long now = Instant.now().toEpochMilli();
    long lastTime = now - limit.getDurationUnit().toMillis(limit.getDuration());

    removeExpiredTimes(lastTime);
    UsageBucket updatedBucket = addNewTime(now);

    return updatedBucket.getAccessTimes().size() <= limit.getCount();
  }

  // removes access times which are past the sliding window.
  private void removeExpiredTimes(long leastAllowedTime) {
    Query<UsageBucket> query = persistence.createQuery(UsageBucket.class).field("key").equal(key);

    AdvancedDatastore ds = persistence.getDatastore(HPersistence.DEFAULT_STORE, ReadPref.NORMAL);
    UpdateOperations<UsageBucket> update = ds.createUpdateOperations(UsageBucket.class,
        new BasicDBObject("$pull", new BasicDBObject("accessTimes", new BasicDBObject("$lt", leastAllowedTime))));

    persistence.findAndModify(query, update, new FindAndModifyOptions());
  }

  private UsageBucket addNewTime(long now) {
    Query<UsageBucket> query = persistence.createQuery(UsageBucket.class).field("key").equal(key);

    UpdateOperations<UsageBucket> update =
        persistence.createUpdateOperations(UsageBucket.class).push("accessTimes", now);

    FindAndModifyOptions options = new FindAndModifyOptions();
    options.returnNew(true);
    options.upsert(true);

    return persistence.findAndModify(query, update, options);
  }
}
