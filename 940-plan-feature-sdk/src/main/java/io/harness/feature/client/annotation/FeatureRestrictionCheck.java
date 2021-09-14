package io.harness.feature.client.annotation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.feature.constants.FeatureRestriction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@OwnedBy(HarnessTeam.GTM)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FeatureRestrictionCheck {
  FeatureRestriction value();
}
