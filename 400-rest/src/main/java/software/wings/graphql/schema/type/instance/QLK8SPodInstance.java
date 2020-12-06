package software.wings.graphql.schema.type.instance;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QLK8SPodInstance extends QLContainerInstance {
  private String releaseName;
  private String podName;
  private String ip;
  private String namespace;
  private List<QLK8sContainer> containers;

  @Builder
  public QLK8SPodInstance(String id, QLInstanceType type, String environmentId, String applicationId, String serviceId,
      QLArtifact artifact, String clusterName, String identifier, String releaseName, String podName, String ip,
      String namespace, List<QLK8sContainer> containers) {
    super(id, type, environmentId, applicationId, serviceId, artifact, clusterName, identifier);
    this.releaseName = releaseName;
    this.podName = podName;
    this.ip = ip;
    this.namespace = namespace;
    this.containers = containers;
  }
}
