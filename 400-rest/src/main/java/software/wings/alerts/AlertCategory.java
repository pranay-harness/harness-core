package software.wings.alerts;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._480_ALERT_BEANS)
public enum AlertCategory {
  All,
  Setup,
  Approval,
  ManualIntervention,
  ContinuousVerification
}
