package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sCanarySetupResponse extends K8sTaskResponse {
  Integer releaseNumber;
  Integer currentInstances;

  @Builder
  public K8sCanarySetupResponse(Integer releaseNumber, Integer currentInstances) {
    this.releaseNumber = releaseNumber;
    this.currentInstances = currentInstances;
  }
}
