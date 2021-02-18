package io.harness.alert;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._480_ALERT_BEANS)
public interface AlertData {
  boolean matches(AlertData alertData);

  String buildTitle();

  default String buildResolutionTitle() {
    return null;
  }
}
