package io.harness.perpetualtask;

import static software.wings.sm.states.customdeployment.InstanceFetchState.OUTPUT_PATH_KEY;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams;
import io.harness.tasks.Cd1SetupFields;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.CustomDeploymentTypeInfo;
import software.wings.api.DeploymentSummary;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.expression.SecretFunctor;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CustomDeploymentInstanceSyncClient implements PerpetualTaskServiceClient {
  @Inject private DeploymentService deploymentService;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SecretManager secretManager;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    final Optional<DeploymentSummary> deploymentSummaryOptional =
        deploymentService.getWithInfraMappingId(getAccountId(clientContext), getInfraMappingId(clientContext));
    if (deploymentSummaryOptional.isPresent()) {
      final CustomDeploymentTypeInfo deploymentInfo =
          (CustomDeploymentTypeInfo) (deploymentSummaryOptional.get().getDeploymentInfo());
      return CustomDeploymentInstanceSyncTaskParams.newBuilder()
          .setScript((String) expressionEvaluator.substitute(
              deploymentInfo.getInstanceFetchScript(), prepareContext(clientContext)))
          .setAccountId(getAccountId(clientContext))
          .setAppId(getAppId(clientContext))
          .setOutputPathKey(OUTPUT_PATH_KEY)
          .build();
    }
    log.error(
        "[CustomDeploymentError] No Deployment Summary Found For InfraMapping " + getInfraMappingId(clientContext));
    return null;
  }

  private Map<String, Object> prepareContext(PerpetualTaskClientContext clientContext) {
    return ImmutableMap.<String, Object>builder()
        .put("secrets",
            SecretFunctor.builder()
                .managerDecryptionService(managerDecryptionService)
                .secretManager(secretManager)
                .accountId(getAccountId(clientContext))
                .appId(getAppId(clientContext))
                .envId(getEnvId(clientContext))
                .disablePhasing(true)
                .build())
        .build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    ShellScriptProvisionParameters taskParameters =
        ShellScriptProvisionParameters.builder()
            .accountId(accountId)
            .appId(getAppId(clientContext))
            .scriptBody(StringUtils.EMPTY)
            .commandUnit(CommandUnitDetails.CommandUnitType.CUSTOM_DEPLOYMENT_FETCH_INSTANCES.getName())
            .outputPathKey(OUTPUT_PATH_KEY)
            .workflowExecutionId("test-execution-id")
            .build();
    return DelegateTask.builder()
        .accountId(accountId)
        .description("Fetch Instances")
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, getAppId(clientContext))
        .data(TaskData.builder()
                  .async(true)
                  .parameters(new Object[] {taskParameters})
                  .taskType(TaskType.SHELL_SCRIPT_PROVISION_TASK.name())
                  .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .build();
  }

  private String getAppId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.HARNESS_APPLICATION_ID);
  }

  private String getInfraMappingId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID);
  }

  private String getAccountId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.HARNESS_ACCOUNT_ID);
  }

  private String getEnvId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.HARNESS_ENV_ID);
  }
}
