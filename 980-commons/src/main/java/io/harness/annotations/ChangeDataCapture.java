package io.harness.changestreamsframework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeDataCapture {
  String table();
  String dataStore() default "harness";
  ChangeDataCaptureSink[] sink() default {ChangeDataCaptureSink.TIMESCALE};
  String[] fields();
}