package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;

import java.util.LinkedHashSet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class NativeHelmDeploymentReleaseData {
  private K8sInfraDelegateConfig k8sInfraDelegateConfig;
  private LinkedHashSet<String> namespaces;
  private String releaseName;
  HelmChartInfo helmChartInfo;
}
