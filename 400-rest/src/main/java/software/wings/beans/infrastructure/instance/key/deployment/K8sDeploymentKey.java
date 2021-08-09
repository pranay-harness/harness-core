package software.wings.beans.infrastructure.instance.key.deployment;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentKey extends DeploymentKey {
  private String releaseName;
  private Integer releaseNumber;
}
