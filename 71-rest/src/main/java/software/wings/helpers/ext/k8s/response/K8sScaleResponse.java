package software.wings.helpers.ext.k8s.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sScaleResponse extends K8sCommandResponse {
  @Builder
  public K8sScaleResponse() {}
}
