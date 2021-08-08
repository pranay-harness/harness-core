package io.harness.delegate.task.k8s;

import java.util.LinkedHashSet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sDeploymentReleaseData {
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  LinkedHashSet<String> namespaces;
  String releaseName;
}
