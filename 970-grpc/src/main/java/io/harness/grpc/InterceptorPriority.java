package io.harness.grpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Priorities on interceptors to control the order in which they are called.
 * Are sorted in ascending order; the lower the number the higher the priority.
 * Interceptors without this annotation are treated as low priority.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface InterceptorPriority {
  int value();
}
