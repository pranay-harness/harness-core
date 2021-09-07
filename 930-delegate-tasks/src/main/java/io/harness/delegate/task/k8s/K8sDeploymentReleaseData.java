package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.LinkedHashSet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class K8sDeploymentReleaseData {
  private K8sInfraDelegateConfig k8sInfraDelegateConfig;
  private LinkedHashSet<String> namespaces;
  private String releaseName;
}
