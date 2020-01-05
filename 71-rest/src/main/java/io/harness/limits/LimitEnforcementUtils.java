package io.harness.limits;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.limits.checker.UsageLimitExceededException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.StaticLimitActionTypeLogContext;

import java.util.function.Supplier;

@UtilityClass
public class LimitEnforcementUtils {
  private static final Logger log = LoggerFactory.getLogger(LimitEnforcementUtils.class);

  /**
   * execute the given function while enforcing usage limits.
   * Consumes a permit if the function is successful, decrements in case any error is thrown in function execution.
   *
   * @param checker limit checker instance
   * @param fn function to execute. Usually this will be something that creates a resource. (example: create an
   * application)
   * @param <T> return type of function (example: {@link software.wings.beans.Application})
   */
  public static <T> T withLimitCheck(StaticLimitCheckerWithDecrement checker, Supplier<T> fn) {
    try (AutoLogContext ignore1 = new AccountLogContext(checker.getAction().getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 =
             new StaticLimitActionTypeLogContext(checker.getAction().getActionType().name(), OVERRIDE_ERROR)) {
      boolean allowed = checker.checkAndConsume();
      if (!allowed) {
        log.info("Resource Usage Limit Reached. Limit: {}", checker.getLimit());
        throw new UsageLimitExceededException(checker.getLimit(), checker.getAction().getAccountId());
      }

      try {
        return fn.get();
      } catch (Exception e) {
        checker.decrement();
        throw e;
      }
    }
  }

  /**
   * executes a function while decrementing the counter.
   * @param checker limit checker instance
   * @param fn fn to execute. This will typically delete a resource (example: delete an application)
   */
  public static void withCounterDecrement(StaticLimitCheckerWithDecrement checker, Runnable fn) {
    fn.run();
    checker.decrement();
  }

  /**
   * executes a function while decrementing the counter.
   * @param checker limit checker instance
   * @param fn fn to execute. This will typically delete a resource (example: delete an application)
   * @param <T> return type of fn execution
   */
  public static <T> T withCounterDecrement(StaticLimitCheckerWithDecrement checker, Supplier<T> fn) {
    T value = fn.get();
    checker.decrement();
    return value;
  }
}
