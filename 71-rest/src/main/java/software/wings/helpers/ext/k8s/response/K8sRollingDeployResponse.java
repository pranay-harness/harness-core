package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sRollingDeployResponse extends K8sTaskResponse {
  Integer releaseNumber;

  @Builder
  public K8sRollingDeployResponse(Integer releaseNumber) {
    this.releaseNumber = releaseNumber;
  }
}
