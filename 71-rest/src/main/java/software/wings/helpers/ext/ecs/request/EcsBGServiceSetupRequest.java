package software.wings.helpers.ext.ecs.request;

import static software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType.BG_SERVICE_SETUP;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.EcsSetupParams;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsBGServiceSetupRequest extends EcsCommandRequest {
  private EcsSetupParams ecsSetupParams;
  private Map<String, String> serviceVariables;
  private Map<String, String> safeDisplayServiceVariables;

  @Builder
  public EcsBGServiceSetupRequest(String commandName, String appId, String accountId, String activityId,
      String clusterName, String region, AwsConfig awsConfig, EcsSetupParams ecsSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables) {
    super(accountId, appId, commandName, activityId, region, clusterName, awsConfig, BG_SERVICE_SETUP);
    this.ecsSetupParams = ecsSetupParams;
    this.serviceVariables = serviceVariables;
    this.safeDisplayServiceVariables = safeDisplayServiceVariables;
  }
}