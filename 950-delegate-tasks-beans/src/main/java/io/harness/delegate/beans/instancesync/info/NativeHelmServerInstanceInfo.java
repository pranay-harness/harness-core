package io.harness.delegate.beans.instancesync.info;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.k8s.model.HelmVersion;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonTypeName("NativeHelmServerInstanceInfo")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public class NativeHelmServerInstanceInfo extends ServerInstanceInfo {
  private String podName;
  private String ip;
  private String namespace;
  private String releaseName;
  private HelmChartInfo helmChartInfo;
  private HelmVersion helmVersion;
}
