package io.harness.annotation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@OwnedBy(HarnessTeam.PIPELINE)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RecasterAlias {
  String value();
}
