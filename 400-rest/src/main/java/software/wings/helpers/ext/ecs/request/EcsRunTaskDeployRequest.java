package software.wings.helpers.ext.ecs.request;

import static software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType.ECS_RUN_TASK_DEPLOY;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsRunTaskDeployRequest extends EcsCommandRequest {
  private List<String> listTaskDefinitionJson;
  private String runTaskFamilyName;
  private String launchType;
  private Long serviceSteadyStateTimeout;
  private boolean skipSteadyStateCheck;
  private boolean isAssignPublicIps;
  private List<String> subnetIds;
  private List<String> securityGroupIds;

  @Builder
  public EcsRunTaskDeployRequest(String accountId, String appId, String commandName, String activityId, String region,
      String cluster, AwsConfig awsConfig, List<String> listTaskDefinitionJson, String runTaskFamilyName,
      String launchType, boolean isAssignPublicIps, Long serviceSteadyStateTimeout, List<String> subnetIds,
      List<String> securityGroupIds, boolean skipSteadyStateCheck) {
    super(accountId, appId, commandName, activityId, region, cluster, awsConfig, ECS_RUN_TASK_DEPLOY);
    this.listTaskDefinitionJson = listTaskDefinitionJson;
    this.runTaskFamilyName = runTaskFamilyName;
    this.launchType = launchType;
    this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
    this.isAssignPublicIps = isAssignPublicIps;
    this.subnetIds = subnetIds;
    this.securityGroupIds = securityGroupIds;
    this.skipSteadyStateCheck = skipSteadyStateCheck;
  }
}
