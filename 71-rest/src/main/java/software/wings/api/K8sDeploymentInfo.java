package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentInfo extends DeploymentInfo {
  private String namespace;
  private String releaseName;
  private Integer releaseNumber;
  private Set<String> namespaces = new HashSet<>();

  @Builder
  public K8sDeploymentInfo(String namespace, String releaseName, Integer releaseNumber, Set<String> namespaces) {
    this.namespace = namespace;
    this.releaseName = releaseName;
    this.releaseNumber = releaseNumber;
    this.namespaces = namespaces;
  }
}
