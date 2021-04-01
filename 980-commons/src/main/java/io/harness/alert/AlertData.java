package io.harness.alert;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._955_ALERT_BEANS)
public interface AlertData {
  boolean matches(AlertData alertData);

  String buildTitle();

  default String buildResolutionTitle() {
    return null;
  }
}
