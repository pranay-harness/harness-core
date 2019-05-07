package software.wings.graphql.schema.type.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class QLEcsContainerInfo extends QLContainerInfo {
  private String taskArn;
  private String taskDefinitionArn;
  private String serviceName;
  private long version;
  private ZonedDateTime startedAt;
  private String startedBy;

  @Builder
  public QLEcsContainerInfo(String clusterName, String taskArn, String taskDefinitionArn, String serviceName,
      long version, ZonedDateTime startedAt, String startedBy) {
    super(clusterName);
    this.taskArn = taskArn;
    this.taskDefinitionArn = taskDefinitionArn;
    this.serviceName = serviceName;
    this.version = version;
    this.startedAt = startedAt;
    this.startedBy = startedBy;
  }
}
