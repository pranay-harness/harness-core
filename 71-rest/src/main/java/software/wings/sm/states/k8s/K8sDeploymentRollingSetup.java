package software.wings.sm.states.k8s;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.delegatetasks.k8s.Utils.getValuesYamlGitFilePath;
import static software.wings.delegatetasks.k8s.Utils.normalizeFilePath;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.k8s.K8sDeploymentRollingSetupStateExecutionData;
import software.wings.api.k8s.K8sRollingDeploySetupElement;
import software.wings.api.k8s.K8sRollingDeploySetupElement.K8sRollingDeploySetupElementBuilder;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.common.Constants;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sCommandRequest.K8sCommandType;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig.K8sDelegateManifestConfigBuilder;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingSetupRequest;
import software.wings.helpers.ext.k8s.request.K8sDeploymentRollingSetupRequest.K8sDeploymentRollingSetupRequestBuilder;
import software.wings.helpers.ext.k8s.response.K8sCommandExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sDeploymentRollingSetupResponse;
import software.wings.security.encryption.EncryptedDataDetail;
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
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sDeploymentRollingSetup extends State {
  private static final transient Logger logger = LoggerFactory.getLogger(K8sDeploymentRollingSetup.class);

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

  public static final String K8S_DEPLOYMENT_SETUP_ROLLING_COMMAND_NAME = "Rolling Deployment";

  public K8sDeploymentRollingSetup(String name) {
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
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      Application app = appService.get(context.getAppId());
      ServiceElement serviceElement = phaseElement.getServiceElement();

      ApplicationManifest applicationManifest = applicationManifestService.get(app.getUuid(), serviceElement.getUuid());
      if (applicationManifest == null) {
        throw new InvalidRequestException("Manifests not found for service.");
      }

      Activity activity = k8sStateHelper.createK8sActivity(context, K8S_DEPLOYMENT_SETUP_ROLLING_COMMAND_NAME,
          getStateType(), activityService, getCommandUnitList(applicationManifest));

      if (StoreType.Local.equals(applicationManifest.getStoreType())) {
        return executeK8CommandTask(context, activity.getUuid(), null);
      } else {
        return executeGitTask(app, applicationManifest, activity.getUuid());
      }

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse executeGitTask(
      Application app, ApplicationManifest applicationManifest, String activityId) {
    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();
    GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());

    gitFileConfig.setFilePath(getValuesYamlGitFilePath(gitFileConfig.getFilePath()));
    GitFetchFilesTaskParams fetchFilesTaskParams =
        GitFetchFilesTaskParams.builder()
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .activityId(activityId)
            .isFinalState(false)
            .gitConfig(gitConfig)
            .gitFileConfig(gitFileConfig)
            .encryptedDataDetails(secretManager.getEncryptionDetails(gitConfig, app.getUuid(), null))
            .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.GIT_FETCH_FILES_TASK)
                                    .withWaitId(activityId)
                                    .withAsync(true)
                                    .withParameters(new Object[] {fetchFilesTaskParams})
                                    // ToDo anshul decide on the timeout values
                                    .withTimeout(TimeUnit.MINUTES.toMillis(60))
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(activityId))
        .withStateExecutionData(K8sDeploymentRollingSetupStateExecutionData.builder()
                                    .activityId(activityId)
                                    .commandName(K8S_DEPLOYMENT_SETUP_ROLLING_COMMAND_NAME)
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .addContextElement(K8sRollingDeploySetupElement.builder().currentTaskType(TaskType.GIT_COMMAND).build())
        .build();
  }

  private ExecutionResponse executeK8CommandTask(ExecutionContext context, String activityId, String valueYamlFromGit) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    Application app = appService.get(context.getAppId());
    ServiceElement serviceElement = phaseElement.getServiceElement();

    ApplicationManifest applicationManifest = applicationManifestService.get(app.getUuid(), serviceElement.getUuid());

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Environment env = workflowStandardParams.getEnv();

    ContainerInfrastructureMapping infraMapping = (ContainerInfrastructureMapping) infrastructureMappingService.get(
        app.getUuid(), phaseElement.getInfraMappingId());

    // ToDo anshul -- Fetch values yaml from service and environment and then decide the order

    List<String> valuesYamlList = getValuesYamlList(valueYamlFromGit);

    K8sDeploymentRollingSetupRequestBuilder setupRequestBuilder =
        K8sDeploymentRollingSetupRequest.builder()
            .activityId(activityId)
            .appId(app.getUuid())
            .accountId(app.getAccountId())
            .releaseName(convertBase64UuidToCanonicalForm(infraMapping.getUuid()))
            .commandName(K8S_DEPLOYMENT_SETUP_ROLLING_COMMAND_NAME)
            .k8sCommandType(K8sCommandType.DEPLOYMENT_ROLLING)
            .k8sClusterConfig(containerDeploymentManagerHelper.getK8sClusterConfig(infraMapping))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .timeoutIntervalInMin(10)
            .k8sDelegateManifestConfig(createDelegateManifestConfig(applicationManifest))
            .valuesYamlList(valuesYamlList);

    K8sCommandRequest commandRequest = setupRequestBuilder.build();

    String waitId = generateUuid();
    DelegateTask delegateTask =
        aDelegateTask()
            .withAccountId(app.getAccountId())
            .withAppId(app.getUuid())
            .withTaskType(TaskType.K8S_COMMAND_TASK)
            .withWaitId(waitId)
            .withTags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(commandRequest))
            .withParameters(new Object[] {commandRequest})
            .withEnvId(env.getUuid())
            .withTimeout(getTimeoutMillis() != null ? getTimeoutMillis() : DEFAULT_ASYNC_CALL_TIMEOUT)
            .withInfrastructureMappingId(infraMapping.getUuid())
            .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(waitId))
        .withStateExecutionData(K8sDeploymentRollingSetupStateExecutionData.builder()
                                    .activityId(activityId)
                                    .commandName(K8S_DEPLOYMENT_SETUP_ROLLING_COMMAND_NAME)
                                    .namespace(commandRequest.getK8sClusterConfig().getNamespace())
                                    .clusterName(commandRequest.getK8sClusterConfig().getClusterName())
                                    .releaseName(commandRequest.getReleaseName())
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .addContextElement(K8sRollingDeploySetupElement.builder().currentTaskType(TaskType.K8S_COMMAND_TASK).build())
        .build();
  }

  private K8sDelegateManifestConfig createDelegateManifestConfig(ApplicationManifest appManifest) {
    K8sDelegateManifestConfigBuilder manifestConfigBuilder =
        K8sDelegateManifestConfig.builder().manifestStoreTypes(appManifest.getStoreType());

    if (StoreType.Local.equals(appManifest.getStoreType())) {
      manifestConfigBuilder.manifestFiles(
          applicationManifestService.getManifestFilesByAppManifestId(appManifest.getAppId(), appManifest.getUuid()));
    } else {
      GitFileConfig gitFileConfig = appManifest.getGitFileConfig();
      GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(gitConfig, appManifest.getAppId(), null);

      gitFileConfig.setFilePath(normalizeFilePath(gitFileConfig.getFilePath()));
      manifestConfigBuilder.gitFileConfig(gitFileConfig);
      manifestConfigBuilder.gitConfig(gitConfig);
      manifestConfigBuilder.encryptedDataDetails(encryptionDetails);
    }

    return manifestConfigBuilder.build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      K8sRollingDeploySetupElement contextElement =
          context.getContextElement(ContextElementType.K8S_ROLLING_DEPLOY_SETUP);

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

    GitFetchFilesResult gitFetchFilesResult = (GitFetchFilesResult) executionResponse.getGitCommandResult();
    List<GitFile> gitFiles = gitFetchFilesResult.getFiles();

    String valueYamlFromGit = null;
    if (!gitFiles.isEmpty()) {
      valueYamlFromGit = gitFiles.get(0).getFileContent();

      if (isNotBlank(valueYamlFromGit)) {
        valueYamlFromGit = context.renderExpression(valueYamlFromGit);
      }
    }

    // ToDo anshul how to handle unhappy case
    return executeK8CommandTask(context, activityId, valueYamlFromGit);
  }

  private ExecutionResponse handleAsyncResponseForK8CommandTask(
      ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    K8sCommandExecutionResponse executionResponse = (K8sCommandExecutionResponse) response.values().iterator().next();

    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;

    K8sDeploymentRollingSetupResponse k8sDeploymentRollingSetupResponse =
        (K8sDeploymentRollingSetupResponse) executionResponse.getK8sCommandResponse();

    K8sRollingDeploySetupElementBuilder k8sRollingDeploySetupElementBuilder = K8sRollingDeploySetupElement.builder();
    k8sRollingDeploySetupElementBuilder.releaseNumber(k8sDeploymentRollingSetupResponse.getReleaseNumber());

    String activityId = k8sDeploymentRollingSetupResponse.getActivityId();
    activityService.updateStatus(activityId, appId, executionStatus);

    K8sDeploymentRollingSetupStateExecutionData stateExecutionData =
        (K8sDeploymentRollingSetupStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setReleaseNumber(k8sDeploymentRollingSetupResponse.getReleaseNumber());
    stateExecutionData.setStatus(executionStatus);

    K8sRollingDeploySetupElement k8sRollingDeploySetupElement = k8sRollingDeploySetupElementBuilder.build();

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(context.getStateExecutionData())
        .addContextElement(k8sRollingDeploySetupElement)
        .addNotifyElement(k8sRollingDeploySetupElement)
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
