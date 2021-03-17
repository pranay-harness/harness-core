package software.wings.alerts;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

@TargetModule(Module._955_ALERT_BEANS)
public enum AlertCategory {
  All,
  Setup,
  Approval,
  ManualIntervention,
  ContinuousVerification
}
