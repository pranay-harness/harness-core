package io.harness.accesscontrol;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@OwnedBy(HarnessTeam.PL)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface NGAccessControlCheck {
  String resourceType();
  String permission();
}
