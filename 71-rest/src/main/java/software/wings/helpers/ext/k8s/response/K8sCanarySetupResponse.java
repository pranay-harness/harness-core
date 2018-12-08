package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sCanarySetupResponse extends K8sTaskResponse {
  int releaseNumber;
  int currentReplicas;

  @Builder
  public K8sCanarySetupResponse(int releaseNumber, int currentReplicas) {
    this.releaseNumber = releaseNumber;
    this.currentReplicas = currentReplicas;
  }
}
