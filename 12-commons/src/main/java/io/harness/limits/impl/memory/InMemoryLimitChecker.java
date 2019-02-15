package io.harness.limits.impl.memory;

import io.harness.limits.lib.StaticLimit;
import io.harness.limits.lib.StaticLimitChecker;
import lombok.Getter;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * In memory implementation of a Static Limit Checker.
 * By In memory, it means the a counter is maintained in memory.
 * <br><br>
 *
 * See <code>InMemoryLimitCheckerTest</code> for usage details
 */
@ThreadSafe
public class InMemoryLimitChecker implements StaticLimitChecker {
  private static final Logger log = LoggerFactory.getLogger(InMemoryLimitChecker.class);

  @Getter private final StaticLimit limit;
  private boolean limitExceeded;
  private final AtomicInteger counter = new AtomicInteger(0);

  public InMemoryLimitChecker(StaticLimit limit) {
    this.limit = limit;
  }

  @Override
  public boolean checkAndConsume() {
    if (this.limitExceeded) {
      return false;
    }

    int allowed = limit.getCount();

    if (counter.incrementAndGet() > allowed) {
      limitExceeded = true;
      return false;
    } else {
      return true;
    }
  }
}
