package software.wings.sm.states.argo;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.TaskType.ARGOCD_TASK;

import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;

import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.settings.argo.ArgoConfig;
import software.wings.delegatetasks.argo.beans.request.ArgoRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.argo.drift.ArgoDriftExecutionData;
import software.wings.sm.states.argo.sync.ArgoSyncExecutionData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class ArgoStateHelper {
  @Inject private transient ActivityService activityService;

  public Activity createActivity(ExecutionContext executionContext, Artifact artifact, String stateType, String command,
      CommandUnitDetails.CommandUnitType commandUnitType, List<CommandUnit> commandUnits) {
    Application app = ((ExecutionContextImpl) executionContext).fetchRequiredApp();
    Environment env = ((ExecutionContextImpl) executionContext).fetchRequiredEnvironment();
    Activity.ActivityBuilder activityBuilder = generateActivityBuilder(app.getName(), app.getUuid(), command,
        Activity.Type.Command, executionContext, stateType, commandUnitType, env, commandUnits);

    if (artifact != null) {
      activityBuilder.artifactName(artifact.getDisplayName()).artifactId(artifact.getUuid());
    }

    return activityService.save(activityBuilder.build());
  }

  public Activity.ActivityBuilder generateActivityBuilder(String appName, String appId, String commandName,
      Activity.Type type, ExecutionContext executionContext, String commandType,
      CommandUnitDetails.CommandUnitType commandUnitType, Environment environment, List<CommandUnit> commandUnits) {
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    return Activity.builder()
        .applicationName(appName)
        .appId(appId)
        .commandName(commandName)
        .type(type)
        .commandType(commandType)
        .commandUnits(commandUnits)
        .commandUnitType(commandUnitType)
        .status(ExecutionStatus.RUNNING)
        .environmentId(environment.getUuid())
        .environmentType(environment.getEnvironmentType())
        .environmentName(environment.getName())
        .workflowType(executionContext.getWorkflowType())
        .workflowExecutionName(executionContext.getWorkflowExecutionName())
        .workflowExecutionId(executionContext.getWorkflowExecutionId())
        .workflowId(executionContext.getWorkflowId())
        .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
        .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
        .triggeredBy(TriggeredBy.builder()
                         .name(workflowStandardParams.getCurrentUser().getName())
                         .email(workflowStandardParams.getCurrentUser().getEmail())
                         .build());
  }

  public DelegateTask getDelegateTask(Application app, Activity activity, Environment env,
      DirectKubernetesInfrastructureMapping infrastructureMapping, ArgoRequest argoRequest,
      ServiceElement serviceElement, boolean isSelectionLogsTrackingForTasksEnabled) {
    return DelegateTask.builder()
        .accountId(app.getAccountId())
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, app.getUuid())
        .waitId(activity.getUuid())
        .data(TaskData.builder()
                  .async(true)
                  .taskType(ARGOCD_TASK.name())
                  .parameters(new Object[] {argoRequest})
                  .timeout(TimeUnit.MINUTES.toMillis(10))
                  .build())
        .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, env.getUuid())
        .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, env.getEnvironmentType().name())
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, infrastructureMapping.getUuid())
        .setupAbstraction(Cd1SetupFields.SERVICE_ID_FIELD, serviceElement.getUuid())
        .selectionLogsTrackingEnabled(isSelectionLogsTrackingForTasksEnabled)
        .description("Argo Drift  Execution")
        .build();
  }

  public ArgoDriftExecutionData getArgoDriftExecutionData(Application app, Environment env,
      DirectKubernetesInfrastructureMapping infrastructureMapping,
      software.wings.infra.argo.ArgoAppConfig argoAppConfig, SettingAttribute settingAttribute, ArgoConfig argoConfig,
      Activity activity, String commandName) {
    return ArgoDriftExecutionData.builder()
        .activityId(activity.getUuid())
        .argoAppName(argoAppConfig.getAppName())
        .argoServerUrl(argoConfig.getArgoServerUrl())
        .clusterUrl(argoAppConfig.getClusterUrl())
        .commandName(commandName)
        .envId(env.getUuid())
        .appId(app.getUuid())
        .infraMappingId(infrastructureMapping.getUuid())
        .repoUrl(argoAppConfig.getGitRepoUrl())
        .repoRef(argoAppConfig.getGitRef())
        .syncOption(argoAppConfig.getSync().toString())
        .argoConnectorId(settingAttribute.getUuid())
        .build();
  }

  public ArgoSyncExecutionData getArgoSyncExecutionData(Application app, Environment env,
      DirectKubernetesInfrastructureMapping infrastructureMapping,
      software.wings.infra.argo.ArgoAppConfig argoAppConfig, SettingAttribute settingAttribute, ArgoConfig argoConfig,
      Activity activity, String commandName) {
    return ArgoSyncExecutionData.builder()
        .activityId(activity.getUuid())
        .argoAppName(argoAppConfig.getAppName())
        .argoServerUrl(argoConfig.getArgoServerUrl())
        .clusterUrl(argoAppConfig.getClusterUrl())
        .commandName(commandName)
        .envId(env.getUuid())
        .appId(app.getUuid())
        .infraMappingId(infrastructureMapping.getUuid())
        .repoUrl(argoAppConfig.getGitRepoUrl())
        .repoRef(argoAppConfig.getGitRef())
        .syncOption(argoAppConfig.getSync().toString())
        .argoConnectorId(settingAttribute.getUuid())
        .build();
  }
}
