package software.wings.sm.states.k8s;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.manifest.ManifestHelper.getValuesYamlGitFilePath;
import static io.harness.k8s.manifest.ManifestHelper.normalizeFilePath;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.serializer.KryoUtils;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.SweepingOutput;
import software.wings.beans.SweepingOutput.Scope;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.common.Constants;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig.K8sDelegateManifestConfigBuilder;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class K8sStateHelper {
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient DelegateService delegateService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private SweepingOutputService sweepingOutputService;
  @Inject private transient AwsCommandHelper awsCommandHelper;
  @Inject private transient ActivityService activityService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;

  public Activity createK8sActivity(ExecutionContext executionContext, String commandName, String stateType,
      ActivityService activityService, List<CommandUnit> commandUnits) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    Activity activity = Activity.builder()
                            .applicationName(app.getName())
                            .appId(app.getUuid())
                            .commandName(commandName)
                            .type(Type.Command)
                            .workflowType(executionContext.getWorkflowType())
                            .workflowExecutionName(executionContext.getWorkflowExecutionName())
                            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                            .commandType(stateType)
                            .workflowExecutionId(executionContext.getWorkflowExecutionId())
                            .workflowId(executionContext.getWorkflowId())
                            .commandUnits(commandUnits)
                            .status(ExecutionStatus.RUNNING)
                            .commandUnitType(CommandUnitType.KUBERNETES)
                            .environmentId(env.getUuid())
                            .environmentName(env.getName())
                            .environmentType(env.getEnvironmentType())
                            .build();

    return activityService.save(activity);
  }

  public K8sDelegateManifestConfig createDelegateManifestConfig(ApplicationManifest appManifest) {
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

  public ExecutionResponse executeGitTask(
      ExecutionContext context, ApplicationManifest applicationManifest, String activityId, String commandName) {
    Application app = appService.get(context.getAppId());

    GitFetchFilesTaskParams fetchFilesTaskParams = createGitFetchFilesTaskParams(app, applicationManifest);

    fetchFilesTaskParams.setActivityId(activityId);
    fetchFilesTaskParams.getGitFileConfig().setFilePath(
        getValuesYamlGitFilePath(applicationManifest.getGitFileConfig().getFilePath()));

    String waitId = generateUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.GIT_FETCH_FILES_TASK)
                                    .withWaitId(waitId)
                                    .withAsync(true)
                                    .withParameters(new Object[] {fetchFilesTaskParams})
                                    // ToDo anshul decide on the timeout values
                                    .withTimeout(TimeUnit.MINUTES.toMillis(60))
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(waitId))
        .withStateExecutionData(K8sStateExecutionData.builder()
                                    .activityId(activityId)
                                    .commandName(commandName)
                                    .currentTaskType(TaskType.GIT_COMMAND)
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  public GitFetchFilesTaskParams createGitFetchFilesTaskParams(
      Application app, ApplicationManifest applicationManifest) {
    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();
    gitFileConfig.setFilePath(normalizeFilePath(gitFileConfig.getFilePath()));
    GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());

    return GitFetchFilesTaskParams.builder()
        .accountId(app.getAccountId())
        .appId(app.getUuid())
        .isFinalState(false)
        .gitConfig(gitConfig)
        .gitFileConfig(gitFileConfig)
        .encryptedDataDetails(secretManager.getEncryptionDetails(gitConfig, app.getUuid(), null))
        .build();
  }

  public String getFileFromGitResponse(GitCommandExecutionResponse executionResponse) {
    GitFetchFilesResult gitFetchFilesResult = (GitFetchFilesResult) executionResponse.getGitCommandResult();
    List<GitFile> gitFiles = gitFetchFilesResult.getFiles();

    String valueYamlFromGit = null;
    if (!gitFiles.isEmpty()) {
      valueYamlFromGit = gitFiles.get(0).getFileContent();
    }

    return valueYamlFromGit;
  }

  public ApplicationManifest getApplicationManifest(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    Application app = appService.get(context.getAppId());
    ServiceElement serviceElement = phaseElement.getServiceElement();

    ApplicationManifest applicationManifest =
        applicationManifestService.getByServiceId(app.getUuid(), serviceElement.getUuid());
    if (applicationManifest == null) {
      throw new InvalidRequestException("Manifests not found for service.");
    }
    return applicationManifest;
  }

  public List<String> getRenderedValuesFiles(
      ApplicationManifest applicationManifest, ExecutionContext context, Map<K8sValuesLocation, String> valuesFiles) {
    if (applicationManifest.getStoreType() == StoreType.Local) {
      ManifestFile manifestFile =
          applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), values_filename);
      if (manifestFile != null) {
        valuesFiles.put(K8sValuesLocation.Service, manifestFile.getFileContent());
      }
    }

    List<String> result = new ArrayList<>();

    if (valuesFiles.containsKey(K8sValuesLocation.Service)) {
      result.add(context.renderExpression(valuesFiles.get(K8sValuesLocation.Service)));
    }

    if (valuesFiles.containsKey(K8sValuesLocation.EnvironmentGlobal)) {
      result.add(context.renderExpression(valuesFiles.get(K8sValuesLocation.EnvironmentGlobal)));
    }

    if (valuesFiles.containsKey(K8sValuesLocation.Environment)) {
      result.add(context.renderExpression(valuesFiles.get(K8sValuesLocation.Environment)));
    }

    return result;
  }

  public void saveK8sElement(ExecutionContext context, K8sElement k8sElement) {
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(Scope.PHASE)
                                   .name("k8s")
                                   .output(KryoUtils.asDeflatedBytes(k8sElement))
                                   .build());
  }

  public K8sElement getK8sElement(ExecutionContext context) {
    SweepingOutput sweepingOutputInput = context.prepareSweepingOutputBuilder(Scope.PHASE).name("k8s").build();
    SweepingOutput result = sweepingOutputService.find(sweepingOutputInput.getAppId(), sweepingOutputInput.getName(),
        sweepingOutputInput.getPipelineExecutionId(), sweepingOutputInput.getWorkflowExecutionId(),
        sweepingOutputInput.getPhaseExecutionId());
    if (result == null) {
      return null;
    }
    return (K8sElement) KryoUtils.asInflatedObject(result.getOutput());
  }

  public ContainerInfrastructureMapping getContainerInfrastructureMapping(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return (ContainerInfrastructureMapping) infrastructureMappingService.get(
        context.getAppId(), phaseElement.getInfraMappingId());
  }

  public Environment getEnvironment(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    return workflowStandardParams.getEnv();
  }

  public ExecutionResponse queueK8sDelegateTask(ExecutionContext context, K8sTaskParameters k8sTaskParameters) {
    Application app = appService.get(context.getAppId());
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Environment env = workflowStandardParams.getEnv();
    ContainerInfrastructureMapping infraMapping = getContainerInfrastructureMapping(context);
    String serviceTemplateId = infraMapping.getServiceTemplateId();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    String artifactStreamId = artifact == null ? null : artifact.getArtifactStreamId();

    k8sTaskParameters.setAccountId(app.getAccountId());
    k8sTaskParameters.setAppId(app.getUuid());
    k8sTaskParameters.setK8sClusterConfig(containerDeploymentManagerHelper.getK8sClusterConfig(infraMapping));
    k8sTaskParameters.setWorkflowExecutionId(context.getWorkflowExecutionId());

    String waitId = generateUuid();
    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.K8S_COMMAND_TASK)
                                    .withWaitId(waitId)
                                    .withTags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(k8sTaskParameters))
                                    .withParameters(new Object[] {k8sTaskParameters})
                                    .withEnvId(env.getUuid())
                                    .withTimeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .withInfrastructureMappingId(infraMapping.getUuid())
                                    .withServiceTemplateId(serviceTemplateId)
                                    .withArtifactStreamId(artifactStreamId)
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Arrays.asList(waitId))
        .withStateExecutionData(K8sStateExecutionData.builder()
                                    .activityId(k8sTaskParameters.getActivityId())
                                    .commandName(k8sTaskParameters.getCommandName())
                                    .namespace(k8sTaskParameters.getK8sClusterConfig().getNamespace())
                                    .clusterName(k8sTaskParameters.getK8sClusterConfig().getClusterName())
                                    .releaseName(k8sTaskParameters.getReleaseName())
                                    .currentTaskType(TaskType.K8S_COMMAND_TASK)
                                    .build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  public String getAppId(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    return workflowStandardParams.getAppId();
  }

  public String getActivityId(ExecutionContext context) {
    return ((K8sStateExecutionData) context.getStateExecutionData()).getActivityId();
  }

  public ExecutionResponse executeWrapperWithManifest(K8sStateExecutor k8sStateExecutor, ExecutionContext context) {
    try {
      k8sStateExecutor.validateParameters(context);

      ApplicationManifest applicationManifest = getApplicationManifest(context);

      Activity activity = createK8sActivity(context, k8sStateExecutor.commandName(), k8sStateExecutor.stateType(),
          activityService, k8sStateExecutor.commandUnitList(applicationManifest.getStoreType()));

      switch (applicationManifest.getStoreType()) {
        case Local:
          Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
          return k8sStateExecutor.executeK8sTask(context, activity.getUuid(), valuesFiles);

        case Remote:
          return executeGitTask(context, applicationManifest, activity.getUuid(), k8sStateExecutor.commandName());

        default:
          throw new WingsException("Unhandled manifest storeType " + applicationManifest.getStoreType());
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  public ExecutionResponse handleAsyncResponseWrapper(
      K8sStateExecutor k8sStateExecutor, ExecutionContext context, Map<String, ResponseData> response) {
    try {
      K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();

      TaskType taskType = k8sStateExecutionData.getCurrentTaskType();
      switch (taskType) {
        case GIT_COMMAND:
          return handleAsyncResponseForGitTaskWrapper(k8sStateExecutor, context, response);

        case K8S_COMMAND_TASK:
          return k8sStateExecutor.handleAsyncResponseForK8sTask(context, response);

        default:
          throw new WingsException("Unhandled task type " + taskType);
      }

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  public ExecutionResponse executeWrapperWithoutManifest(K8sStateExecutor k8sStateExecutor, ExecutionContext context) {
    try {
      k8sStateExecutor.validateParameters(context);

      Activity activity = createK8sActivity(context, k8sStateExecutor.commandName(), k8sStateExecutor.stateType(),
          activityService, k8sStateExecutor.commandUnitList(StoreType.Local));
      return k8sStateExecutor.executeK8sTask(context, activity.getUuid(), null);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncResponseForGitTaskWrapper(
      K8sStateExecutor k8sStateExecutor, ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String activityId = getActivityId(context);
    GitCommandExecutionResponse executionResponse = (GitCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getGitCommandStatus().equals(GitCommandStatus.SUCCESS)
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED.equals(executionStatus)) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return anExecutionResponse().withExecutionStatus(executionStatus).build();
    }

    String valueYamlFromGit = getFileFromGitResponse(executionResponse);

    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    valuesFiles.put(K8sValuesLocation.Service, valueYamlFromGit);

    // ToDo anshul how to handle unhappy case
    return k8sStateExecutor.executeK8sTask(context, activityId, valuesFiles);
  }
}
