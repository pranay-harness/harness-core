/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.FeatureName.IGNORE_PCF_CONNECTION_CONTEXT_CACHE;
import static io.harness.beans.FeatureName.LIMIT_PCF_THREADS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.task.helm.CustomManifestFetchTaskHelper.unzipManifestFiles;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.getFilesUnderPath;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.pcf.CfCommandUnitConstants.FetchCustomFiles;
import static io.harness.pcf.CfCommandUnitConstants.FetchGitFiles;
import static io.harness.pcf.model.ManifestType.APPLICATION_MANIFEST;
import static io.harness.pcf.model.ManifestType.AUTOSCALAR_MANIFEST;
import static io.harness.pcf.model.ManifestType.VARIABLE_MANIFEST;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.CUSTOM_SOURCE_MANIFESTS;
import static io.harness.pcf.model.PcfConstants.DEFAULT_PCF_TASK_TIMEOUT_MIN;
import static io.harness.pcf.model.PcfConstants.INSTANCE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.INSTANCE_PLACEHOLDER_TOKEN_DEPRECATED;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NO_ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTES_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.ROUTE_PLACEHOLDER_TOKEN_DEPRECATED;
import static io.harness.pcf.model.PcfConstants.VARS_YML;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.TaskType.GIT_FETCH_FILES_TASK;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.EnvironmentGlobal;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.ServiceOverride;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FileData;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskDetails;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.task.manifests.request.CustomManifestValuesFetchParams;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.CfCommandRequest.PcfCommandType;
import io.harness.delegate.task.pcf.PcfManifestsPackage;
import io.harness.delegate.task.pcf.request.CfCommandRouteUpdateRequest;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ff.FeatureFlagService;
import io.harness.filesystem.FileIo;
import io.harness.git.model.GitFile;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.Misc;
import io.harness.manifest.CustomManifestSource;
import io.harness.manifest.CustomSourceConfig;
import io.harness.pcf.PcfFileTypeChecker;
import io.harness.pcf.model.CfCliVersion;
import io.harness.pcf.model.ManifestType;
import io.harness.pcf.model.PcfConstants;

