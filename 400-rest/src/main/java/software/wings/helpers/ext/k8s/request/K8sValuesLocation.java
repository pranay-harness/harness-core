package software.wings.helpers.ext.k8s.request;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public enum K8sValuesLocation {
  Environment,
  EnvironmentGlobal,
  Service,
  ServiceOverride
}
