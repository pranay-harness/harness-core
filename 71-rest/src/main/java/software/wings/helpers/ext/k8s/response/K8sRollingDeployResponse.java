package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sRollingDeployResponse extends K8sTaskResponse {
  int releaseNumber;

  @Builder
  public K8sRollingDeployResponse(int releaseNumber) {
    this.releaseNumber = releaseNumber;
  }
}
