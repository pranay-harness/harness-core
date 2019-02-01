package software.wings.sm.states.pcf;

import static com.google.common.collect.Maps.newHashMap;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import software.wings.beans.DelegateTask;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.common.Constants;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.ServiceHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  @Inject @Transient protected transient LogService logService;
  @DefaultValue("${app.name}__${service.name}__${env.name}")
  @Attributes(title = "PCF App Name")
  private String pcfAppName;

  private boolean useCurrentRunningCount;
  private Integer currentRunningCount;
  @Attributes(title = "Total Number of Instances", required = true) private Integer maxInstances;

  @Attributes(title = "Resize Strategy", required = true) private ResizeStrategy resizeStrategy;

  @Attributes(title = "Map Route") private String route;

  @Attributes(title = "API Timeout Interval (Minutes)") @DefaultValue("5") private Integer timeoutIntervalInMinutes = 5;

  @Attributes(title = "Active Versions to Keep") @DefaultValue("3") private Integer olderActiveVersionCountToKeep;

  public static final String PCF_SETUP_COMMAND = "PCF Setup";

  private boolean blueGreen;

  private static final Logger logger = LoggerFactory.getLogger(PcfSetupState.class);

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public PcfSetupState(String name) {
    super(name, StateType.PCF_SETUP.name());
  }

  public PcfSetupState(String name, String stateType) {
    super(name, stateType);
  }

  public Integer getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(Integer maxInstances) {
    this.maxInstances = maxInstances;
  }

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  public String getPcfAppName() {
    return pcfAppName;
  }

  public void setPcfAppName(String pcfAppName) {
    this.pcfAppName = pcfAppName;
  }

  public Integer getTimeoutIntervalInMinutes() {
    return timeoutIntervalInMinutes;
  }

  public void setTimeoutIntervalInMinutes(Integer timeoutIntervalInMinutes) {
    this.timeoutIntervalInMinutes = timeoutIntervalInMinutes;
  }

  public String getRoute() {
    return route;
  }

  public void setRoute(String route) {
    this.route = route;
  }

  public Integer getOlderActiveVersionCountToKeep() {
    return olderActiveVersionCountToKeep;
  }

  public void setOlderActiveVersionCountToKeep(Integer olderActiveVersionCountToKeep) {
    this.olderActiveVersionCountToKeep = olderActiveVersionCountToKeep;
  }

  public boolean isUseCurrentRunningCount() {
    return useCurrentRunningCount;
  }

  public void setUseCurrentRunningCount(boolean useCurrentRunningCount) {
    this.useCurrentRunningCount = useCurrentRunningCount;
  }

  public Integer getCurrentRunningCount() {
    return currentRunningCount;
  }

  public void setCurrentRunningCount(Integer currentRunningCount) {
    this.currentRunningCount = currentRunningCount;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (Exception e) {
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceElement.getUuid());
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
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
        (PcfInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());

    Activity activity = createActivity(context, artifact, artifactStream);

    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    PcfServiceSpecification pcfServiceSpecification =
        serviceResourceService.getPcfServiceSpecification(context.getAppId(), serviceElement.getUuid());

    serviceHelper.addPlaceholderTexts(pcfServiceSpecification);
    validateSpecification(pcfServiceSpecification);

    // @TODO as decided, will change to use expressions here
    // User has control to decide if for new application,
    // inframapping.routemaps to use or inframapping.temproutemaps to use.
    // so it automatically, attaches B-G deployment capability to deployment
    // if user selected tempRoutes
    boolean isOriginalRoute = false;
    String infraRouteConstLegacy = "${" + Constants.INFRA_ROUTE + "}";
    String infraRouteConst = "${" + Constants.INFRA_ROUTE_PCF + "}";
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
            .build();

    DelegateTask delegateTask =
        pcfStateHelper.getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.PCF_COMMAND_TASK, activity.getUuid(),
            env.getUuid(), pcfInfrastructureMapping.getUuid(), new Object[] {commandRequest, encryptedDataDetails}, 5);

    delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
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
      throw new InvalidRequestException(Misc.getMessage(e), e);
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();

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
            .currentRunningInstanceCount(stateExecutionData.getCurrentRunningInstanceCount())
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

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(pcfSetupContextElement)
        .addNotifyElement(pcfSetupContextElement)
        .build();
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

  protected Activity createActivity(
      ExecutionContext executionContext, Artifact artifact, ArtifactStream artifactStream) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    ActivityBuilder activityBuilder = pcfStateHelper.getActivityBuilder(app.getName(), app.getUuid(), PCF_SETUP_COMMAND,
        Type.Command, executionContext, getStateType(), CommandUnitType.PCF_SETUP, env);

    activityBuilder.artifactStreamId(artifactStream.getUuid())
        .artifactStreamName(artifactStream.getSourceName())
        .artifactName(artifact.getDisplayName())
        .artifactId(artifact.getUuid());

    return activityService.save(activityBuilder.build());
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = newHashMap();
    if (!useCurrentRunningCount && (maxInstances == null || maxInstances < 0)) {
      invalidFields.put("maxInstances", "Maximum instances needs to be populated");
    }

    return invalidFields;
  }
}
