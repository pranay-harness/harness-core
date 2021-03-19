package software.wings.helpers.ext.ecs.request;

import static software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType.ROUTE53_BG_SERVICE_SETUP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.command.EcsSetupParams;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class EcsBGRoute53ServiceSetupRequest extends EcsCommandRequest {
  private EcsSetupParams ecsSetupParams;
  private Map<String, String> serviceVariables;
  private Map<String, String> safeDisplayServiceVariables;

  @Builder
  public EcsBGRoute53ServiceSetupRequest(String commandName, String appId, String accountId, String activityId,
      String clusterName, String region, software.wings.beans.AwsConfig awsConfig, EcsSetupParams ecsSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      boolean timeoutErrorSupported) {
    super(accountId, appId, commandName, activityId, region, clusterName, awsConfig, ROUTE53_BG_SERVICE_SETUP,
        timeoutErrorSupported);
    this.ecsSetupParams = ecsSetupParams;
    this.serviceVariables = serviceVariables;
    this.safeDisplayServiceVariables = safeDisplayServiceVariables;
  }
}
