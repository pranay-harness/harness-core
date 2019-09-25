package software.wings.sm.states.pcf;

import static com.google.common.collect.Maps.newHashMap;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.TaskType.GIT_FETCH_FILES_TASK;
import static software.wings.beans.TaskType.PCF_COMMAND_TASK;
import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.PCF_SETUP;
import static software.wings.beans.command.PcfDummyCommandUnit.FetchFiles;
import static software.wings.beans.command.PcfDummyCommandUnit.PcfSetup;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.api.pcf.PcfSetupContextElement.PcfSetupContextElementBuilder;
import software.wings.api.pcf.PcfSetupStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.PcfDummyCommandUnit;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.common.Constants;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.service.ServiceHelper;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.ApplicationManifestUtils;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PcfSetupState extends State {
  @Inject private transient AppService appService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient PcfStateHelper pcfStateHelper;
  @Inject private ServiceHelper serviceHelper;
  @Inject private transient ActivityHelperService activityHelperService;
  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private transient ApplicationManifestUtils applicationManifestUtils;

  @Inject @Transient protected transient LogService logService;

  public static final String PCF_SETUP_COMMAND = "PCF Setup";

  @Getter
  @Setter
  @DefaultValue("${app.name}__${service.name}__${env.name}")
  @Attributes(title = "PCF App Name")
  private String pcfAppName;

  @Getter @Setter private boolean useCurrentRunningCount;
  @Getter @Setter private Integer currentRunningCount;
  @Getter @Setter @Attributes(title = "Total Number of Instances", required = true) private Integer maxInstances;

  @Getter @Setter @Attributes(title = "Resize Strategy", required = true) private ResizeStrategy resizeStrategy;

  @Getter @Setter @Attributes(title = "Map Route") private String route;

  @Getter
  @Setter
  @Attributes(title = "API Timeout Interval (Minutes)")
  @DefaultValue("5")
  private Integer timeoutIntervalInMinutes = 5;

  @Getter
  @Setter
  @Attributes(title = "Active Versions to Keep")
  @DefaultValue("3")
  private Integer olderActiveVersionCountToKeep;

  private boolean blueGreen;

  public PcfSetupState(String name) {
    super(name, StateType.PCF_SETUP.name());
  }

  public PcfSetupState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    boolean valuesInGit = false;
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    Application app = appService.get(context.getAppId());

    if (featureFlagService.isEnabled(FeatureName.PCF_MANIFEST_REDESIGN, app.getAccountId())) {
      appManifestMap = applicationManifestUtils.getApplicationManifests(context, AppManifestKind.PCF_OVERRIDE);
      valuesInGit = isManifestInGit(appManifestMap);
    }

    Activity activity = createActivity(context, valuesInGit);

    if (valuesInGit) {
      return executeGitTask(context, appManifestMap, activity.getUuid());
    } else {
      return executePcfTask(context, activity.getUuid());
    }
  }

  protected ExecutionResponse executePcfTask(ExecutionContext context, String activityId) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();

    Artifact artifact = ((DeploymentExecutionContext) context).getDefaultArtifactForService(serviceElement.getUuid());
    if (artifact == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, WingsException.USER).addParam("args", "Artifact is null");
    }

    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
    // Observed NPE in alerts
    if (artifactStream == null) {
      throw new WingsException(format(
          "Unable to find artifact stream for service %s, artifact %s", serviceElement.getName(), artifact.getUuid()));
    }

    if (olderActiveVersionCountToKeep == null) {
      olderActiveVersionCountToKeep = Integer.valueOf(3);
    }

    if (olderActiveVersionCountToKeep <= 0) {
      throw new WingsException("Value for Older Active Versions To Keep Must be > 0");
    }

    PcfInfrastructureMapping pcfInfrastructureMapping =
        (PcfInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    Activity activity = updateActivity(activityId, app.getUuid(), artifact, artifactStream);

    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    PcfServiceSpecification pcfServiceSpecification =
        serviceResourceService.getPcfServiceSpecification(context.getAppId(), serviceElement.getUuid());

    if (pcfServiceSpecification == null) {
      String errorMsg = "PCF Manifest config Doesn't Exist for Service. Please create PCF Manifest";
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, errorMsg, USER).addParam("message", errorMsg);
    }

    serviceHelper.addPlaceholderTexts(pcfServiceSpecification);
    validateSpecification(pcfServiceSpecification);

    // @TODO as decided, will change to use expressions here
    // User has control to decide if for new application,
    // inframapping.routemaps to use or inframapping.temproutemaps to use.
    // so it automatically, attaches B-G deployment capability to deployment
    // if user selected tempRoutes
    boolean isOriginalRoute = false;
    String infraRouteConstLegacy = "${" + Constants.INFRA_ROUTE + "}";
    String infraRouteConst = "${" + WorkflowServiceHelper.INFRA_ROUTE_PCF + "}";
    if (route == null || infraRouteConstLegacy.equalsIgnoreCase(route.trim())
        || infraRouteConst.equalsIgnoreCase(route.trim())) {
      isOriginalRoute = true;
    }

    List<String> tempRouteMaps = CollectionUtils.isEmpty(pcfInfrastructureMapping.getTempRouteMap())
        ? emptyList()
        : pcfInfrastructureMapping.getTempRouteMap();

    List<String> routeMaps = CollectionUtils.isEmpty(pcfInfrastructureMapping.getRouteMaps())
        ? emptyList()
        : pcfInfrastructureMapping.getRouteMaps();

    // change ${app.name}__${service.name}__${env.name} to actual name.
    pcfAppName = isNotBlank(pcfAppName) ? Misc.normalizeExpression(context.renderExpression(pcfAppName))
                                        : Misc.normalizeExpression(ServiceVersionConvention.getPrefix(
                                              app.getName(), serviceElement.getName(), env.getName()));

    Map<String, String> serviceVariables = context.getServiceVariables().entrySet().stream().collect(
        Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    if (serviceVariables != null) {
      serviceVariables.replaceAll((name, value) -> context.renderExpression(value));
    }

    boolean useCliForSetup = featureFlagService.isEnabled(FeatureName.USE_PCF_CLI, app.getAccountId());
    PcfCommandRequest commandRequest =
        PcfCommandSetupRequest.builder()
            .activityId(activity.getUuid())
            .appId(app.getUuid())
            .accountId(app.getAccountId())
            .commandName(PCF_SETUP_COMMAND)
            .releaseNamePrefix(pcfAppName)
            .organization(pcfInfrastructureMapping.getOrganization())
            .space(pcfInfrastructureMapping.getSpace())
            .pcfConfig(pcfConfig)
            .pcfCommandType(PcfCommandType.SETUP)
            .manifestYaml(context.renderExpression(pcfServiceSpecification.getManifestYaml()))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .artifactFiles(artifact.getArtifactFiles())
            .routeMaps(isOriginalRoute ? routeMaps : tempRouteMaps)
            .serviceVariables(serviceVariables)
            .timeoutIntervalInMin(timeoutIntervalInMinutes == null ? Integer.valueOf(5) : timeoutIntervalInMinutes)
            .maxCount(maxInstances)
            .useCurrentCount(useCurrentRunningCount)
            .currentRunningCount(getCurrentRunningCountForSetupRequest())
            .blueGreen(blueGreen)
            .olderActiveVersionCountToKeep(
                olderActiveVersionCountToKeep == null ? Integer.valueOf(3) : olderActiveVersionCountToKeep)
            .useCLIForPcfAppCreation(useCliForSetup)
            .build();

    PcfSetupStateExecutionData stateExecutionData =
        PcfSetupStateExecutionData.builder()
            .activityId(activity.getUuid())
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .envId(env.getUuid())
            .infraMappingId(pcfInfrastructureMapping.getUuid())
            .pcfCommandRequest(commandRequest)
            .commandName(PCF_SETUP_COMMAND)
            .maxInstanceCount(maxInstances)
            .useCurrentRunningInstanceCount(useCurrentRunningCount)
            .currentRunningInstanceCount(getCurrentRunningCountForSetupRequest())
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .serviceId(serviceElement.getUuid())
            .routeMaps(routeMaps)
            .tempRouteMaps(tempRouteMaps)
            .isStandardBlueGreen(blueGreen)
            .useTempRoutes(!isOriginalRoute)
            .taskType(PCF_COMMAND_TASK)
            .build();

    String waitId = generateUuid();

    DelegateTask delegateTask =
        pcfStateHelper.getDelegateTask(app.getAccountId(), app.getUuid(), PCF_COMMAND_TASK, waitId, env.getUuid(),
            pcfInfrastructureMapping.getUuid(), new Object[] {commandRequest, encryptedDataDetails}, 5);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .correlationIds(Arrays.asList(waitId))
        .stateExecutionData(stateExecutionData)
        .async(true)
        .build();
  }

  private Integer getCurrentRunningCountForSetupRequest() {
    if (!useCurrentRunningCount) {
      return null;
    }

    if (currentRunningCount == null || currentRunningCount.intValue() == 0) {
      return Integer.valueOf(2);
    }

    return currentRunningCount;
  }

  private String getPrefix(String appName, String serviceName, String envName) {
    return appName + "_" + serviceName + "_" + envName;
  }

  private void validateSpecification(PcfServiceSpecification pcfServiceSpecification) {
    if (pcfServiceSpecification == null || pcfServiceSpecification.getManifestYaml() == null
        || !pcfServiceSpecification.getManifestYaml().contains("{FILE_LOCATION}")
        || !pcfServiceSpecification.getManifestYaml().contains("{INSTANCE_COUNT}")
        || !pcfServiceSpecification.getManifestYaml().contains("{APPLICATION_NAME}")) {
      throw new InvalidRequestException("Invalid manifest yaml "
              + (pcfServiceSpecification == null || pcfServiceSpecification.getManifestYaml() == null
                        ? "NULL"
                        : pcfServiceSpecification.getManifestYaml()),
          USER);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    PcfSetupStateExecutionData stateExecutionData = (PcfSetupStateExecutionData) context.getStateExecutionData();

    TaskType taskType = stateExecutionData.getTaskType();

    switch (taskType) {
      case GIT_FETCH_FILES_TASK:
        return handleAsyncResponseForGitTask(context, response);

      case PCF_COMMAND_TASK:
        return handleAsyncResponseForPCFTask(context, response);

      default:

        throw new InvalidRequestException("Unhandled task type " + taskType);
    }
  }

  protected ExecutionResponse handleAsyncResponseForPCFTask(
      ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = getActivityId(context);

    PcfCommandExecutionResponse executionResponse = (PcfCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    PcfSetupStateExecutionData stateExecutionData = (PcfSetupStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    PcfSetupCommandResponse pcfSetupCommandResponse =
        (PcfSetupCommandResponse) executionResponse.getPcfCommandResponse();

    PcfSetupContextElementBuilder pcfSetupContextElementBuilder =
        PcfSetupContextElement.builder()
            .serviceId(stateExecutionData.getServiceId())
            .commandName(PCF_SETUP_COMMAND)
            .maxInstanceCount(stateExecutionData.getMaxInstanceCount())
            .useCurrentRunningInstanceCount(stateExecutionData.isUseCurrentRunningInstanceCount())
            .currentRunningInstanceCount(generateCurrentRunningCount(pcfSetupCommandResponse))
            .resizeStrategy(resizeStrategy)
            .infraMappingId(stateExecutionData.getInfraMappingId())
            .pcfCommandRequest(stateExecutionData.getPcfCommandRequest())
            .isStandardBlueGreenWorkflow(stateExecutionData.isStandardBlueGreen());

    if (pcfSetupCommandResponse != null) {
      pcfSetupContextElementBuilder.timeoutIntervalInMinutes(timeoutIntervalInMinutes)
          .totalPreviousInstanceCount(
              Optional.ofNullable(pcfSetupCommandResponse.getTotalPreviousInstanceCount()).orElse(0))
          .appDetailsToBeDownsized(pcfSetupCommandResponse.getDownsizeDetails());
      if (ExecutionStatus.SUCCESS.equals(executionStatus)) {
        pcfSetupContextElementBuilder.newPcfApplicationDetails(pcfSetupCommandResponse.getNewApplicationDetails());
        addNewlyCreateRouteMapIfRequired(
            context, stateExecutionData, pcfSetupCommandResponse, pcfSetupContextElementBuilder);
      }
    }

    PcfSetupContextElement pcfSetupContextElement = pcfSetupContextElementBuilder.build();

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .contextElement(pcfSetupContextElement)
        .notifyElement(pcfSetupContextElement)
        .build();
  }

  private Integer generateCurrentRunningCount(PcfSetupCommandResponse pcfSetupCommandResponse) {
    Integer currentRunningCountFetched = pcfSetupCommandResponse.getInstanceCountForMostRecentVersion();

    if (currentRunningCountFetched == null || currentRunningCountFetched.intValue() <= 0) {
      return Integer.valueOf(2);
    }

    return currentRunningCountFetched;
  }

  private void addNewlyCreateRouteMapIfRequired(ExecutionContext context, PcfSetupStateExecutionData stateExecutionData,
      PcfSetupCommandResponse pcfSetupCommandResponse, PcfSetupContextElementBuilder pcfSetupContextElementBuilder) {
    PcfInfrastructureMapping infrastructureMapping = (PcfInfrastructureMapping) infrastructureMappingService.get(
        stateExecutionData.getAppId(), stateExecutionData.getInfraMappingId());
    boolean isInfraUpdated = false;
    if (stateExecutionData.isUseTempRoutes()) {
      List<String> routes = infrastructureMapping.getTempRouteMap();
      if (EmptyPredicate.isEmpty(routes)) {
        routes = pcfSetupCommandResponse.getNewApplicationDetails().getUrls();
        isInfraUpdated = true;
        infrastructureMapping.setTempRouteMap(routes);
      }
      pcfSetupContextElementBuilder.tempRouteMap(routes);
      stateExecutionData.setTempRouteMaps(routes);
      pcfSetupContextElementBuilder.routeMaps(infrastructureMapping.getRouteMaps());
    } else {
      List<String> routes = infrastructureMapping.getRouteMaps();
      if (EmptyPredicate.isEmpty(routes)) {
        routes = pcfSetupCommandResponse.getNewApplicationDetails().getUrls();
        isInfraUpdated = true;
        infrastructureMapping.setRouteMaps(routes);
      }
      pcfSetupContextElementBuilder.routeMaps(routes);
      stateExecutionData.setRouteMaps(routes);
      pcfSetupContextElementBuilder.tempRouteMap(infrastructureMapping.getTempRouteMap());
    }

    if (isInfraUpdated) {
      infrastructureMappingService.update(infrastructureMapping);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private Activity createActivity(ExecutionContext executionContext, boolean remoteManifestType) {
    Application app = executionContext.getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    List<CommandUnit> commandUnitList = getCommandUnitList(remoteManifestType);

    ActivityBuilder activityBuilder = pcfStateHelper.getActivityBuilder(app.getName(), app.getUuid(), PCF_SETUP_COMMAND,
        Type.Command, executionContext, getStateType(), PCF_SETUP, env, commandUnitList);

    return activityService.save(activityBuilder.build());
  }

  private Activity updateActivity(String activityId, String appId, Artifact artifact, ArtifactStream artifactStream) {
    Activity activity = activityService.get(activityId, appId);
    activity.setArtifactStreamId(artifactStream.getUuid());
    activity.setArtifactStreamName(artifactStream.getSourceName());
    activity.setArtifactName(artifact.getDisplayName());
    activity.setArtifactId(artifact.getUuid());

    return activityService.save(activity);
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = newHashMap();
    if (!useCurrentRunningCount && (maxInstances == null || maxInstances < 0)) {
      invalidFields.put("maxInstances", "Maximum instances needs to be populated");
    }

    return invalidFields;
  }

  private ExecutionResponse executeGitTask(
      ExecutionContext context, Map<K8sValuesLocation, ApplicationManifest> appManifestMap, String activityId) {
    Application app = context.getApp();
    Environment env = ((ExecutionContextImpl) context).getEnv();

    InfrastructureMapping infraMapping = infrastructureMappingService.get(app.getUuid(), context.fetchInfraMappingId());

    GitFetchFilesTaskParams fetchFilesTaskParams =
        applicationManifestUtils.createGitFetchFilesTaskParams(context, app, appManifestMap);
    fetchFilesTaskParams.setActivityId(activityId);
    fetchFilesTaskParams.setFinalState(true);
    fetchFilesTaskParams.setAppManifestKind(AppManifestKind.PCF_OVERRIDE);

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(app.getAccountId())
                                    .appId(app.getUuid())
                                    .envId(env.getUuid())
                                    .infrastructureMappingId(infraMapping.getUuid())
                                    .waitId(waitId)
                                    .async(true)
                                    .data(TaskData.builder()
                                              .taskType(GIT_FETCH_FILES_TASK.name())
                                              .parameters(new Object[] {fetchFilesTaskParams})
                                              .timeout(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT))
                                              .build())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Arrays.asList(waitId))
        .stateExecutionData(PcfSetupStateExecutionData.builder()
                                .activityId(activityId)
                                .commandName(PCF_SETUP_COMMAND)
                                .taskType(GIT_FETCH_FILES_TASK)
                                .build())
        .delegateTaskId(delegateTaskId)
        .build();
  }

  private ExecutionResponse handleAsyncResponseForGitTask(
      ExecutionContext context, Map<String, ResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String activityId = getActivityId(context);

    GitCommandExecutionResponse executionResponse = (GitCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = executionResponse.getGitCommandStatus().equals(GitCommandStatus.SUCCESS)
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;

    if (ExecutionStatus.FAILED.equals(executionStatus)) {
      activityService.updateStatus(activityId, appId, executionStatus);
      return ExecutionResponse.builder().executionStatus(executionStatus).build();
    }

    PcfSetupStateExecutionData pcfSetupStateExecutionData =
        (PcfSetupStateExecutionData) context.getStateExecutionData();

    return executePcfTask(context, activityId);
  }

  private String getActivityId(ExecutionContext context) {
    return ((PcfSetupStateExecutionData) context.getStateExecutionData()).getActivityId();
  }

  private boolean isManifestInGit(Map<K8sValuesLocation, ApplicationManifest> appManifestMap) {
    for (Entry<K8sValuesLocation, ApplicationManifest> entry : appManifestMap.entrySet()) {
      ApplicationManifest applicationManifest = entry.getValue();
      if (StoreType.Remote.equals(applicationManifest.getStoreType())) {
        return true;
      }
    }

    return false;
  }

  private List<CommandUnit> getCommandUnitList(boolean remoteStoreType) {
    List<CommandUnit> canaryCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      canaryCommandUnits.add(new PcfDummyCommandUnit(FetchFiles));
    }

    canaryCommandUnits.add(new PcfDummyCommandUnit(PcfSetup));
    return canaryCommandUnits;
  }
}
