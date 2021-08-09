package io.harness.annotations.test;

import io.harness.annotations.dev.HarnessTeam;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TestInfo {
  String[] testCaseIds();
  FeatureName featureName();
  Class<?>[] category();
  HarnessTeam ownedBy();
}
