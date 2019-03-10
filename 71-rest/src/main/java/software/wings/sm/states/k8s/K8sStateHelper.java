package software.wings.sm.states.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.manifest.ManifestHelper.getValuesYamlGitFilePath;
import static io.harness.k8s.manifest.ManifestHelper.normalizeFolderPath;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.K8sPod;
import io.harness.serializer.KryoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
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
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SweepingOutput;
import software.wings.beans.SweepingOutput.Scope;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.common.Constants;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig.K8sDelegateManifestConfigBuilder;
import software.wings.helpers.ext.k8s.request.K8sInstanceSyncTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sInstanceSyncResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
  @Inject GitFileConfigHelperService gitFileConfigHelperService;

  private static final Logger logger = LoggerFactory.getLogger(K8sStateHelper.class);

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

  public K8sDelegateManifestConfig createDelegateManifestConfig(
      ExecutionContext context, ApplicationManifest appManifest) {
    K8sDelegateManifestConfigBuilder manifestConfigBuilder =
        K8sDelegateManifestConfig.builder().manifestStoreTypes(appManifest.getStoreType());

    if (StoreType.Local.equals(appManifest.getStoreType())) {
      manifestConfigBuilder.manifestFiles(
          applicationManifestService.getManifestFilesByAppManifestId(appManifest.getAppId(), appManifest.getUuid()));
    } else {
      GitFileConfig gitFileConfig =
          gitFileConfigHelperService.renderGitFileConfig(context, appManifest.getGitFileConfig());
      GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
      notNullCheck("Git config not found", gitConfig);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails(gitConfig, appManifest.getAppId(), null);

      gitFileConfig.setFilePath(normalizeFolderPath(gitFileConfig.getFilePath()));
      manifestConfigBuilder.gitFileConfig(gitFileConfig);
      manifestConfigBuilder.gitConfig(gitConfig);
      manifestConfigBuilder.encryptedDataDetails(encryptionDetails);
    }

    return manifestConfigBuilder.build();
  }

  public ExecutionResponse executeGitTask(ExecutionContext context,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, String activityId, String commandName) {
    Application app = appService.get(context.getAppId());

    GitFetchFilesTaskParams fetchFilesTaskParams = createGitFetchFilesTaskParams(context, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(activityId);
    setValuesYamlPath(fetchFilesTaskParams);

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .taskType(TaskType.GIT_FETCH_FILES_TASK.name())
                                    .waitId(waitId)
                                    .async(true)
                                    .data(TaskData.builder().parameters(new Object[] {fetchFilesTaskParams}).build())
                                    .timeout(TimeUnit.MINUTES.toMillis(60))
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

  private void setValuesYamlPath(GitFetchFilesTaskParams gitFetchFilesTaskParams) {
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap = gitFetchFilesTaskParams.getGitFetchFilesConfigMap();

    for (Entry<String, GitFetchFilesConfig> entry : gitFetchFileConfigMap.entrySet()) {
      if (K8sValuesLocation.Service.name().equals(entry.getKey())) {
        GitFetchFilesConfig gitFetchFileConfig = entry.getValue();
        gitFetchFileConfig.getGitFileConfig().setFilePath(
            getValuesYamlGitFilePath(gitFetchFileConfig.getGitFileConfig().getFilePath()));
      }
    }
  }

  private Map<String, GitFetchFilesConfig> getGitFetchFileConfigMap(
      ExecutionContext context, Application app, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap = new HashMap<>();

    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      K8sValuesLocation k8sValuesLocation = entry.getKey();
      ApplicationManifest applicationManifest = entry.getValue();

      if (StoreType.Remote.equals(applicationManifest.getStoreType())) {
        GitFileConfig gitFileConfig =
            gitFileConfigHelperService.renderGitFileConfig(context, applicationManifest.getGitFileConfig());
        GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitFileConfig.getConnectorId());
        notNullCheck("Git config not found", gitConfig);
        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails(gitConfig, app.getUuid(), null);

        GitFetchFilesConfig gitFetchFileConfig = GitFetchFilesConfig.builder()
                                                     .gitConfig(gitConfig)
                                                     .gitFileConfig(gitFileConfig)
                                                     .encryptedDataDetails(encryptionDetails)
                                                     .build();

        gitFetchFileConfigMap.put(k8sValuesLocation.name(), gitFetchFileConfig);
      }
    }

    return gitFetchFileConfigMap;
  }

  public GitFetchFilesTaskParams createGitFetchFilesTaskParams(
      ExecutionContext context, Application app, Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    Map<String, GitFetchFilesConfig> gitFetchFileConfigMap = getGitFetchFileConfigMap(context, app, appManifestMap);

    return GitFetchFilesTaskParams.builder()
        .accountId(app.getAccountId())
        .appId(app.getUuid())
        .isFinalState(isRemoteFetchRequiredForManifest(appManifestMap))
        .gitFetchFilesConfigMap(gitFetchFileConfigMap)
        .build();
  }

  public Map<K8sValuesLocation, ApplicationManifest> getApplicationManifests(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    Application app = appService.get(context.getAppId());
    ServiceElement serviceElement = phaseElement.getServiceElement();

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();

    ApplicationManifest applicationManifest =
        applicationManifestService.getByServiceId(app.getUuid(), serviceElement.getUuid());
    if (applicationManifest == null) {
      throw new InvalidRequestException("Manifests not found for service.");
    }
    appManifestMap.put(K8sValuesLocation.Service, applicationManifest);

    InfrastructureMapping infraMapping =
        infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());
    if (infraMapping == null) {
      throw new InvalidRequestException(format(
          "Infra mapping not found for appId %s infraMappingId %s", app.getUuid(), phaseElement.getInfraMappingId()));
    }

    applicationManifest =
        applicationManifestService.getByEnvId(app.getUuid(), infraMapping.getEnvId(), AppManifestKind.VALUES);
    if (applicationManifest != null) {
      appManifestMap.put(K8sValuesLocation.EnvironmentGlobal, applicationManifest);
    }

    applicationManifest = applicationManifestService.getByEnvAndServiceId(
        app.getUuid(), infraMapping.getEnvId(), serviceElement.getUuid(), AppManifestKind.VALUES);
    if (applicationManifest != null) {
      appManifestMap.put(K8sValuesLocation.Environment, applicationManifest);
    }

    return appManifestMap;
  }

  public boolean doManifestsUseArtifact(String appId, String infraMappingId) {
    InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infraMapping == null) {
      throw new InvalidRequestException(
          format("Infra mapping not found for appId %s infraMappingId %s", appId, infraMappingId));
    }

    ApplicationManifest applicationManifest =
        applicationManifestService.getByServiceId(appId, infraMapping.getServiceId());
    if (doesValuesFileContainArtifact(applicationManifest)) {
      return true;
    }

    applicationManifest = applicationManifestService.getByEnvId(appId, infraMapping.getEnvId(), AppManifestKind.VALUES);
    if (doesValuesFileContainArtifact(applicationManifest)) {
      return true;
    }

    applicationManifest = applicationManifestService.getByEnvAndServiceId(
        appId, infraMapping.getEnvId(), infraMapping.getServiceId(), AppManifestKind.VALUES);

    return doesValuesFileContainArtifact(applicationManifest);
  }

  private boolean doesValuesFileContainArtifact(ApplicationManifest applicationManifest) {
    if (applicationManifest != null && StoreType.Local.equals(applicationManifest.getStoreType())) {
      ManifestFile manifestFile =
          applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), values_filename);
      if (manifestFile != null) {
        return contains(manifestFile.getFileContent(), "${artifact.");
      }
    }
    return false;
  }

  private void populateValuesFiles(
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, Map<K8sValuesLocation, String> valuesFiles) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      K8sValuesLocation k8sValuesLocation = entry.getKey();
      ApplicationManifest applicationManifest = entry.getValue();
      if (StoreType.Local.equals(applicationManifest.getStoreType())) {
        ManifestFile manifestFile =
            applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), values_filename);
        if (manifestFile != null) {
          valuesFiles.put(k8sValuesLocation, manifestFile.getFileContent());
        }
      }
    }
  }

  public List<String> getRenderedValuesFiles(Map<K8sValuesLocation, ApplicationManifest> appManifestMap,
      ExecutionContext context, Map<K8sValuesLocation, String> valuesFiles) {
    populateValuesFiles(appManifestMap, valuesFiles);

    List<String> result = new ArrayList<>();

    logger.info("Found Values at following sources: " + valuesFiles.keySet());

    if (valuesFiles.containsKey(K8sValuesLocation.Service)) {
      addRenderedValueToList(context, K8sValuesLocation.Service, valuesFiles.get(K8sValuesLocation.Service), result);
    }

    if (valuesFiles.containsKey(K8sValuesLocation.EnvironmentGlobal)) {
      addRenderedValueToList(
          context, K8sValuesLocation.EnvironmentGlobal, valuesFiles.get(K8sValuesLocation.EnvironmentGlobal), result);
    }

    if (valuesFiles.containsKey(K8sValuesLocation.Environment)) {
      addRenderedValueToList(
          context, K8sValuesLocation.Environment, valuesFiles.get(K8sValuesLocation.Environment), result);
    }

    return result;
  }

  private void addRenderedValueToList(
      ExecutionContext context, K8sValuesLocation location, String value, List<String> result) {
    String renderedValue = context.renderExpression(value);
    if (isNotBlank(renderedValue)) {
      result.add(renderedValue);
    } else {
      logger.info("Values content is empty in " + location);
    }
  }

  public void saveK8sElement(ExecutionContext context, K8sElement k8sElement) {
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(Scope.WORKFLOW)
                                   .name("k8s")
                                   .output(KryoUtils.asDeflatedBytes(k8sElement))
                                   .build());
  }

  public K8sElement getK8sElement(ExecutionContext context) {
    SweepingOutput sweepingOutputInput = context.prepareSweepingOutputBuilder(Scope.WORKFLOW).name("k8s").build();
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

    K8sClusterConfig k8sClusterConfig = containerDeploymentManagerHelper.getK8sClusterConfig(infraMapping);
    k8sClusterConfig.setNamespace(context.renderExpression(k8sClusterConfig.getNamespace()));

    k8sTaskParameters.setAccountId(app.getAccountId());
    k8sTaskParameters.setAppId(app.getUuid());
    k8sTaskParameters.setK8sClusterConfig(k8sClusterConfig);
    k8sTaskParameters.setWorkflowExecutionId(context.getWorkflowExecutionId());

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .taskType(TaskType.K8S_COMMAND_TASK.name())
                                    .waitId(waitId)
                                    .tags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(k8sTaskParameters))
                                    .data(TaskData.builder().parameters(new Object[] {k8sTaskParameters}).build())
                                    .envId(env.getUuid())
                                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                    .infrastructureMappingId(infraMapping.getUuid())
                                    .serviceTemplateId(serviceTemplateId)
                                    .artifactStreamId(artifactStreamId)
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

      Map<K8sValuesLocation, ApplicationManifest> appManifestMap = getApplicationManifests(context);
      boolean remoteStoreType = anyRemoteStoreType(appManifestMap);

      Activity activity = createK8sActivity(context, k8sStateExecutor.commandName(), k8sStateExecutor.stateType(),
          activityService, k8sStateExecutor.commandUnitList(remoteStoreType));

      if (remoteStoreType) {
        return executeGitTask(context, appManifestMap, activity.getUuid(), k8sStateExecutor.commandName());
      } else {
        Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
        return k8sStateExecutor.executeK8sTask(context, activity.getUuid(), valuesFiles);
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
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
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  public ExecutionResponse executeWrapperWithoutManifest(K8sStateExecutor k8sStateExecutor, ExecutionContext context) {
    try {
      k8sStateExecutor.validateParameters(context);

      Activity activity = createK8sActivity(context, k8sStateExecutor.commandName(), k8sStateExecutor.stateType(),
          activityService, k8sStateExecutor.commandUnitList(false));
      return k8sStateExecutor.executeK8sTask(context, activity.getUuid(), null);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  public InstanceElementListParam getInstanceElementListParam(List<K8sPod> podDetailsList) {
    return InstanceElementListParamBuilder.anInstanceElementListParam()
        .withInstanceElements(getInstanceElementList(podDetailsList))
        .build();
  }

  public List<InstanceStatusSummary> getInstanceStatusSummaries(
      List<InstanceElement> instanceElementList, ExecutionStatus executionStatus) {
    return instanceElementList.stream()
        .map(instanceElement
            -> anInstanceStatusSummary().withInstanceElement(instanceElement).withStatus(executionStatus).build())
        .collect(Collectors.toList());
  }

  public List<K8sPod> getPodList(
      ContainerInfrastructureMapping containerInfrastructureMapping, String namespace, String releaseName) {
    K8sInstanceSyncTaskParameters k8sInstanceSyncTaskParameters =
        K8sInstanceSyncTaskParameters.builder()
            .accountId(containerInfrastructureMapping.getAccountId())
            .appId(containerInfrastructureMapping.getAppId())
            .k8sClusterConfig(containerDeploymentManagerHelper.getK8sClusterConfig(containerInfrastructureMapping))
            .namespace(namespace)
            .releaseName(releaseName)
            .build();

    String waitId = generateUuid();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .async(true)
            .accountId(containerInfrastructureMapping.getAccountId())
            .appId(containerInfrastructureMapping.getAppId())
            .taskType(TaskType.K8S_COMMAND_TASK.name())
            .waitId(waitId)
            .tags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(k8sInstanceSyncTaskParameters))
            .data(TaskData.builder().parameters(new Object[] {k8sInstanceSyncTaskParameters}).build())
            .envId(containerInfrastructureMapping.getEnvId())
            .infrastructureMappingId(containerInfrastructureMapping.getUuid())
            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
            .build();

    try {
      K8sTaskExecutionResponse k8sTaskExecutionResponse = delegateService.executeTask(delegateTask);
      if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        K8sInstanceSyncResponse k8sInstanceSyncResponse =
            (K8sInstanceSyncResponse) k8sTaskExecutionResponse.getK8sTaskResponse();
        return k8sInstanceSyncResponse.getK8sPodInfoList();
      }
      logger.info("Failed to fetch PodList for release " + releaseName);
    } catch (Exception e) {
      logger.info("Failed to fetch PodList for release " + releaseName, e);
    }
    return null;
  }

  private List<InstanceElement> getInstanceElementList(List<K8sPod> podList) {
    if (isEmpty(podList)) {
      return Collections.emptyList();
    }

    return podList.stream()
        .map(podDetails -> {
          return anInstanceElement()
              .withUuid(podDetails.getName())
              .withHost(aHostElement().withHostName(podDetails.getName()).withIp(podDetails.getPodIP()).build())
              .withHostName(podDetails.getName())
              .withDisplayName(podDetails.getName())
              .withPodName(podDetails.getName())
              .build();
        })
        .collect(Collectors.toList());
  }

  private boolean anyRemoteStoreType(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      ApplicationManifest applicationManifest = entry.getValue();
      if (StoreType.Remote.equals(applicationManifest.getStoreType())) {
        return true;
      }
    }

    return false;
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

    Map<K8sValuesLocation, String> valuesFiles = getValuesFilesFromGitResponse(executionResponse);

    // ToDo anshul how to handle unhappy case
    return k8sStateExecutor.executeK8sTask(context, activityId, valuesFiles);
  }

  private Map<K8sValuesLocation, String> getValuesFilesFromGitResponse(GitCommandExecutionResponse executionResponse) {
    GitFetchFilesFromMultipleRepoResult gitCommandResult =
        (GitFetchFilesFromMultipleRepoResult) executionResponse.getGitCommandResult();

    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    for (Entry<String, GitFetchFilesResult> entry : gitCommandResult.getFilesFromMultipleRepo().entrySet()) {
      GitFetchFilesResult gitFetchFilesResult = entry.getValue();

      if (isNotEmpty(gitFetchFilesResult.getFiles())
          && isNotBlank(gitFetchFilesResult.getFiles().get(0).getFileContent())) {
        valuesFiles.put(
            K8sValuesLocation.valueOf(entry.getKey()), gitFetchFilesResult.getFiles().get(0).getFileContent());
      }
    }

    return valuesFiles;
  }

  private boolean isRemoteFetchRequiredForManifest(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    if (!appManifestMap.containsKey(K8sValuesLocation.Service)) {
      return true;
    }

    ApplicationManifest applicationManifest = appManifestMap.get(K8sValuesLocation.Service);
    return StoreType.Local.equals(applicationManifest.getStoreType());
  }
}
