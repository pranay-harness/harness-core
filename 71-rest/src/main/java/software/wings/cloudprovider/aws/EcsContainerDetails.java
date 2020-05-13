package software.wings.cloudprovider.aws;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsContainerDetails {
  private String taskId;
  private String taskArn;
  private String dockerId;
  private String completeDockerId;
  private String containerId;
  private String containerInstanceId;
  private String containerInstanceArn;
  private String ecsServiceName;
}