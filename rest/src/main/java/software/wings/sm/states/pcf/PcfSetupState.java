package software.wings.sm.states.pcf;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.api.pcf.PcfSetupStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.OrchestrationWorkflowType;
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
import software.wings.exception.WingsException;
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
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
  @Inject private ServiceHelper serviceHelper;

  @Inject @Transient protected transient LogService logService;
  @Attributes(title = "PCF Service Name") private String pcfServiceName;
  @Attributes(title = "Desired Instances", required = true) private Integer maxInstances;
  @Attributes(title = "Resize Strategy", required = true)
  private ResizeStrategy resizeStrategy = ResizeStrategy.DOWNSIZE_OLD_FIRST;
  @Attributes(title = "Api Timeout Interval(min)") private Integer timeoutIntervalInMinutes = 5;
  public static final String PCF_SETUP_COMMAND = "PCF Setup";

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

  public String getPcfServiceName() {
    return pcfServiceName;
  }

  public void setPcfServiceName(String pcfServiceName) {
    this.pcfServiceName = pcfServiceName;
  }

  public Integer getTimeoutIntervalInMinutes() {
    return timeoutIntervalInMinutes;
  }

  public void setTimeoutIntervalInMinutes(Integer timeoutIntervalInMinutes) {
    this.timeoutIntervalInMinutes = timeoutIntervalInMinutes;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    //@TODO how to check if canary ?
    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();

    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceElement.getUuid());
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    // Observed NPE in alerts
    if (artifactStream == null) {
      throw new StateExecutionException(String.format(
          "Unable to find artifact stream for service %s, artifact %s", serviceElement.getName(), artifact.getUuid()));
    }
    PcfInfrastructureMapping pcfInfrastructureMapping =
        (PcfInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());

    Activity activity = createActivity(context, artifact, artifactStream);

    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (Encryptable) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    PcfServiceSpecification pcfServiceSpecification =
        serviceResourceService.getPcfServiceSpecification(context.getAppId(), serviceElement.getUuid());

    serviceHelper.addPlaceholderTexts(pcfServiceSpecification);
    validateSpecification(pcfServiceSpecification);

    // is Blue Green deployment
    boolean isBlueGreenDeployment = OrchestrationWorkflowType.BASIC.equals(context.getOrchestrationWorkflowType());

    List<String> tempRouteMaps = CollectionUtils.isEmpty(pcfInfrastructureMapping.getTempRouteMap())
        ? Collections.EMPTY_LIST
        : pcfInfrastructureMapping.getTempRouteMap();

    List<String> routeMaps = CollectionUtils.isEmpty(pcfInfrastructureMapping.getRouteMaps())
        ? Collections.EMPTY_LIST
        : pcfInfrastructureMapping.getRouteMaps();

    pcfServiceName = isNotBlank(pcfServiceName) ? Misc.normalizeExpression(context.renderExpression(pcfServiceName))
                                                : Misc.normalizeExpression(ServiceVersionConvention.getPrefix(
                                                      app.getName(), serviceElement.getName(), env.getName()));

    PcfCommandRequest commandRequest =
        PcfCommandSetupRequest.builder()
            .activityId(activity.getUuid())
            .appId(app.getUuid())
            .accountId(app.getAccountId())
            .commandName(PCF_SETUP_COMMAND)
            .releaseNamePrefix(pcfServiceName)
            .organization(pcfInfrastructureMapping.getOrganization())
            .space(pcfInfrastructureMapping.getSpace())
            .pcfConfig(pcfConfig)
            .pcfCommandType(PcfCommandType.SETUP)
            .manifestYaml(pcfServiceSpecification.getManifestYaml())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .artifactFiles(artifact.getArtifactFiles())
            .routeMaps(isBlueGreenDeployment ? tempRouteMaps : routeMaps)
            .serviceVariables(context.getServiceVariables())
            .timeoutIntervalInMin(timeoutIntervalInMinutes == null ? 5 : timeoutIntervalInMinutes)
            .maxCount(maxInstances)
            .isBlueGreenDeployment(isBlueGreenDeployment)
            .build();

    PcfSetupStateExecutionData stateExecutionData = PcfSetupStateExecutionData.builder()
                                                        .activityId(activity.getUuid())
                                                        .accountId(app.getAccountId())
                                                        .appId(app.getUuid())
                                                        .envId(env.getUuid())
                                                        .infraMappingId(pcfInfrastructureMapping.getUuid())
                                                        .pcfCommandRequest(commandRequest)
                                                        .commandName(PCF_SETUP_COMMAND)
                                                        .maxInstanceCount(maxInstances)
                                                        .manifestYaml(pcfServiceSpecification.getManifestYaml())
                                                        .accountId(app.getAccountId())
                                                        .appId(app.getUuid())
                                                        .serviceId(serviceElement.getUuid())
                                                        .tempRouteMap(pcfInfrastructureMapping.getTempRouteMap())
                                                        .routeMaps(pcfInfrastructureMapping.getRouteMaps())
                                                        .isBlueGreenDeployment(isBlueGreenDeployment)
                                                        .build();

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.PCF_COMMAND_TASK)
                                    .withWaitId(activity.getUuid())
                                    .withParameters(new Object[] {commandRequest, encryptedDataDetails})
                                    .withEnvId(env.getUuid())
                                    .withTimeout(TimeUnit.HOURS.toMillis(1))
                                    .withInfrastructureMappingId(pcfInfrastructureMapping.getUuid())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  private String getPrefix(String appName, String serviceName, String envName) {
    return new StringBuilder().append(appName).append("_").append(serviceName).append("_").append(envName).toString();
  }

  private void validateSpecification(PcfServiceSpecification pcfServiceSpecification) {
    if (pcfServiceSpecification == null || pcfServiceSpecification.getManifestYaml() == null
        || !pcfServiceSpecification.getManifestYaml().contains("{FILE_LOCATION}")
        || !pcfServiceSpecification.getManifestYaml().contains("{INSTANCE_COUNT}")
        || !pcfServiceSpecification.getManifestYaml().contains("{APPLICATION_NAME}")) {
      throw new WingsException(ErrorCode.INVALID_REQUEST,
          "Invalid manifest yaml "
              + (pcfServiceSpecification.getManifestYaml() == null ? "NULL"
                                                                   : pcfServiceSpecification.getManifestYaml()));
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, NotifyResponseData> response) {
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

    PcfSetupContextElement pcfSetupContextElement =
        PcfSetupContextElement.builder()
            .serviceId(stateExecutionData.getServiceId())
            .commandName(PCF_SETUP_COMMAND)
            .maxInstanceCount(maxInstances)
            .resizeStrategy(resizeStrategy)
            .infraMappingId(stateExecutionData.getInfraMappingId())
            .pcfCommandRequest(stateExecutionData.getPcfCommandRequest())
            .ManifestYaml(stateExecutionData.getManifestYaml())
            .tempRouteMap(stateExecutionData.getTempRouteMap())
            .routeMaps(stateExecutionData.getRouteMaps())
            .totalPreviousInstanceCount(pcfSetupCommandResponse.getTotalPreviousInstanceCount() == null
                    ? 0
                    : pcfSetupCommandResponse.getTotalPreviousInstanceCount())
            .timeoutIntervalInMinutes(timeoutIntervalInMinutes)
            .isBlueGreenDeployment(stateExecutionData.isBlueGreenDeployment())
            .appsToBeDownsized(pcfSetupCommandResponse.getDownsizeDetails())
            .build();

    if (ExecutionStatus.SUCCESS.equals(executionStatus)) {
      pcfSetupContextElement.setNewPcfApplicationId(pcfSetupCommandResponse.getNewApplicationId());
      pcfSetupContextElement.setNewPcfApplicationName(pcfSetupCommandResponse.getNewApplicationName());
    }

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(pcfSetupContextElement)
        .addNotifyElement(pcfSetupContextElement)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(
      ExecutionContext executionContext, Artifact artifact, ArtifactStream artifactStream) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    // @TODO do we need this
    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(PCF_SETUP_COMMAND)
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING)
                                          .artifactStreamId(artifactStream.getUuid())
                                          .artifactStreamName(artifactStream.getSourceName())
                                          .artifactName(artifact.getDisplayName())
                                          .artifactId(artifact.getUuid())
                                          .commandUnitType(CommandUnitType.PCF_SETUP); // @Todo what should be used here

    activityBuilder.environmentId(env.getUuid())
        .environmentName(env.getName())
        .environmentType(env.getEnvironmentType());

    return activityService.save(activityBuilder.build());
  }
}
