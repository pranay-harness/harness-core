package io.harness.limits;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

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
    boolean allowed = checker.checkAndConsume();
    if (!allowed) {
      log.info("Usage Limits Reached. Action: {}, Limit: {}", checker.getAction(), checker.getLimit());
      throw new WingsException(ErrorCode.USAGE_LIMITS_EXCEEDED,
          "Usage Limit Reached. Please contact Harness support to upgrade your license.");
    }

    try {
      return fn.get();
    } catch (Exception e) {
      checker.decrement();
      throw e;
    }
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
