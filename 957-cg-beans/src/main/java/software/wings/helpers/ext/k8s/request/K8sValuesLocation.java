package software.wings.helpers.ext.k8s.request;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP) public enum K8sValuesLocation { Environment, EnvironmentGlobal, Service, ServiceOverride }
