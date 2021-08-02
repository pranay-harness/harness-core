package io.harness.dtos.deploymentinfo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.annotations.dev.OwnedBy;
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
public class K8sDeploymentInfoDTO extends DeploymentInfoDTO {
  @NotNull private LinkedHashSet<String> namespaces;
  @NotNull private String releaseName;
  private String blueGreenStageColor;

  @Override
  public String prepareInstanceSyncHandlerKey() {
    return InstanceSyncKey
        .builder()
        // TODO @Ivan remove mandatory clazz field
        .clazz(K8sDeploymentInfoDTO.class)
        .part(join(EMPTY, this.namespaces))
        .part(this.releaseName)
        .build()
        .toString();
  }
}
