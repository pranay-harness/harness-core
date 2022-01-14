package io.harness.entities.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.k8s.model.HelmVersion;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NativeHelmInstanceInfo extends InstanceInfo {
  @NotNull private String podName;
  private String ip;
  private String namespace;
  private String releaseName;
  private HelmChartInfo helmChartInfo;
  private HelmVersion helmVersion;
}
