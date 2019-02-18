package software.wings.helpers.ext.ecs.request;

import static software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType.ROUTE53_BG_SERVICE_SETUP;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.EcsSetupParams;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsBGRoute53ServiceSetupRequest extends EcsCommandRequest {
  private EcsSetupParams ecsSetupParams;
  private Map<String, String> serviceVariables;
  private Map<String, String> safeDisplayServiceVariables;

  @Builder
  public EcsBGRoute53ServiceSetupRequest(String commandName, String appId, String accountId, String activityId,
      String clusterName, String region, software.wings.beans.AwsConfig awsConfig, EcsSetupParams ecsSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables) {
    super(accountId, appId, commandName, activityId, region, clusterName, awsConfig, ROUTE53_BG_SERVICE_SETUP);
    this.ecsSetupParams = ecsSetupParams;
    this.serviceVariables = serviceVariables;
    this.safeDisplayServiceVariables = safeDisplayServiceVariables;
  }
}