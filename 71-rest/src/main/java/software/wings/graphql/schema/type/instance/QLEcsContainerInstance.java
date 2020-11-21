package software.wings.graphql.schema.type.instance;

import software.wings.graphql.schema.type.artifact.QLArtifact;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QLEcsContainerInstance extends QLContainerInstance {
  private String taskArn;
  private String taskDefinitionArn;
  private String serviceName;
  private long version;
  private Long startedAt;
  private String startedBy;

  @Builder
  public QLEcsContainerInstance(String id, QLInstanceType type, String environmentId, String applicationId,
      String serviceId, QLArtifact artifact, String clusterName, String identifier, String taskArn,
      String taskDefinitionArn, String serviceName, long version, Long startedAt, String startedBy) {
    super(id, type, environmentId, applicationId, serviceId, artifact, clusterName, identifier);
    this.taskArn = taskArn;
    this.taskDefinitionArn = taskDefinitionArn;
    this.serviceName = serviceName;
    this.version = version;
    this.startedAt = startedAt;
    this.startedBy = startedBy;
  }
}
