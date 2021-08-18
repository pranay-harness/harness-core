package software.wings.helpers.ext.k8s.request;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum K8sValuesLocation {
  Environment,
  EnvironmentGlobal,
  Service,
  ServiceOverride
}
