
package software.wings.sm.states.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.manifest.ManifestHelper.normalizeFolderPath;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.K8sPodSyncException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.model.K8sPod;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
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
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.request.HelmValuesFetchTaskParameters;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig.K8sDelegateManifestConfigBuilder;
import software.wings.helpers.ext.k8s.request.K8sInstanceSyncTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sInstanceSyncResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.HelmChartConfigHelperService;
import software.wings.service.impl.KubernetesHelperService;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.k8s.kustomize.KustomizeHelper;
import software.wings.utils.ApplicationManifestUtils;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class K8sStateHelper {
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient DelegateService delegateService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private SweepingOutputService sweepingOutputService;
  @Inject private transient AwsCommandHelper awsCommandHelper;
  @Inject private transient ActivityService activityService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject GitFileConfigHelperService gitFileConfigHelperService;
  @Inject ApplicationManifestUtils applicationManifestUtils;
  @Inject private HelmChartConfigHelperService helmChartConfigHelperService;
  @Inject private ServiceTemplateHelper serviceTemplateHelper;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private KustomizeHelper kustomizeHelper;

  private static final long MIN_TASK_TIMEOUT_IN_MINUTES = 1L;
  private static final long MAX_TASK_TIMEOUT_IN_MINUTES = 120L;

  public Activity createK8sActivity(ExecutionContext executionContext, String commandName, String stateType,
      ActivityService activityService, List<CommandUnit> commandUnits) {
    Application app = ((ExecutionContextImpl) executionContext).fetchRequiredApp();
    Environment env = ((ExecutionContextImpl) executionContext).fetchRequiredEnvironment();

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

    StoreType storeType = appManifest.getStoreType();
    switch (storeType) {
      case Local:
        manifestConfigBuilder.manifestFiles(
            applicationManifestService.getManifestFilesByAppManifestId(appManifest.getAppId(), appManifest.getUuid()));
        break;

      case KustomizeSourceRepo:
        KustomizeConfig kustomizeConfig = appManifest.getKustomizeConfig();
        kustomizeHelper.renderKustomizeConfig(context, kustomizeConfig);
        manifestConfigBuilder.kustomizeConfig(kustomizeConfig);
        prepareRemoteDelegateManifestConfig(context, appManifest, manifestConfigBuilder);
        break;
      case Remote:
      case HelmSourceRepo:
        prepareRemoteDelegateManifestConfig(context, appManifest, manifestConfigBuilder);
        break;

      case HelmChartRepo:
        ApplicationManifest appManifestWithChartRepoOverrideApplied =
            applicationManifestUtils.getAppManifestByApplyingHelmChartOverride(context);
        appManifest =
            appManifestWithChartRepoOverrideApplied == null ? appManifest : appManifestWithChartRepoOverrideApplied;
        manifestConfigBuilder.helmChartConfigParams(
            helmChartConfigHelperService.getHelmChartConfigTaskParams(context, appManifest));
        break;

      default:
        unhandled(storeType);
    }

    return manifestConfigBuilder.build();
  }

  private void prepareRemoteDelegateManifestConfig(ExecutionContext context, ApplicationManifest appManifest,
      K8sDelegateManifestConfigBuilder manifestConfigBuilder) {
    ApplicationManifest appManifestWithSourceRepoOverrideApplied =
        applicationManifestUtils.getAppManifestByApplyingHelmChartOverride(context);
    appManifest =
        appManifestWithSourceRepoOverrideApplied == null ? appManifest : appManifestWithSourceRepoOverrideApplied;
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

  public ExecutionResponse executeGitTask(ExecutionContext context,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, String activityId, String commandName) {
    Application app = appService.get(context.getAppId());

    GitFetchFilesTaskParams fetchFilesTaskParams =
        applicationManifestUtils.createGitFetchFilesTaskParams(context, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(activityId);
    fetchFilesTaskParams.setAppManifestKind(AppManifestKind.VALUES);

    applicationManifestUtils.setValuesPathInGitFetchFilesTaskParams(fetchFilesTaskParams);

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .envId(getEnvIdFromExecutionContext(context))
                                    .infrastructureMappingId(getContainerInfrastructureMappingId(context))
                                    .waitId(waitId)
                                    .async(true)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.GIT_FETCH_FILES_TASK.name())
                                              .parameters(new Object[] {fetchFilesTaskParams})
                                              .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                                              .build())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    if (stateExecutionData != null) {
      valuesFiles.putAll(stateExecutionData.getValuesFiles());
    }

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Arrays.asList(waitId))
        .stateExecutionData(K8sStateExecutionData.builder()
                                .activityId(activityId)
                                .commandName(commandName)
                                .currentTaskType(TaskType.GIT_COMMAND)
                                .valuesFiles(valuesFiles)
                                .build())
        .delegateTaskId(delegateTaskId)
        .build();
  }

  public boolean doManifestsUseArtifact(String appId, String infraMappingId) {
    InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infraMapping == null) {
      throw new InvalidRequestException(
          format("Infra mapping not found for appId %s infraMappingId %s", appId, infraMappingId));
    }

    return doManifestsUseArtifactInternal(appId, infraMapping.getServiceId(), infraMapping.getEnvId());
  }

  private boolean doManifestsUseArtifactInternal(String appId, String serviceId, String envId) {
    ApplicationManifest applicationManifest = applicationManifestService.getManifestByServiceId(appId, serviceId);
    if (doesValuesFileContainArtifact(applicationManifest)) {
      return true;
    }

    applicationManifest = applicationManifestService.getAppManifest(appId, null, serviceId, AppManifestKind.VALUES);
    if (doesValuesFileContainArtifact(applicationManifest)) {
      return true;
    }

    applicationManifest = applicationManifestService.getByEnvId(appId, envId, AppManifestKind.VALUES);
    if (doesValuesFileContainArtifact(applicationManifest)) {
      return true;
    }

    applicationManifest =
        applicationManifestService.getByEnvAndServiceId(appId, envId, serviceId, AppManifestKind.VALUES);

    return doesValuesFileContainArtifact(applicationManifest);
  }

  public boolean doManifestsUseArtifact(String appId, String serviceId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      throw new InvalidRequestException(
          format("Infra mapping not found for appId %s infraMappingId %s", appId, infraDefinitionId));
    }

    return doManifestsUseArtifactInternal(appId, serviceId, infrastructureDefinition.getEnvId());
  }

  private boolean doesValuesFileContainArtifact(ApplicationManifest applicationManifest) {
    if (applicationManifest != null && StoreType.Local == applicationManifest.getStoreType()) {
      ManifestFile manifestFile =
          applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), values_filename);
      if (manifestFile != null) {
        return isValueToFindPresent(manifestFile.getFileContent(), "${artifact.");
      }
    }
    return false;
  }

  private boolean isValueToFindPresent(String fileContent, String valueToFind) {
    if (isBlank(fileContent)) {
      return false;
    }

    try (LineIterator lineIterator = new LineIterator(new StringReader(fileContent))) {
      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (isBlank(line) || line.trim().charAt(0) == '#') {
          continue;
        }
        if (line.contains(valueToFind)) {
          return true;
        }
      }
    } catch (IOException exception) {
      return false;
    }

    return false;
  }

  public void updateManifestsArtifactVariableNames(
      String appId, String infraMappingId, Set<String> serviceArtifactVariableNames) {
    InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infraMapping == null) {
      throw new InvalidRequestException(
          format("Infra mapping not found for appId %s infraMappingId %s", appId, infraMappingId));
    }

    updateManifestsArtifactVariableNames(
        appId, serviceArtifactVariableNames, infraMapping.getServiceId(), infraMapping.getEnvId());
  }

  public void updateManifestsArtifactVariableNamesInfraDefinition(
      String appId, String infraDefinitionId, Set<String> serviceArtifactVariableNames, String serviceId) {
    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      throw new InvalidRequestException(
          format("Infra Definition not found for appId %s infraDefinitionId %s", appId, infraDefinitionId));
    }

    updateManifestsArtifactVariableNames(
        appId, serviceArtifactVariableNames, serviceId, infrastructureDefinition.getEnvId());
  }

  public void updateManifestsArtifactVariableNames(
      String appId, Set<String> serviceArtifactVariableNames, String serviceId, String envId) {
    ApplicationManifest applicationManifest = applicationManifestService.getManifestByServiceId(appId, serviceId);
    updateManifestFileVariableNames(applicationManifest, serviceArtifactVariableNames);

    applicationManifest = applicationManifestService.getByEnvId(appId, envId, AppManifestKind.VALUES);
    updateManifestFileVariableNames(applicationManifest, serviceArtifactVariableNames);

    applicationManifest =
        applicationManifestService.getByEnvAndServiceId(appId, envId, serviceId, AppManifestKind.VALUES);
    updateManifestFileVariableNames(applicationManifest, serviceArtifactVariableNames);
  }

  private void updateManifestFileVariableNames(
      ApplicationManifest applicationManifest, Set<String> serviceArtifactVariableNames) {
    if (applicationManifest != null && StoreType.Local == applicationManifest.getStoreType()) {
      ManifestFile manifestFile =
          applicationManifestService.getManifestFileByFileName(applicationManifest.getUuid(), values_filename);
      if (manifestFile != null) {
        String content = manifestFile.getFileContent();
        ExpressionEvaluator.updateServiceArtifactVariableNames(content, serviceArtifactVariableNames);
      }
    }
  }

  public List<String> getRenderedValuesFiles(
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, ExecutionContext context) {
    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    if (stateExecutionData != null) {
      valuesFiles.putAll(stateExecutionData.getValuesFiles());
    }

    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);

    List<String> result = new ArrayList<>();

    logger.info("Found Values at following sources: " + valuesFiles.keySet());

    if (valuesFiles.containsKey(K8sValuesLocation.Service)) {
      addRenderedValueToList(context, K8sValuesLocation.Service, valuesFiles.get(K8sValuesLocation.Service), result);
    }

    if (valuesFiles.containsKey(K8sValuesLocation.ServiceOverride)) {
      addRenderedValueToList(
          context, K8sValuesLocation.ServiceOverride, valuesFiles.get(K8sValuesLocation.ServiceOverride), result);
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
    SweepingOutputInquiry sweepingOutputInquiry = context.prepareSweepingOutputInquiryBuilder().name("k8s").build();
    SweepingOutputInstance result = sweepingOutputService.find(sweepingOutputInquiry);
    if (result == null) {
      return null;
    }
    return (K8sElement) KryoUtils.asInflatedObject(result.getOutput());
  }

  public ContainerInfrastructureMapping getContainerInfrastructureMapping(ExecutionContext context) {
    return (ContainerInfrastructureMapping) infrastructureMappingService.get(
        context.getAppId(), context.fetchInfraMappingId());
  }

  public Environment getEnvironment(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    return workflowStandardParams.getEnv();
  }

  public ExecutionResponse queueK8sDelegateTask(ExecutionContext context, K8sTaskParameters k8sTaskParameters) {
    Application app = appService.get(context.getAppId());
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    notNullCheck("WorkflowStandardParams should not be null", workflowStandardParams);
    Environment env = workflowStandardParams.getEnv();
    ContainerInfrastructureMapping infraMapping = getContainerInfrastructureMapping(context);
    String serviceTemplateId = serviceTemplateHelper.fetchServiceTemplateId(infraMapping);

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    // NOTE: This is no longer used for multi-artifact. Here for backwards compatibility.
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceId);
    String artifactStreamId = artifact == null ? null : artifact.getArtifactStreamId();

    K8sClusterConfig k8sClusterConfig = containerDeploymentManagerHelper.getK8sClusterConfig(infraMapping);
    k8sClusterConfig.setNamespace(context.renderExpression(k8sClusterConfig.getNamespace()));
    KubernetesHelperService.validateNamespace(k8sClusterConfig.getNamespace());

    k8sTaskParameters.setAccountId(app.getAccountId());
    k8sTaskParameters.setAppId(app.getUuid());
    k8sTaskParameters.setK8sClusterConfig(k8sClusterConfig);
    k8sTaskParameters.setWorkflowExecutionId(context.getWorkflowExecutionId());
    k8sTaskParameters.setHelmVersion(serviceResourceService.getHelmVersionWithDefault(context.getAppId(), serviceId));

    long taskTimeoutInMillis = DEFAULT_ASYNC_CALL_TIMEOUT;

    if (k8sTaskParameters.getTimeoutIntervalInMin() != null) {
      long taskTimeoutInMinutes;
      if (k8sTaskParameters.getTimeoutIntervalInMin() < MIN_TASK_TIMEOUT_IN_MINUTES) {
        taskTimeoutInMinutes = MIN_TASK_TIMEOUT_IN_MINUTES;
      } else if (k8sTaskParameters.getTimeoutIntervalInMin() > MAX_TASK_TIMEOUT_IN_MINUTES) {
        taskTimeoutInMinutes = MAX_TASK_TIMEOUT_IN_MINUTES;
      } else {
        taskTimeoutInMinutes = k8sTaskParameters.getTimeoutIntervalInMin();
      }

      taskTimeoutInMillis = taskTimeoutInMinutes * 60L * 1000L;
    }

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .waitId(waitId)
                                    .tags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(k8sTaskParameters))
                                    .data(TaskData.builder()
                                              .taskType(TaskType.K8S_COMMAND_TASK.name())
                                              .parameters(new Object[] {k8sTaskParameters})
                                              .timeout(taskTimeoutInMillis)
                                              .build())
                                    .envId(env.getUuid())
                                    .infrastructureMappingId(infraMapping.getUuid())
                                    .serviceTemplateId(serviceTemplateId)
                                    .artifactStreamId(artifactStreamId)
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Arrays.asList(waitId))
        .stateExecutionData(K8sStateExecutionData.builder()
                                .activityId(k8sTaskParameters.getActivityId())
                                .commandName(k8sTaskParameters.getCommandName())
                                .namespace(k8sTaskParameters.getK8sClusterConfig().getNamespace())
                                .clusterName(k8sTaskParameters.getK8sClusterConfig().getClusterName())
                                .releaseName(k8sTaskParameters.getReleaseName())
                                .currentTaskType(TaskType.K8S_COMMAND_TASK)
                                .build())
        .delegateTaskId(delegateTaskId)
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

      Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
          applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES);

      boolean valuesInGit = isValuesInGit(appManifestMap);
      boolean valuesInHelmChartRepo = applicationManifestUtils.isValuesInHelmChartRepo(context);
      boolean kustomizeSource = applicationManifestUtils.isKustomizeSource(context);

      Activity activity = createK8sActivity(context, k8sStateExecutor.commandName(), k8sStateExecutor.stateType(),
          activityService, k8sStateExecutor.commandUnitList(valuesInGit || valuesInHelmChartRepo || kustomizeSource));

      if (valuesInHelmChartRepo) {
        return executeHelmValuesFetchTask(context, activity.getUuid(), k8sStateExecutor.commandName());
      } else if (valuesInGit) {
        return executeGitTask(context, appManifestMap, activity.getUuid(), k8sStateExecutor.commandName());
      } else {
        return k8sStateExecutor.executeK8sTask(context, activity.getUuid());
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
        case HELM_VALUES_FETCH:
          return handleAsyncResponseForHelmFetchTask(k8sStateExecutor, context, response);

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
      return k8sStateExecutor.executeK8sTask(context, activity.getUuid());
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  public InstanceElementListParam getInstanceElementListParam(List<K8sPod> podDetailsList) {
    return InstanceElementListParam.builder().instanceElements(getInstanceElementList(podDetailsList)).build();
  }

  public List<InstanceStatusSummary> getInstanceStatusSummaries(
      List<InstanceElement> instanceElementList, ExecutionStatus executionStatus) {
    return instanceElementList.stream()
        .map(instanceElement
            -> anInstanceStatusSummary().withInstanceElement(instanceElement).withStatus(executionStatus).build())
        .collect(Collectors.toList());
  }

  public List<K8sPod> tryGetPodList(
      ContainerInfrastructureMapping containerInfrastructureMapping, String namespace, String releaseName) {
    try {
      return getPodList(containerInfrastructureMapping, namespace, releaseName);
    } catch (Exception e) {
      logger.info("Failed to fetch PodList for release {}. Exception: {}.", releaseName, e);
    }
    return null;
  }

  public List<K8sPod> getPodList(ContainerInfrastructureMapping containerInfrastructureMapping, String namespace,
      String releaseName) throws K8sPodSyncException, InterruptedException {
    K8sInstanceSyncTaskParameters k8sInstanceSyncTaskParameters =
        K8sInstanceSyncTaskParameters.builder()
            .accountId(containerInfrastructureMapping.getAccountId())
            .appId(containerInfrastructureMapping.getAppId())
            .k8sClusterConfig(containerDeploymentManagerHelper.getK8sClusterConfig(containerInfrastructureMapping))
            .namespace(namespace)
            .releaseName(releaseName)
            .build();

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(false)
                                    .accountId(containerInfrastructureMapping.getAccountId())
                                    .appId(containerInfrastructureMapping.getAppId())
                                    .waitId(waitId)
                                    .tags(awsCommandHelper.getAwsConfigTagsFromK8sConfig(k8sInstanceSyncTaskParameters))
                                    .data(TaskData.builder()
                                              .taskType(TaskType.K8S_COMMAND_TASK.name())
                                              .parameters(new Object[] {k8sInstanceSyncTaskParameters})
                                              .timeout(Duration.ofMinutes(2).toMillis())
                                              .build())
                                    .envId(containerInfrastructureMapping.getEnvId())
                                    .infrastructureMappingId(containerInfrastructureMapping.getUuid())
                                    .build();

    ResponseData notifyResponseData = delegateService.executeTask(delegateTask);
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      throw new K8sPodSyncException(format("Failed to fetch PodList for release %s. Error: %s", releaseName,
          ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage()));
    } else if (notifyResponseData instanceof RemoteMethodReturnValueData
        && ((RemoteMethodReturnValueData) notifyResponseData).getException() != null) {
      throw new K8sPodSyncException(
          format("Failed to fetch PodList for release %s. Exception: %s", releaseName, notifyResponseData));
    } else if (!(notifyResponseData instanceof K8sTaskExecutionResponse)) {
      throw new UnexpectedException(format("Failed to fetch PodList for release %s. Unknown return type %s",
          releaseName, notifyResponseData.getClass().getName()));
    }

    K8sTaskExecutionResponse k8sTaskExecutionResponse = (K8sTaskExecutionResponse) notifyResponseData;
    if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      K8sInstanceSyncResponse k8sInstanceSyncResponse =
          (K8sInstanceSyncResponse) k8sTaskExecutionResponse.getK8sTaskResponse();
      return k8sInstanceSyncResponse.getK8sPodInfoList();
    }

    throw new K8sPodSyncException(format("Failed to fetch PodList for release %s. Msg: %s. Status: %s", releaseName,
        k8sTaskExecutionResponse.getErrorMessage(), k8sTaskExecutionResponse.getCommandExecutionStatus()));
  }

  private List<InstanceElement> getInstanceElementList(List<K8sPod> podList) {
    if (isEmpty(podList)) {
      return Collections.emptyList();
    }

    return podList.stream()
        .map(podDetails -> {
          return anInstanceElement()
              .uuid(podDetails.getName())
              .host(HostElement.builder().hostName(podDetails.getName()).ip(podDetails.getPodIP()).build())
              .hostName(podDetails.getName())
              .displayName(podDetails.getName())
              .podName(podDetails.getName())
              .build();
        })
        .collect(Collectors.toList());
  }

  private boolean isValuesInGit(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      ApplicationManifest applicationManifest = entry.getValue();
      if (StoreType.Remote == applicationManifest.getStoreType()
          || StoreType.HelmSourceRepo == applicationManifest.getStoreType()) {
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
    ExecutionStatus executionStatus = executionResponse.getGitCommandStatus() == GitCommandStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED == executionStatus) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return ExecutionResponse.builder().executionStatus(executionStatus).build();
    }

    Map<K8sValuesLocation, String> valuesFiles =
        applicationManifestUtils.getValuesFilesFromGitFetchFilesResponse(executionResponse);
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.getValuesFiles().putAll(valuesFiles);

    return k8sStateExecutor.executeK8sTask(context, activityId);
  }

  public String getReleaseName(ExecutionContext context, ContainerInfrastructureMapping infraMapping) {
    String releaseName = infraMapping.getReleaseName();

    if (isBlank(releaseName)) {
      releaseName = convertBase64UuidToCanonicalForm(infraMapping.getUuid());
    }

    releaseName = context.renderExpression(releaseName);
    validateReleaseName(releaseName);

    return releaseName;
  }

  private void validateReleaseName(String releaseName) {
    if (!ExpressionEvaluator.containsVariablePattern(releaseName)) {
      try {
        new ConfigMapBuilder().withNewMetadata().withName(releaseName).endMetadata().build();
      } catch (Exception e) {
        throw new InvalidArgumentsException(
            Pair.of("Release name",
                "\"" + releaseName
                    + "\" is an invalid name. Release name may only contain lowercase letters, numbers, and '-'."),
            e, USER);
      }
    }
  }

  private HelmValuesFetchTaskParameters getHelmValuesFetchTaskParameters(ExecutionContext context, String activityId) {
    ApplicationManifest applicationManifest =
        applicationManifestUtils.getAppManifestByApplyingHelmChartOverride(context);
    if (applicationManifest == null || HelmChartRepo != applicationManifest.getStoreType()) {
      return null;
    }

    return HelmValuesFetchTaskParameters.builder()
        .accountId(context.getAccountId())
        .appId(context.getAppId())
        .activityId(activityId)
        .helmChartConfigTaskParams(
            helmChartConfigHelperService.getHelmChartConfigTaskParams(context, applicationManifest))
        .workflowExecutionId(context.getWorkflowExecutionId())
        .build();
  }

  private ExecutionResponse handleAsyncResponseForHelmFetchTask(
      K8sStateExecutor k8sStateExecutor, ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String activityId = getActivityId(context);
    HelmValuesFetchTaskResponse executionResponse = (HelmValuesFetchTaskResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED == executionStatus) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return ExecutionResponse.builder().executionStatus(executionStatus).build();
    }

    if (isNotBlank(executionResponse.getValuesFileContent())) {
      K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
      k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Service, executionResponse.getValuesFileContent());
    }

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES);

    boolean valuesInGit = isValuesInGit(appManifestMap);
    if (valuesInGit) {
      return executeGitTask(context, appManifestMap, activityId, k8sStateExecutor.commandName());
    } else {
      return k8sStateExecutor.executeK8sTask(context, activityId);
    }
  }

  public ExecutionResponse executeHelmValuesFetchTask(ExecutionContext context, String activityId, String commandName) {
    Application app = appService.get(context.getAppId());
    HelmValuesFetchTaskParameters helmValuesFetchTaskParameters = getHelmValuesFetchTaskParameters(context, activityId);

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .envId(getEnvIdFromExecutionContext(context))
                                    .infrastructureMappingId(getContainerInfrastructureMappingId(context))
                                    .waitId(waitId)
                                    .async(true)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.HELM_VALUES_FETCH.name())
                                              .parameters(new Object[] {helmValuesFetchTaskParameters})
                                              .timeout(TimeUnit.MINUTES.toMillis(10))
                                              .build())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Arrays.asList(waitId))
        .stateExecutionData(K8sStateExecutionData.builder()
                                .activityId(activityId)
                                .commandName(commandName)
                                .currentTaskType(TaskType.HELM_VALUES_FETCH)
                                .build())
        .delegateTaskId(delegateTaskId)
        .build();
  }

  public Set<String> getNamespacesFromK8sPodList(List<K8sPod> k8sPodList) {
    if (isEmpty(k8sPodList)) {
      return new HashSet<>();
    }

    return k8sPodList.stream().map(K8sPod::getNamespace).collect(Collectors.toSet());
  }

  public String getContainerInfrastructureMappingId(ExecutionContext context) {
    ContainerInfrastructureMapping infrastructureMapping = getContainerInfrastructureMapping(context);
    if (infrastructureMapping == null) {
      return null;
    }

    return infrastructureMapping.getUuid();
  }

  public String getEnvIdFromExecutionContext(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (workflowStandardParams == null || workflowStandardParams.getEnv() == null) {
      return null;
    }

    return workflowStandardParams.getEnv().getUuid();
  }

  public void validateK8sV2TypeServiceUsed(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    ServiceElement serviceElement = phaseElement.getServiceElement();
    Service service = serviceResourceService.get(serviceElement.getUuid());

    if (!service.isK8sV2()) {
      throw new InvalidRequestException(format(
          "Service %s used in workflow is of incompatible type. Use Kubernetes V2 type service", service.getName()));
    }
  }
}
