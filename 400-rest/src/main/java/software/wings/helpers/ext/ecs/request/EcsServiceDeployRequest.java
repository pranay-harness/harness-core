package software.wings.helpers.ext.ecs.request;

import static software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType.SERVICE_DEPLOY;

import software.wings.beans.AwsConfig;
import software.wings.beans.command.EcsResizeParams;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsServiceDeployRequest extends EcsCommandRequest {
  private EcsResizeParams ecsResizeParams;

  @Builder
  public EcsServiceDeployRequest(String accountId, String appId, String commandName, String activityId, String region,
      String cluster, AwsConfig awsConfig, EcsResizeParams ecsResizeParams) {
    super(accountId, appId, commandName, activityId, region, cluster, awsConfig, SERVICE_DEPLOY);
    this.ecsResizeParams = ecsResizeParams;
  }
}
