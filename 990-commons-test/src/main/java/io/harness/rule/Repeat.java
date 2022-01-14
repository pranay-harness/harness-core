package io.harness.rule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD})
@Deprecated
public @interface Repeat {
  int times();
  int successes() default - 1; // default value -1 gets converted to times() value.
}
