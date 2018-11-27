package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentRollingSetupResponse extends K8sCommandResponse {
  int releaseNumber;

  @Builder
  public K8sDeploymentRollingSetupResponse(int releaseNumber) {
    this.releaseNumber = releaseNumber;
  }
}
