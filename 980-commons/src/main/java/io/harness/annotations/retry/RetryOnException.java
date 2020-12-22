package io.harness.annotations.retry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface RetryOnException {
  int retryCount() default 1;

  long sleepDurationInMilliseconds() default 10L;

  Class<? extends Throwable>[] retryOn() default {Exception.class};
}
