package io.harness.telemetry.annotation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@OwnedBy(HarnessTeam.GTM)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SendTrackEvents {
  SendTrackEvent[] value();
}
