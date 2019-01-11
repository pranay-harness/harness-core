package io.harness.limits.checker;

import io.harness.limits.Counter;
import io.harness.limits.lib.StaticLimit;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

@AllArgsConstructor
public class StaticLimitVicinityCheckerMongoImpl implements StaticLimitVicinityChecker {
  private static final Logger log = LoggerFactory.getLogger(StaticLimitVicinityCheckerMongoImpl.class);

  private final StaticLimit limit;
  private final String key;
  private final WingsPersistence persistence;

  @Override
  public boolean hasCrossedPercentLimit(int percentage) {
    Counter counter = persistence.createQuery(Counter.class).field("key").equal(key).get();
    long used = counter.getValue();
    long allowed = limit.getCount();

    boolean crossed = used > (percentage / 100.0) * allowed;
    log.info("Static Limit Check. Used: {} , Allowed: {}, Percent: {}, Crossed Percent Limit?: {}", used, allowed,
        percentage, crossed);

    return crossed;
  }

  @Override
  public io.harness.limits.impl.model.StaticLimit getLimit() {
    return io.harness.limits.impl.model.StaticLimit.copy(limit);
  }
}
