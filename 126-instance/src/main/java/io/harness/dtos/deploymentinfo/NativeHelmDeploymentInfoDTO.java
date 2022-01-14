package io.harness.dtos.deploymentinfo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.beans.ServiceSpecType;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.k8s.model.HelmVersion;
import io.harness.util.InstanceSyncKey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashSet;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class NativeHelmDeploymentInfoDTO extends DeploymentInfoDTO {
  @NotNull private LinkedHashSet<String> namespaces;
  @NotNull private String releaseName;
  private HelmChartInfo helmChartInfo;
  @NotNull private HelmVersion helmVersion;

  @Override
  public String getType() {
    return ServiceSpecType.NATIVE_HELM;
  }

  @Override
  public String prepareInstanceSyncHandlerKey() {
    return InstanceSyncKey.builder().part(releaseName).build().toString();
  }
}
