package io.harness.annotations;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ChangeDataCaptures.class)
@OwnedBy(HarnessTeam.CE)
public @interface ChangeDataCapture {
  String table();
  String dataStore() default "harness";
  ChangeDataCaptureSink sink() default ChangeDataCaptureSink.TIMESCALE;
  String[] fields();
  String handler();
}