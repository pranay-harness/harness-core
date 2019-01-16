package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentInfo extends DeploymentInfo {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;

  @Builder
  public K8sDeploymentInfo(String namespace, String releaseName, Integer releaseNumber) {
    this.namespace = namespace;
    this.releaseName = releaseName;
    this.releaseNumber = releaseNumber;
  }
}