import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.PcfInstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.DeploySweepingOutputPcf;
import software.wings.api.pcf.InfoVariables;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.PcfSetupStateExecutionData;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.api.pcf.SwapRouteRollbackSweepingOutputPcf;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Log;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.PcfInfraStructure;
import software.wings.service.ServiceHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.service.mappers.artifact.CfConfigToInternalMapper;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.rollback.RollbackStateMachineGenerator;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.ApplicationManifestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
@Slf4j
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PcfStateHelper {
  public static final String WORKFLOW_STANDARD_PARAMS = "workflowStandardParams";
  public static final String CURRENT_USER = "currentUser";
  private static final Splitter lineSplitter = Splitter.onPattern("\\r?\\n").trimResults().omitEmptyStrings();

  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ServiceHelper serviceHelper;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private PcfFileTypeChecker pcfFileTypeChecker;
  @Inject private DelegateService delegateService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient ApplicationManifestUtils applicationManifestUtils;
  @Inject private transient StateExecutionService stateExecutionService;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private LogService logService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private SettingsService settingsService;
  @Inject private FileService fileService;

  public DelegateTask getDelegateTask(PcfDelegateTaskCreationData taskCreationData) {
    return DelegateTask.builder()
        .accountId(taskCreationData.getAccountId())
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, taskCreationData.getAppId())
        .waitId(taskCreationData.getWaitId())
        .data(TaskData.builder()
                  .async(true)
                  .taskType(taskCreationData.getTaskType().name())
                  .parameters(taskCreationData.getParameters())
                  .timeout(TimeUnit.MINUTES.toMillis(taskCreationData.getTimeout()))
                  .build())
        .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, taskCreationData.getEnvId())
        .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, taskCreationData.getEnvironmentType().name())
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, taskCreationData.getInfrastructureMappingId())
        .tags(ListUtils.emptyIfNull(taskCreationData.getTagList()))
        .setupAbstraction(Cd1SetupFields.SERVICE_TEMPLATE_ID_FIELD, taskCreationData.getServiceTemplateId())
        .setupAbstraction(Cd1SetupFields.SERVICE_ID_FIELD, taskCreationData.getServiceId())
        .selectionLogsTrackingEnabled(taskCreationData.isSelectionLogsTrackingEnabled())
        .description(taskCreationData.getTaskDescription())
        .build();
  }

  public List<String> getRenderedTags(ExecutionContext context, List<String> tagList) {
    final List<String> renderedTags = CollectionUtils.emptyIfNull(tagList)
                                          .stream()
                                          .map(context::renderExpression)
                                          .distinct()
                                          .collect(Collectors.toList());
    return io.harness.data.structure.ListUtils.trimStrings(renderedTags);
  }

  public Integer getStateTimeoutMillis(ExecutionContext context, Integer defaultValue, boolean isRollback) {
    SetupSweepingOutputPcf setupSweepingOutputPcf = null;
    try {
      setupSweepingOutputPcf = findSetupSweepingOutputPcf(context, isRollback);
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    if (setupSweepingOutputPcf != null && setupSweepingOutputPcf.getTimeoutIntervalInMinutes() != null) {
      return Ints.checkedCast(TimeUnit.MINUTES.toMillis(setupSweepingOutputPcf.getTimeoutIntervalInMinutes()));
    }
    return Ints.checkedCast(TimeUnit.MINUTES.toMillis(defaultValue));
  }

  public PcfRouteUpdateStateExecutionData getRouteUpdateStateExecutionData(String activityId, String appId,
      String accountId, CfCommandRequest cfCommandRequest, String commandName,
      CfRouteUpdateRequestConfigData requestConfigData, List<String> tags) {
    return PcfRouteUpdateStateExecutionData.builder()
        .activityId(activityId)
        .accountId(accountId)
        .appId(appId)
        .pcfCommandRequest(cfCommandRequest)
        .commandName(commandName)
        .pcfRouteUpdateRequestConfigData(requestConfigData)
        .tags(tags)
        .isRollback(requestConfigData.isRollback())
        .build();
  }

  public ActivityBuilder getActivityBuilder(PcfActivityBuilderCreationData activityBuilderCreationData) {
    ExecutionContext executionContext = activityBuilderCreationData.getExecutionContext();
    Environment environment = activityBuilderCreationData.getEnvironment();
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck(WORKFLOW_STANDARD_PARAMS, workflowStandardParams, USER);
    notNullCheck(CURRENT_USER, workflowStandardParams.getCurrentUser(), USER);

    return Activity.builder()
        .applicationName(activityBuilderCreationData.getAppName())
        .appId(activityBuilderCreationData.getAppId())
        .commandName(activityBuilderCreationData.getCommandName())
        .type(activityBuilderCreationData.getType())
        .workflowType(executionContext.getWorkflowType())
        .workflowExecutionName(executionContext.getWorkflowExecutionName())
        .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
        .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
        .commandType(activityBuilderCreationData.getCommandType())
        .workflowExecutionId(executionContext.getWorkflowExecutionId())
        .workflowId(executionContext.getWorkflowId())
        .commandUnits(activityBuilderCreationData.getCommandUnits())
        .status(ExecutionStatus.RUNNING)
        .commandUnitType(activityBuilderCreationData.getCommandUnitType())
        .environmentId(environment.getUuid())
        .environmentName(environment.getName())
        .environmentType(environment.getEnvironmentType())
        .triggeredBy(TriggeredBy.builder()
                         .email(workflowStandardParams.getCurrentUser().getEmail())
                         .name(workflowStandardParams.getCurrentUser().getName())
                         .build());
  }

  public ExecutionResponse queueDelegateTaskForRouteUpdate(PcfRouteUpdateQueueRequestData queueRequestData,
      SetupSweepingOutputPcf setupSweepingOutputPcf, String stateExecutionInstanceId, boolean selectionLogsEnabled,
      List<String> renderedTags) {
    Integer timeoutIntervalInMinutes = queueRequestData.getTimeoutIntervalInMinutes() == null
        ? Integer.valueOf(DEFAULT_PCF_TASK_TIMEOUT_MIN)
        : queueRequestData.getTimeoutIntervalInMinutes();
    Application app = queueRequestData.getApp();
    PcfInfrastructureMapping pcfInfrastructureMapping = queueRequestData.getPcfInfrastructureMapping();
    String activityId = queueRequestData.getActivityId();

    CfCommandRequest cfCommandRequest =
        CfCommandRouteUpdateRequest.builder()
            .pcfCommandType(PcfCommandType.UPDATE_ROUTE)
            .commandName(queueRequestData.getCommandName())
            .appId(app.getUuid())
            .accountId(app.getAccountId())
            .activityId(activityId)
            .pcfConfig(CfConfigToInternalMapper.toCfInternalConfig(queueRequestData.getPcfConfig()))
            .organization(getOrganizationFromSetupContext(setupSweepingOutputPcf))
            .space(getSpaceFromSetupContext(setupSweepingOutputPcf))
            .pcfRouteUpdateConfigData(queueRequestData.getRequestConfigData())
            .timeoutIntervalInMin(timeoutIntervalInMinutes)
            .enforceSslValidation(setupSweepingOutputPcf.isEnforceSslValidation())
            .useAppAutoscalar(setupSweepingOutputPcf.isUseAppAutoscalar())
            .useCfCLI(queueRequestData.isUseCfCli())
            .limitPcfThreads(
                featureFlagService.isEnabled(LIMIT_PCF_THREADS, queueRequestData.getPcfConfig().getAccountId()))
            .ignorePcfConnectionContextCache(featureFlagService.isEnabled(
                IGNORE_PCF_CONNECTION_CONTEXT_CACHE, queueRequestData.getPcfConfig().getAccountId()))
            .cfCliVersion(getCfCliVersionOrDefault(app.getAppId(), setupSweepingOutputPcf.getServiceId()))
            .build();

    PcfRouteUpdateStateExecutionData stateExecutionData =
        getRouteUpdateStateExecutionData(activityId, app.getUuid(), app.getAccountId(), cfCommandRequest,
            queueRequestData.getCommandName(), queueRequestData.getRequestConfigData(), renderedTags);

    DelegateTask delegateTask =
        getDelegateTask(PcfDelegateTaskCreationData.builder()
                            .waitId(queueRequestData.getActivityId())
                            .accountId(app.getAccountId())
                            .appId(app.getUuid())
                            .envId(queueRequestData.getEnvId())
                            .taskType(TaskType.PCF_COMMAND_TASK)
                            .infrastructureMappingId(pcfInfrastructureMapping.getUuid())
                            .environmentType(queueRequestData.getEnvironmentType())
                            .serviceId(pcfInfrastructureMapping.getServiceId())
                            .parameters(new Object[] {cfCommandRequest, queueRequestData.getEncryptedDataDetails()})
                            .selectionLogsTrackingEnabled(selectionLogsEnabled)
                            .taskDescription("PCF Route update task execution")
                            .timeout(timeoutIntervalInMinutes)
                            .tagList(renderedTags)
                            .build());

    delegateService.queueTask(delegateTask);
    appendDelegateTaskDetails(delegateTask, stateExecutionInstanceId);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(queueRequestData.getActivityId()))
        .stateExecutionData(stateExecutionData)
        .async(true)
        .build();
  }

  private void appendDelegateTaskDetails(DelegateTask delegateTask, String stateExecutionInstanceId) {
    if (isBlank(delegateTask.getUuid())) {
      delegateTask.setUuid(generateUuid());
    }

    stateExecutionService.appendDelegateTaskDetails(stateExecutionInstanceId,
        DelegateTaskDetails.builder()
            .delegateTaskId(delegateTask.getUuid())
            .taskDescription(delegateTask.calcDescription())
            .setupAbstractions(delegateTask.getSetupAbstractions())
            .build());
  }

  String getSpaceFromSetupContext(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    if (setupSweepingOutputPcf == null || setupSweepingOutputPcf.getPcfCommandRequest() == null) {
      return StringUtils.EMPTY;
    }
    return setupSweepingOutputPcf.getPcfCommandRequest().getSpace();
  }

  public String getOrganizationFromSetupContext(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    if (setupSweepingOutputPcf == null || setupSweepingOutputPcf.getPcfCommandRequest() == null) {
      return StringUtils.EMPTY;
    }

    return setupSweepingOutputPcf.getPcfCommandRequest().getOrganization();
  }

  public Activity createActivity(ExecutionContext executionContext, String commandName, String stateType,
      CommandUnitType commandUnitType, ActivityService activityService, List<CommandUnit> commandUnitList) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    notNullCheck("Application does not exist", app, USER);
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    notNullCheck("Environment does not exist", env, USER);

    ActivityBuilder activityBuilder = getActivityBuilder(PcfActivityBuilderCreationData.builder()
                                                             .appName(app.getName())
                                                             .appId(app.getUuid())
                                                             .environment(env)
                                                             .type(Type.Command)
                                                             .commandName(commandName)
                                                             .executionContext(executionContext)
                                                             .commandType(stateType)
                                                             .commandUnitType(commandUnitType)
                                                             .commandUnits(commandUnitList)
                                                             .build());

    return activityService.save(activityBuilder.build());
  }

  public String fetchManifestYmlString(ExecutionContext context, ServiceElement serviceElement) {
    String applicationManifestYmlContent = getManifestFromPcfServiceSpecification(context, serviceElement);
    return context.renderExpression(applicationManifestYmlContent);
  }

  @VisibleForTesting
  String getManifestFromPcfServiceSpecification(ExecutionContext context, ServiceElement serviceElement) {
    PcfServiceSpecification pcfServiceSpecification =
        serviceResourceService.getPcfServiceSpecification(context.getAppId(), serviceElement.getUuid());

    if (pcfServiceSpecification == null) {
      throw new InvalidArgumentsException(
          Pair.of("PcfServiceSpecification", "Missing for PCF Service " + serviceElement.getUuid()));
    }

    return pcfServiceSpecification.getManifestYaml();
  }

  public PcfManifestsPackage getFinalManifestFilesMap(Map<K8sValuesLocation, ApplicationManifest> appManifestMap,
      GitFetchFilesFromMultipleRepoResult fetchFilesResult,
      Map<K8sValuesLocation, Collection<String>> manifestsFromCustomSource) {
    PcfManifestsPackage pcfManifestsPackage = PcfManifestsPackage.builder().build();

    ApplicationManifest applicationManifest = appManifestMap.get(K8sValuesLocation.Service);
    updatePcfManifestFilesMap(applicationManifest, fetchFilesResult, manifestsFromCustomSource,
        K8sValuesLocation.Service, pcfManifestsPackage);

    applicationManifest = appManifestMap.get(ServiceOverride);
    updatePcfManifestFilesMap(
        applicationManifest, fetchFilesResult, manifestsFromCustomSource, ServiceOverride, pcfManifestsPackage);

    applicationManifest = appManifestMap.get(EnvironmentGlobal);
    updatePcfManifestFilesMap(
        applicationManifest, fetchFilesResult, manifestsFromCustomSource, EnvironmentGlobal, pcfManifestsPackage);

    applicationManifest = appManifestMap.get(K8sValuesLocation.Environment);
    updatePcfManifestFilesMap(applicationManifest, fetchFilesResult, manifestsFromCustomSource,
        K8sValuesLocation.Environment, pcfManifestsPackage);

    return pcfManifestsPackage;
  }

  private void updatePcfManifestFilesMap(ApplicationManifest applicationManifest,
      GitFetchFilesFromMultipleRepoResult fetchFilesResult,
      Map<K8sValuesLocation, Collection<String>> manifestsFromCustomSource, K8sValuesLocation k8sValuesLocation,
      PcfManifestsPackage pcfManifestsPackage) {
    if (applicationManifest == null) {
      return;
    }

    if (StoreType.Local == applicationManifest.getStoreType()) {
      List<ManifestFile> manifestFiles = applicationManifestService.getManifestFilesByAppManifestId(
          applicationManifest.getAppId(), applicationManifest.getUuid());

      for (ManifestFile manifestFile : manifestFiles) {
        addToPcfManifestFilesMap(manifestFile.getFileContent(), pcfManifestsPackage);
      }
    } else if (StoreType.Remote == applicationManifest.getStoreType()) {
      if (fetchFilesResult == null || isEmpty(fetchFilesResult.getFilesFromMultipleRepo())) {
        return;
      }

      GitFetchFilesResult gitFetchFilesResult =
          fetchFilesResult.getFilesFromMultipleRepo().get(k8sValuesLocation.name());
      if (gitFetchFilesResult == null || isEmpty(gitFetchFilesResult.getFiles())) {
        return;
      }

      List<GitFile> files = gitFetchFilesResult.getFiles();
      for (GitFile gitFile : files) {
        addToPcfManifestFilesMap(gitFile.getFileContent(), pcfManifestsPackage);
      }
    } else if (StoreType.CUSTOM == applicationManifest.getStoreType()) {
      if (null == manifestsFromCustomSource || isEmpty(manifestsFromCustomSource.get(k8sValuesLocation))) {
        return;
      }
      Collection<String> files = manifestsFromCustomSource.get(k8sValuesLocation);
      for (String content : files) {
        addToPcfManifestFilesMap(content, pcfManifestsPackage);
      }
    }
  }

  @VisibleForTesting
  void addToPcfManifestFilesMap(String fileContent, PcfManifestsPackage pcfManifestsPackage) {
    ManifestType manifestType = pcfFileTypeChecker.getManifestType(fileContent);
    if (manifestType == null) {
      return;
    }

    if (APPLICATION_MANIFEST == manifestType) {
      pcfManifestsPackage.setManifestYml(fileContent);
    } else if (VARIABLE_MANIFEST == manifestType) {
      if (isEmpty(pcfManifestsPackage.getVariableYmls())) {
        pcfManifestsPackage.setVariableYmls(new ArrayList<>());
      }
      pcfManifestsPackage.getVariableYmls().add(fileContent);
    } else if (AUTOSCALAR_MANIFEST == manifestType) {
      pcfManifestsPackage.setAutoscalarManifestYml(fileContent);
    }
  }

  public List<String> getRouteMaps(
      String applicationManifestYmlContent, PcfInfrastructureMapping pcfInfrastructureMapping) {
    Map<String, Object> applicationConfigMap = getApplicationYamlMap(applicationManifestYmlContent);

    // fetch Routes element from application config
    final List<Map<String, String>> routeMapsInYaml = new ArrayList<>();
    try {
      Object routeMaps = applicationConfigMap.get(ROUTES_MANIFEST_YML_ELEMENT);
      if (routeMaps != null) {
        routeMapsInYaml.addAll((List<Map<String, String>>) routeMaps);
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Invalid Route Format In Manifest");
    }

    // routes is not mentioned in Manifest
    if (isEmpty(routeMapsInYaml)) {
      List<String> infraMapRoutes = pcfInfrastructureMapping.getRouteMaps();
      // Manifest mentions no-route or route is not provided in infraMapping as well.
      if (useNoRoute(applicationConfigMap) || isEmpty(infraMapRoutes)) {
        return emptyList();
      }

      return infraMapRoutes;
    } else if (routeMapsInYaml.size() == 1) {
      Map mapForRoute = routeMapsInYaml.get(0);
      String routeValue = (String) mapForRoute.get(ROUTE_MANIFEST_YML_ELEMENT);
      List<String> routes = new ArrayList<>();
      // if manifest contains "${ROUTE_MAP}", means read from InfraMapping
      if (ROUTE_PLACEHOLDER_TOKEN_DEPRECATED.equals(routeValue)) {
        return isEmpty(pcfInfrastructureMapping.getRouteMaps()) ? emptyList() : pcfInfrastructureMapping.getRouteMaps();
      }

      // actual route value is mentioned
      routes.add(routeValue);
      return routes;
    } else {
      // more than 1 routes are mentioned, means user has mentioned multiple actual route values
      List<String> routes = new ArrayList<>();
      routeMapsInYaml.forEach(routeMap -> routes.add(routeMap.get(ROUTE_MANIFEST_YML_ELEMENT)));
      return routes;
    }
  }

  @VisibleForTesting
  boolean useNoRoute(Map application) {
    return application.containsKey(NO_ROUTE_MANIFEST_YML_ELEMENT)
        && (boolean) application.get(NO_ROUTE_MANIFEST_YML_ELEMENT);
  }

  public PcfManifestsPackage generateManifestMap(ExecutionContext context,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, ServiceElement serviceElement, String activityId) {
    PcfManifestsPackage pcfManifestsPackage;

    Service service = serviceResourceService.get(serviceElement.getUuid());
    notNullCheck("Service does not exists", service);

    PcfSetupStateExecutionData pcfSetupStateExecutionData =
        (PcfSetupStateExecutionData) context.getStateExecutionData();

    String zippedManifestFileId = null;
    if (context.getStateExecutionData() != null) {
      zippedManifestFileId = ((PcfSetupStateExecutionData) context.getStateExecutionData()).getZippedManifestFileId();
    }
    List<String> customSourceFiles = downloadAndGetCustomSourceManifestFiles(zippedManifestFileId, activityId);

    GitFetchFilesFromMultipleRepoResult filesFromMultipleRepoResult = null;
    Map<K8sValuesLocation, Collection<String>> valuesFiles = null;
    // Null means locally hosted files
    if (pcfSetupStateExecutionData != null) {
      filesFromMultipleRepoResult = pcfSetupStateExecutionData.getFetchFilesResult();
      valuesFiles = pcfSetupStateExecutionData.getValuesFiles();
      // newly extracted customSourceFiles being appended in Service Location of valuesFiles map.
      if (isNotEmpty(customSourceFiles)) {
        if (valuesFiles.containsKey(K8sValuesLocation.Service)) {
          valuesFiles.get(K8sValuesLocation.Service).addAll(customSourceFiles);
        } else {
          valuesFiles.put(K8sValuesLocation.Service, customSourceFiles);
        }
      }
      appManifestMap = pcfSetupStateExecutionData.getAppManifestMap();
    }

    pcfManifestsPackage = getFinalManifestFilesMap(appManifestMap, filesFromMultipleRepoResult, valuesFiles);
    String manifestYml = pcfManifestsPackage.getManifestYml();
    notNullCheck("Application Manifest Can not be null/blank", manifestYml);
    evaluateExpressionsInManifestTypes(context, pcfManifestsPackage);
    return pcfManifestsPackage;
  }

  @VisibleForTesting
  void evaluateExpressionsInManifestTypes(ExecutionContext context, PcfManifestsPackage pcfManifestsPackage) {
    // evaluate expression in variables.yml
    context.resetPreparedCache();
    List<String> varYmls = pcfManifestsPackage.getVariableYmls();
    if (isNotEmpty(varYmls)) {
      varYmls = varYmls.stream().map(context::renderExpression).collect(toList());
      pcfManifestsPackage.setVariableYmls(varYmls);
    }
  }

  public String fetchPcfApplicationName(PcfManifestsPackage pcfManifestsPackage, String defaultPrefix) {
    String appName = null;

    Map<String, Object> applicationYamlMap = getApplicationYamlMap(pcfManifestsPackage.getManifestYml());
    String name = (String) applicationYamlMap.get(NAME_MANIFEST_YML_ELEMENT);
    if (isBlank(name) || PcfConstants.LEGACY_NAME_PCF_MANIFEST.equals(name)) {
      return defaultPrefix;
    }

    boolean hasVarFiles = isNotEmpty(pcfManifestsPackage.getVariableYmls());

    if (!hasVarFiles) {
      appName = name;
    } else {
      appName = finalizeSubstitution(pcfManifestsPackage, name);
    }

    return appName;
  }

  String finalizeSubstitution(PcfManifestsPackage pcfManifestsPackage, String name) {
    String varName;
    String appName;
    Matcher m = Pattern.compile("\\(\\(([^)]+)\\)\\)").matcher(name);
    List<String> varFiles = pcfManifestsPackage.getVariableYmls();
    while (m.find()) {
      varName = m.group(1);
      for (int i = varFiles.size() - 1; i >= 0; i--) {
        Object value = getVaribleValue(varFiles.get(i), varName);
        if (value != null) {
          String val = value.toString();
          if (isNotBlank(val)) {
            name = name.replace("((" + varName + "))", val);
            break;
          }
        }
      }
    }
    appName = name;
    return appName;
  }

  @VisibleForTesting
  Map<String, Object> getApplicationYamlMap(String applicationManifestYmlContent) {
    Map<String, Object> yamlMap;
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      yamlMap = (Map<String, Object>) mapper.readValue(applicationManifestYmlContent, Map.class);
    } catch (Exception e) {
      throw new UnexpectedException("failed to get application Yaml Map", e);
    }

    List<Map> applicationsMaps = (List<Map>) yamlMap.get(APPLICATION_YML_ELEMENT);
    if (isEmpty(applicationsMaps)) {
      throw new InvalidArgumentsException(Pair.of("Manifest", "contains no application config"));
    }

    // Always assume, 1st app is main application being deployed.
    Map application = applicationsMaps.get(0);
    Map<String, Object> applicationConfigMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    applicationConfigMap.putAll(application);
    return applicationConfigMap;
  }

  public Object getVaribleValue(String content, String key) {
    try {
      Map<String, Object> map = null;
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      map = mapper.readValue(content, Map.class);
      return map.get(key);
    } catch (Exception e) {
      throw new UnexpectedException("Failed while trying to substitute vars yml value", e);
    }
  }

  public List<String> applyVarsYmlSubstitutionIfApplicable(
      List<String> routeMaps, PcfManifestsPackage pcfManifestsPackage) {
    if (isEmpty(pcfManifestsPackage.getVariableYmls())) {
      return routeMaps;
    }

    return routeMaps.stream()
        .filter(EmptyPredicate::isNotEmpty)
        .map(route -> applyVarsYamlVariables(route, pcfManifestsPackage))
        .collect(toList());
  }

  private String applyVarsYamlVariables(String route, PcfManifestsPackage pcfManifestsPackage) {
    if (route.contains("((") && route.contains("))")) {
      route = finalizeSubstitution(pcfManifestsPackage, route);
    }

    return route;
  }

  public Integer fetchMaxCountFromManifest(PcfManifestsPackage pcfManifestsPackage, Integer maxInstances) {
    Map<String, Object> applicationYamlMap = getApplicationYamlMap(pcfManifestsPackage.getManifestYml());
    Map<String, Object> treeMap = generateCaseInsensitiveTreeMap(applicationYamlMap);

    Object maxCount = treeMap.get(INSTANCE_MANIFEST_YML_ELEMENT);
    String maxVal;
    if (maxCount instanceof Integer) {
      maxVal = maxCount.toString();
    } else {
      maxVal = (String) maxCount;
    }

    if (isBlank(maxVal) || INSTANCE_PLACEHOLDER_TOKEN_DEPRECATED.equals(maxVal)) {
      return maxInstances;
    }

    if (maxVal.contains("((") && maxVal.contains("))")) {
      if (isEmpty(pcfManifestsPackage.getVariableYmls())) {
        throw new InvalidRequestException(
            "No Valid Variable file Found, please verify var file is present and has valid structure");
      }
      maxVal = finalizeSubstitution(pcfManifestsPackage, maxVal);
    }

    return Integer.parseInt(maxVal);
  }

  public boolean isManifestInGit(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      ApplicationManifest applicationManifest = entry.getValue();
      if (StoreType.Remote == applicationManifest.getStoreType()) {
        return true;
      }
    }

    return false;
  }

  public DelegateTask createGitFetchFileAsyncTask(ExecutionContext context,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, String activityId, boolean selectionLogsEnabled) {
    Application app = context.getApp();
    Environment env = ((ExecutionContextImpl) context).getEnv();
    notNullCheck("Environment is null", env, USER);
    InfrastructureMapping infraMapping = infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());
    notNullCheck("InfraStructureMapping is null", infraMapping, USER);
    GitFetchFilesTaskParams fetchFilesTaskParams =
        applicationManifestUtils.createGitFetchFilesTaskParams(context, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(activityId);
    fetchFilesTaskParams.setFinalState(true);
    fetchFilesTaskParams.setAppManifestKind(AppManifestKind.PCF_OVERRIDE);
    fetchFilesTaskParams.setExecutionLogName(FetchGitFiles);

    String waitId = generateUuid();
    return DelegateTask.builder()
        .accountId(app.getAccountId())
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, app.getUuid())
        .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, env.getUuid())
        .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, env.getEnvironmentType().name())
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, infraMapping.getUuid())
        .setupAbstraction(Cd1SetupFields.SERVICE_ID_FIELD, infraMapping.getServiceId())
        .selectionLogsTrackingEnabled(selectionLogsEnabled)
        .description("Fetch remote git files")
        .waitId(waitId)
        .data(TaskData.builder()
                  .async(true)
                  .taskType(GIT_FETCH_FILES_TASK.name())
                  .parameters(new Object[] {fetchFilesTaskParams})
                  .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                  .build())
        .build();
  }

  private Map<String, Object> generateCaseInsensitiveTreeMap(Map<String, Object> map) {
    Map<String, Object> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    treeMap.putAll(map);
    return treeMap;
  }

  @NotNull
  String obtainDeploySweepingOutputName(ExecutionContext context, boolean isRollback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    return isRollback ? DeploySweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseElement.getPhaseNameForRollback().trim()
                      : DeploySweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseElement.getPhaseName().trim();
  }

  @NotNull
  String obtainSetupSweepingOutputName(ExecutionContext context, boolean isRollback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    return isRollback ? SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseElement.getPhaseNameForRollback().trim()
                      : SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseElement.getPhaseName().trim();
  }

  String obtainSwapRouteSweepingOutputName(ExecutionContext context, boolean isRollback) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    return isRollback
        ? SwapRouteRollbackSweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseElement.getPhaseNameForRollback().trim()
        : SwapRouteRollbackSweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseElement.getPhaseName().trim();
  }

  SetupSweepingOutputPcf findSetupSweepingOutputPcf(ExecutionContext context, boolean isRollback) {
    SweepingOutputInquiry sweepingOutputInquiry =
        context.prepareSweepingOutputInquiryBuilder().name(obtainSetupSweepingOutputName(context, isRollback)).build();
    return findSetupSweepingOutput(context, sweepingOutputInquiry);
  }

  public void updateInfoVariables(ExecutionContext context, PcfRouteUpdateStateExecutionData stateExecutionData) {
    SweepingOutputInstance sweepingOutputInstance = sweepingOutputService.find(
        context.prepareSweepingOutputInquiryBuilder().name(InfoVariables.SWEEPING_OUTPUT_NAME).build());

    if (sweepingOutputInstance != null) {
      InfoVariables infoVariables = (InfoVariables) sweepingOutputInstance.getValue();
      sweepingOutputService.deleteById(context.getAppId(), sweepingOutputInstance.getUuid());
      infoVariables.setNewAppRoutes(stateExecutionData.getPcfRouteUpdateRequestConfigData().getFinalRoutes());
      sweepingOutputService.ensure(context.prepareSweepingOutputBuilder(getSweepingOutputScope(context))
                                       .name(InfoVariables.SWEEPING_OUTPUT_NAME)
                                       .value(infoVariables)
                                       .build());
    }
  }

  private SetupSweepingOutputPcf findSetupSweepingOutput(
      ExecutionContext context, SweepingOutputInquiry sweepingOutputInquiry) {
    SetupSweepingOutputPcf setupSweepingOutputPcf = sweepingOutputService.findSweepingOutput(sweepingOutputInquiry);
    if (setupSweepingOutputPcf == null) {
      StateExecutionInstance previousPhaseStateExecutionInstance =
          stateExecutionService.fetchPreviousPhaseStateExecutionInstance(sweepingOutputInquiry.getAppId(),
              sweepingOutputInquiry.getWorkflowExecutionId(), sweepingOutputInquiry.getStateExecutionId());
      if (previousPhaseStateExecutionInstance == null) {
        return SetupSweepingOutputPcf.builder().build();
      } else {
        if (checkSameServiceAndInfra(context, sweepingOutputInquiry, previousPhaseStateExecutionInstance)) {
          String phaseName = getPhaseNameForQuery(sweepingOutputInquiry.getAppId(),
              sweepingOutputInquiry.getWorkflowExecutionId(), previousPhaseStateExecutionInstance.getStateName());
          SweepingOutputInquiry newSweepingOutputInquiry =
              SweepingOutputInquiry.builder()
                  .appId(sweepingOutputInquiry.getAppId())
                  .workflowExecutionId(sweepingOutputInquiry.getWorkflowExecutionId())
                  .stateExecutionId(previousPhaseStateExecutionInstance.getUuid())
                  .name(SetupSweepingOutputPcf.SWEEPING_OUTPUT_NAME + phaseName)
                  .build();
          return findSetupSweepingOutput(context, newSweepingOutputInquiry);
        } else {
          throw new InvalidArgumentsException("Different Infrastructure or Service on worklflow phases");
        }
      }
    } else {
      return setupSweepingOutputPcf;
    }
  }

  private boolean checkSameServiceAndInfra(ExecutionContext context, SweepingOutputInquiry sweepingOutputInquiry,
      StateExecutionInstance previousPhaseStateExecutionInstance) {
    StateExecutionInstance stateExecutionInstance =
        stateExecutionService.fetchCurrentPhaseStateExecutionInstance(sweepingOutputInquiry.getAppId(),
            sweepingOutputInquiry.getWorkflowExecutionId(), sweepingOutputInquiry.getStateExecutionId());

    PhaseExecutionData currentPhaseExecutionData =
        stateExecutionService.fetchPhaseExecutionDataSweepingOutput(stateExecutionInstance);
    PhaseExecutionData previousPhaseExecutionData =
        stateExecutionService.fetchPhaseExecutionDataSweepingOutput(previousPhaseStateExecutionInstance);

    List<InfrastructureDefinition> prevInfraDefinitions =
        infrastructureDefinitionService.getInfraStructureDefinitionByUuids(
            previousPhaseStateExecutionInstance.getAppId(),
            Collections.singletonList(previousPhaseExecutionData.getInfraDefinitionId()));

    List<InfrastructureDefinition> currentInfraDefinitions =
        infrastructureDefinitionService.getInfraStructureDefinitionByUuids(stateExecutionInstance.getAppId(),
            Collections.singletonList(currentPhaseExecutionData.getInfraDefinitionId()));

    return isTheSameInfrastructure(context, prevInfraDefinitions, currentInfraDefinitions)
        && previousPhaseExecutionData.getServiceId().equals(currentPhaseExecutionData.getServiceId());
  }

  private boolean isTheSameInfrastructure(ExecutionContext context, List<InfrastructureDefinition> prevInfraDefinitions,
      List<InfrastructureDefinition> currentInfraDefinitions) {
    if (!prevInfraDefinitions.isEmpty() && !currentInfraDefinitions.isEmpty()) {
      PcfInfraStructure prevPcfInfraDefinition = (PcfInfraStructure) prevInfraDefinitions.get(0).getInfrastructure();
      PcfInfraStructure currentPcfInfraDefinition =
          (PcfInfraStructure) currentInfraDefinitions.get(0).getInfrastructure();

      if (prevPcfInfraDefinition != null && currentPcfInfraDefinition != null) {
        PcfConfig previousPcfConfig =
            (PcfConfig) settingsService.get(prevPcfInfraDefinition.getCloudProviderId()).getValue();
        PcfConfig currentPcfConfig =
            (PcfConfig) settingsService.get(currentPcfInfraDefinition.getCloudProviderId()).getValue();

        return previousPcfConfig.getEndpointUrl().equals(currentPcfConfig.getEndpointUrl())
            && context.renderExpression(prevPcfInfraDefinition.getOrganization())
                   .equals(context.renderExpression(currentPcfInfraDefinition.getOrganization()))
            && context.renderExpression(prevPcfInfraDefinition.getSpace())
                   .equals(context.renderExpression(currentPcfInfraDefinition.getSpace()));
      }
    }
    return false;
  }

  void populatePcfVariables(ExecutionContext context, SetupSweepingOutputPcf setupSweepingOutputPcf) {
    InfoVariables infoVariables = sweepingOutputService.findSweepingOutput(
        context.prepareSweepingOutputInquiryBuilder().name(InfoVariables.SWEEPING_OUTPUT_NAME).build());
    if (infoVariables == null) {
      Scope outputScope = getSweepingOutputScope(context);
      sweepingOutputService.save(context.prepareSweepingOutputBuilder(outputScope)
                                     .name(InfoVariables.SWEEPING_OUTPUT_NAME)
                                     .value(setupSweepingOutputPcf.fetchPcfVariableInfo())
                                     .build());
    }
  }

  private Scope getSweepingOutputScope(ExecutionContext context) {
    return workflowExecutionService.isMultiService(context.getAppId(), context.getWorkflowExecutionId())
        ? Scope.PHASE
        : Scope.WORKFLOW;
  }

  @VisibleForTesting
  String getPhaseNameForQuery(String appId, String workflowExecutionId, String name) {
    boolean isOnDemand = workflowExecutionService.checkIfOnDemand(appId, workflowExecutionId);
    if (!isOnDemand) {
      return name.trim();
    } else {
      return name
          .replace(RollbackStateMachineGenerator.STAGING_PHASE_NAME + RollbackStateMachineGenerator.WHITE_SPACE, "")
          .trim();
    }
  }

  public List<InstanceElement> generateInstanceElement(List<PcfInstanceElement> pcfInstanceElements) {
    if (isEmpty(pcfInstanceElements)) {
      return Collections.EMPTY_LIST;
    }

    return pcfInstanceElements.stream()
        .map(pcfInstanceElement
            -> InstanceElement.Builder.anInstanceElement()
                   .displayName(pcfInstanceElement.getDisplayName())
                   .uuid(pcfInstanceElement.getUuid())
                   .hostName(pcfInstanceElement.getDisplayName() + ":" + pcfInstanceElement.getInstanceIndex())
                   .newInstance(pcfInstanceElement.isNewInstance())
                   .host(
                       HostElement.builder()
                           .hostName(pcfInstanceElement.getDisplayName() + ":" + pcfInstanceElement.getInstanceIndex())
                           .pcfElement(pcfInstanceElement)
                           .build())
                   .build())
        .collect(toList());
  }

  public List<InstanceDetails> generateInstanceDetails(List<PcfInstanceElement> pcfInstanceElements) {
    if (isEmpty(pcfInstanceElements)) {
      return Collections.EMPTY_LIST;
    }

    return pcfInstanceElements.stream()
        .map(pcfInstanceElement
            -> InstanceDetails.builder()
                   .hostName(pcfInstanceElement.getName() + ":" + pcfInstanceElement.getInstanceIndex())
                   .newInstance(pcfInstanceElement.isNewInstance())
                   .instanceType(InstanceDetails.InstanceType.PCF)
                   .pcf(InstanceDetails.PCF.builder()
                            .applicationId(pcfInstanceElement.getApplicationId())
                            .instanceIndex(pcfInstanceElement.getInstanceIndex())
                            .applicationName(pcfInstanceElement.getDisplayName())
                            .build())
                   .build())
        .collect(toList());
  }

  public String removeCommentedLineFromScript(String scriptString) {
    return lineSplitter.splitToList(scriptString)
        .stream()
        .filter(line -> !line.isEmpty())
        .filter(line -> line.charAt(0) != '#')
        .collect(Collectors.joining("\n"));
  }

  @VisibleForTesting
  boolean isRollBackNotNeeded(SetupSweepingOutputPcf setupSweepingOutputPcf) {
    return setupSweepingOutputPcf == null || !setupSweepingOutputPcf.isSuccess();
  }

  @VisibleForTesting
  ExecutionResponse handleRollbackSkipped(String appId, String activityId, String commandUnitName, String logMessage) {
    Log.Builder logBuilder = Log.Builder.aLog()
                                 .appId(appId)
                                 .activityId(activityId)
                                 .commandUnitName(commandUnitName)
                                 .logLevel(INFO)
                                 .executionResult(CommandExecutionStatus.SKIPPED);
    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, activityId);

    executionLogCallback.saveExecutionLog(logMessage, CommandExecutionStatus.SKIPPED);
    Misc.logAllMessages(null, executionLogCallback, CommandExecutionStatus.SKIPPED);

    return ExecutionResponse.builder()
        .executionStatus(ExecutionStatus.SKIPPED)
        .stateExecutionData(PcfDeployStateExecutionData.builder()
                                .activityId(activityId)
                                .commandName(commandUnitName)
                                .updateDetails(new StringBuilder().append(logMessage).toString())
                                .build())
        .build();
  }

  public CfCliVersion getCfCliVersionOrDefault(final String appId, final String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    return service != null && service.getCfCliVersion() != null ? service.getCfCliVersion() : CfCliVersion.V6;
  }

  public boolean isValuesInCustomSource(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      ApplicationManifest applicationManifest = entry.getValue();
      if (StoreType.CUSTOM == applicationManifest.getStoreType()) {
        return true;
      }
    }

    return false;
  }

  public Set<String> getRenderedAndTrimmedSelectors(ExecutionContext context, List<String> delegateSelectors) {
    context.resetPreparedCache();
    if (isEmpty(delegateSelectors)) {
      return emptySet();
    }
    List<String> renderedSelectors = delegateSelectors.stream().map(context::renderExpression).collect(toList());
    List<String> trimmedSelectors = trimStrings(renderedSelectors);
    return new HashSet<>(trimmedSelectors);
  }

  private Set<String> getDelegateSelectors(ApplicationManifest applicationManifest, ExecutionContext context) {
    final Set<String> result = new HashSet<>();
    if (applicationManifest == null || applicationManifest.getCustomSourceConfig() == null) {
      return result;
    }

    result.addAll(
        getRenderedAndTrimmedSelectors(context, applicationManifest.getCustomSourceConfig().getDelegateSelectors()));
    return result;
  }

  public DelegateTask createCustomFetchValuesTask(ExecutionContext context,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, String activityId,
      boolean selectionLogsTrackingEnabled, int timeoutInMillis) {
    Application app = context.getApp();
    Environment env = ((ExecutionContextImpl) context).getEnv();
    notNullCheck("Environment is null", env, USER);
    InfrastructureMapping infraMapping = infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());
    notNullCheck("InfraStructureMapping is null", infraMapping, USER);
    CustomManifestValuesFetchParams fetchValuesParams =
        applicationManifestUtils.createCustomManifestValuesFetchParams(context, appManifestMap, VARS_YML);
    fetchValuesParams.setActivityId(activityId);
    fetchValuesParams.setCommandUnitName(FetchCustomFiles);
    fetchValuesParams.setAppId(context.getAppId());
    fetchValuesParams.setDelegateSelectors(
        getDelegateSelectors(appManifestMap.get(K8sValuesLocation.Service), context));

    ApplicationManifest applicationManifest = appManifestMap.get(K8sValuesLocation.Service);
    CustomSourceConfig customSourceConfig = null;
    if (applicationManifest != null) {
      customSourceConfig = applicationManifest.getCustomSourceConfig();
    }
    fetchValuesParams.setCustomManifestSource(customSourceConfig == null
            ? null
            : CustomManifestSource.builder()
                  .filePaths(Arrays.asList(customSourceConfig.getPath()))
                  .script(customSourceConfig.getScript())
                  .build());

    return DelegateTask.builder()
        .accountId(app.getAccountId())
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, app.getUuid())
        .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, env.getUuid())
        .setupAbstraction(Cd1SetupFields.ENV_TYPE_FIELD, env.getEnvironmentType().name())
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, infraMapping.getUuid())
        .setupAbstraction(Cd1SetupFields.SERVICE_ID_FIELD, infraMapping.getServiceId())
        .data(TaskData.builder()
                  .async(true)
                  .taskType(TaskType.CUSTOM_MANIFEST_FETCH_TASK.name())
                  .parameters(new Object[] {fetchValuesParams})
                  .timeout(timeoutInMillis)
                  .build())
        .selectionLogsTrackingEnabled(selectionLogsTrackingEnabled)
        .build();
  }

  public List<String> downloadAndGetCustomSourceManifestFiles(String zippedManifestFileId, String activityId) {
    if (isEmpty(zippedManifestFileId)) {
      return new ArrayList<>();
    }
    try {
      InputStream inputStream = fileService.openDownloadStream(zippedManifestFileId, FileBucket.CUSTOM_MANIFEST);
      ZipInputStream zipInputStream = new ZipInputStream(inputStream);
      File tempDir = Files.createTempDir();
      String fileName = CUSTOM_SOURCE_MANIFESTS + activityId;
      createDirectoryIfDoesNotExist(tempDir + fileName);
      File file = new File(tempDir, fileName);
      unzipManifestFiles(file, zipInputStream);
      List<FileData> customManifestFiles = readManifestFilesFromDirectory(file.getAbsolutePath());
      FileIo.deleteDirectoryAndItsContentIfExists(file.getAbsolutePath());
      return customManifestFiles.stream().map(FileData::getFileContent).collect(toList());
    } catch (IOException e) {
      throw new UnexpectedException("Failed to get custom source manifest files", e);
    }
  }

  public List<FileData> readManifestFilesFromDirectory(String manifestFilesDirectory) {
    List<FileData> fileDataList;
    Path directory = Paths.get(manifestFilesDirectory);

    try {
      fileDataList = getFilesUnderPath(directory.toString());
    } catch (Exception ex) {
      log.error(ExceptionUtils.getMessage(ex));
      throw new UnexpectedException("Failed to get files. Error: " + ExceptionUtils.getMessage(ex));
    }

    List<FileData> manifestFiles = new ArrayList<>();
    for (FileData fileData : fileDataList) {
      String filePath = fileData.getFilePath();
      try {
        String fileContent = new String(fileData.getFileBytes(), UTF_8);
        if (isValidManifest(fileContent)) {
          manifestFiles.add(FileData.builder().fileName(filePath).fileContent(fileContent).build());
        }
      } catch (Exception ex) {
        throw new UnexpectedException(String.format("Failed to read content of file %s. Error: %s",
            new File(filePath).getName(), ExceptionUtils.getMessage(ex)));
      }
    }

    checkDuplicateManifests(manifestFiles);

    return manifestFiles;
  }

  private void checkDuplicateManifests(List<FileData> manifestFiles) {
    Map<ManifestType, Long> fileTypeCount =
        manifestFiles.stream()
            .map(FileData::getFileContent)
            .collect(Collectors.groupingBy(pcfFileTypeChecker::getManifestType, counting()));
    verifyMultipleCount(AUTOSCALAR_MANIFEST, fileTypeCount);
    verifyMultipleCount(APPLICATION_MANIFEST, fileTypeCount);
  }

  private void verifyMultipleCount(ManifestType manifestType, Map<ManifestType, Long> fileTypeCount) {
    if (fileTypeCount.getOrDefault(manifestType, 0L) > 1) {
      throw new UnexpectedException(String.format("Found more than %d counts of %s", 1, manifestType.getDescription()));
    }
  }

  public boolean isValidManifest(String fileContent) {
    ManifestType manifestType = pcfFileTypeChecker.getManifestType(fileContent);
    return null != manifestType;
  }
}
