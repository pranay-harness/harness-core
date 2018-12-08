package software.wings.sm.states.k8s;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.k8s.K8sContextElement;
import software.wings.api.k8s.K8sContextElement.K8sContextElementBuilder;
import software.wings.api.k8s.K8sSetupExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters.K8sRollingDeployTaskParametersBuilder;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType;
import software.wings.helpers.ext.k8s.response.K8sRollingDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sRollingDeploy extends State {
  private static final transient Logger logger = LoggerFactory.getLogger(K8sRollingDeploy.class);

  @Inject private transient ActivityService activityService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private transient K8sStateHelper k8sStateHelper;
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient AwsCommandHelper awsCommandHelper;

  public static final String K8S_ROLLING_DEPLOY_COMMAND_NAME = "Rolling Deployment";

  public K8sRollingDeploy(String name) {
    super(name, K8S_DEPLOYMENT_ROLLING.name());
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      ApplicationManifest applicationManifest = k8sStateHelper.getApplicationManifest(context);

      Activity activity = k8sStateHelper.createK8sActivity(context, K8S_ROLLING_DEPLOY_COMMAND_NAME, getStateType(),
          activityService, getCommandUnitList(applicationManifest));

      switch (applicationManifest.getStoreType()) {
        case Local:
          return executeK8sRollingDeployTask(context, activity.getUuid(), null);

        case Remote:
          return k8sStateHelper.executeGitTask(
              context, applicationManifest, activity.getUuid(), K8S_ROLLING_DEPLOY_COMMAND_NAME);

        default:
          throw new WingsException("Unhandled manifest storeType " + applicationManifest.getStoreType());
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse executeK8sRollingDeployTask(
      ExecutionContext context, String activityId, String valueYamlFromGit) {
    Application app = appService.get(context.getAppId());

    ApplicationManifest applicationManifest = k8sStateHelper.getApplicationManifest(context);

    ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

    // ToDo anshul -- Fetch values yaml from service and environment and then decide the order

    List<String> valuesYamlList = getValuesYamlList(valueYamlFromGit);

    K8sRollingDeployTaskParametersBuilder setupRequestBuilder =
        K8sRollingDeployTaskParameters.builder()
            .activityId(activityId)
            .appId(app.getUuid())
            .accountId(app.getAccountId())
            .releaseName(convertBase64UuidToCanonicalForm(infraMapping.getUuid()))
            .commandName(K8S_ROLLING_DEPLOY_COMMAND_NAME)
            .k8sTaskType(K8sTaskType.DEPLOYMENT_ROLLING)
            .k8sClusterConfig(containerDeploymentManagerHelper.getK8sClusterConfig(infraMapping))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .timeoutIntervalInMin(10)
            .k8sDelegateManifestConfig(k8sStateHelper.createDelegateManifestConfig(applicationManifest))
            .valuesYamlList(valuesYamlList);

    K8sTaskParameters commandRequest = setupRequestBuilder.build();

    String waitId = generateUuid();
    DelegateTask delegateTask =
        aDelegateTask()
            .withAccountId(app.getAccountId())
            .withAppId(app.getUuid())
            .withTaskType(TaskType.K8S_COMMAND_TASK)
            .withWaitId(waitId)
            .withTags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(commandRequest))
            .withParameters(new Object[] {commandRequest})
            .withEnvId(k8sStateHelper.getEnvironment(context).getUuid())
            .withTimeout(getTimeoutMillis() != null ? getTimeoutMillis() : DEFAULT_ASYNC_CALL_TIMEOUT)
            .withInfrastructureMappingId(infraMapping.getUuid())
            .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(waitId))
        .withStateExecutionData(K8sSetupExecutionData.builder()
                                    .activityId(activityId)
                                    .commandName(K8S_ROLLING_DEPLOY_COMMAND_NAME)
                                    .namespace(commandRequest.getK8sClusterConfig().getNamespace())
                                    .clusterName(commandRequest.getK8sClusterConfig().getClusterName())
                                    .releaseName(commandRequest.getReleaseName())
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .addContextElement(K8sContextElement.builder().currentTaskType(TaskType.K8S_COMMAND_TASK).build())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      K8sContextElement contextElement = context.getContextElement(ContextElementType.K8S);

      TaskType taskType = contextElement.getCurrentTaskType();
      switch (taskType) {
        case GIT_COMMAND:
          return handleAsyncResponseForGitTask(context, response);

        case K8S_COMMAND_TASK:
          return handleAsyncResponseForK8CommandTask(context, response);

        default:
          throw new WingsException("Unhandled task type " + taskType);
      }

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncResponseForGitTask(
      ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String activityId = response.keySet().iterator().next();
    GitCommandExecutionResponse executionResponse = (GitCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getGitCommandStatus().equals(GitCommandStatus.SUCCESS)
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED.equals(executionStatus)) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return anExecutionResponse().withExecutionStatus(executionStatus).build();
    }

    String valueYamlFromGit = k8sStateHelper.getFileFromGitResponse(executionResponse);

    // ToDo anshul how to handle unhappy case
    return executeK8sRollingDeployTask(context, activityId, valueYamlFromGit);
  }

  private ExecutionResponse handleAsyncResponseForK8CommandTask(
      ExecutionContext context, Map<String, ResponseData> response) {
    Application app = appService.get(context.getAppId());
    K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    K8sRollingDeployResponse k8sRollingDeployResponse =
        (K8sRollingDeployResponse) executionResponse.getK8sTaskResponse();

    K8sContextElementBuilder k8sContextElementBuilder = K8sContextElement.builder();
    k8sContextElementBuilder.releaseNumber(k8sRollingDeployResponse.getReleaseNumber());

    String activityId = k8sRollingDeployResponse.getActivityId();
    activityService.updateStatus(activityId, app.getUuid(), executionStatus);

    K8sSetupExecutionData stateExecutionData = (K8sSetupExecutionData) context.getStateExecutionData();
    stateExecutionData.setReleaseNumber(k8sRollingDeployResponse.getReleaseNumber());
    stateExecutionData.setStatus(executionStatus);

    K8sContextElement k8SContextElement = k8sContextElementBuilder.build();

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(context.getStateExecutionData())
        .addContextElement(k8SContextElement)
        .addNotifyElement(k8SContextElement)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private List<CommandUnit> getCommandUnitList(ApplicationManifest appManifest) {
    List<CommandUnit> commandUnits = new ArrayList<>();

    if (StoreType.Remote.equals(appManifest.getStoreType())) {
      commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.FetchFiles));
    }

    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Prepare));
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Apply));
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WaitForSteadyState));
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.WrapUp));

    return commandUnits;
  }

  private List<String> getValuesYamlList(String valuesYamlFromGit) {
    List<String> valuesYamlList = new ArrayList<>();

    if (isNotBlank(valuesYamlFromGit)) {
      valuesYamlList.add(valuesYamlFromGit);
    }

    return valuesYamlList;
  }
}
