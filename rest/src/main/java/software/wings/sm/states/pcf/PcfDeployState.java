package software.wings.sm.states.pcf;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.PcfInstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest.PcfCommandType;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
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
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PcfDeployState extends State {
  @Inject private transient AppService appService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ActivityService activityService;

  @Attributes(title = "Desired Instances(cumulative)", required = true) private Integer instanceCount;
  @Attributes(title = "Instance Unit Type", required = true)
  private InstanceUnitType instanceUnitType = InstanceUnitType.PERCENTAGE;
  @Attributes(title = "Desired Instances- Old version") private Integer downsizeInstanceCount;
  @Attributes(title = "Instance Unit Type")
  private InstanceUnitType downsizeInstanceUnitType = InstanceUnitType.PERCENTAGE;
  public static final String PCF_RESIZE_COMMAND = "PCF Resize";

  private static final Logger logger = LoggerFactory.getLogger(PcfDeployState.class);

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public PcfDeployState(String name) {
    super(name, StateType.PCF_RESIZE.name());
  }

  public PcfDeployState(String name, String stateType) {
    super(name, stateType);
  }

  public Integer getInstanceCount() {
    return instanceCount;
  }
  public void setInstanceCount(Integer instanceCount) {
    this.instanceCount = instanceCount;
  }
  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }
  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  public Integer getDownsizeInstanceCount() {
    return downsizeInstanceCount;
  }

  public void setDownsizeInstanceCount(Integer downsizeInstanceCount) {
    this.downsizeInstanceCount = downsizeInstanceCount;
  }

  public InstanceUnitType getDownsizeInstanceUnitType() {
    return downsizeInstanceUnitType;
  }

  public void setDownsizeInstanceUnitType(InstanceUnitType downsizeInstanceUnitType) {
    this.downsizeInstanceUnitType = downsizeInstanceUnitType;
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

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();

    PcfInfrastructureMapping pcfInfrastructureMapping =
        (PcfInfrastructureMapping) infrastructureMappingService.get(app.getUuid(), phaseElement.getInfraMappingId());

    PcfSetupContextElement pcfSetupContextElement =
        context.<PcfSetupContextElement>getContextElementList(ContextElementType.PCF_SERVICE_SETUP)
            .stream()
            .filter(cse -> phaseElement.getInfraMappingId().equals(cse.getInfraMappingId()))
            .findFirst()
            .orElse(PcfSetupContextElement.builder().build());

    Activity activity = createActivity(context);
    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (Encryptable) pcfConfig, context.getAppId(), context.getWorkflowExecutionId());

    Integer upsizeUpdateCount = getUpsizeUpdateCount(pcfSetupContextElement);
    Integer downsizeUpdateCount = getDownsizeUpdateCount(upsizeUpdateCount, pcfSetupContextElement);

    PcfDeployStateExecutionData stateExecutionData =
        getPcfDeployStateExecutionData(pcfSetupContextElement, activity, upsizeUpdateCount, downsizeUpdateCount);

    PcfCommandRequest commandRequest = getPcfCommandRequest(context, app, activity.getUuid(), pcfSetupContextElement,
        pcfConfig, upsizeUpdateCount, downsizeUpdateCount, stateExecutionData);

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

  private PcfDeployStateExecutionData getPcfDeployStateExecutionData(PcfSetupContextElement pcfSetupContextElement,
      Activity activity, Integer upsizeUpdateCount, Integer downsizeUpdateCount) {
    return PcfDeployStateExecutionData.builder()
        .activityId(activity.getUuid())
        .commandName(PCF_RESIZE_COMMAND)
        .releaseName(pcfSetupContextElement.getNewPcfApplicationName())
        .updateCount(upsizeUpdateCount)
        .updateDetails(new StringBuilder()
                           .append("{Name: ")
                           .append(pcfSetupContextElement.getNewPcfApplicationName())
                           .append(", DesiredCount: ")
                           .append(upsizeUpdateCount)
                           .append("}")
                           .toString())
        .setupContextElement(pcfSetupContextElement)
        .build();
  }

  protected Integer getUpsizeUpdateCount(PcfSetupContextElement pcfSetupContextElement) {
    return getInstanceCountToBeUpdated(
        pcfSetupContextElement.getMaxInstanceCount(), instanceCount, instanceUnitType, true);
  }

  protected Integer getDownsizeUpdateCount(Integer updateCount, PcfSetupContextElement pcfSetupContextElement) {
    // if downsizeInstanceCount is not set, use same updateCount as upsize
    Integer downsizeUpdateCount = updateCount;
    if (downsizeInstanceCount != null) {
      downsizeUpdateCount = getInstanceCountToBeUpdated(
          pcfSetupContextElement.getMaxInstanceCount(), downsizeInstanceCount, downsizeInstanceUnitType, false);
    }

    return downsizeUpdateCount;
  }

  private Integer getInstanceCountToBeUpdated(
      Integer maxInstanceCount, Integer instanceCountValue, InstanceUnitType unitType, boolean upsize) {
    Integer updateCount;
    if (unitType == PERCENTAGE) {
      int percent = Math.min(instanceCountValue, 100);
      int count = (int) Math.round((percent * maxInstanceCount) / 100.0);
      if (upsize) {
        updateCount = Math.max(count, 1);
      } else {
        updateCount = Math.max(count, 0);
      }
    } else {
      updateCount = instanceCountValue;
    }
    return updateCount;
  }

  protected PcfCommandRequest getPcfCommandRequest(ExecutionContext context, Application application, String activityId,
      PcfSetupContextElement pcfSetupContextElement, PcfConfig pcfConfig, Integer updateCount,
      Integer downsizeUpdateCount, PcfDeployStateExecutionData stateExecutionData) {
    return PcfCommandDeployRequest.builder()
        .activityId(activityId)
        .commandName(PCF_RESIZE_COMMAND)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .organization(pcfSetupContextElement.getPcfCommandRequest().getOrganization())
        .space(pcfSetupContextElement.getPcfCommandRequest().getSpace())
        .pcfConfig(pcfConfig)
        .pcfCommandType(PcfCommandType.RESIZE)
        .updateCount(updateCount)
        .downSizeCount(downsizeUpdateCount)
        .totalPreviousInstanceCount(pcfSetupContextElement.getTotalPreviousInstanceCount() == null
                ? 0
                : pcfSetupContextElement.getTotalPreviousInstanceCount())
        .resizeStrategy(pcfSetupContextElement.getResizeStrategy())
        .instanceData(Collections.EMPTY_LIST)
        .routeMaps(pcfSetupContextElement.getRouteMaps())
        .appId(application.getUuid())
        .accountId(application.getAccountId())
        .newReleaseName(pcfSetupContextElement.getNewPcfApplicationName())
        .timeoutIntervalInMin(pcfSetupContextElement.getTimeoutIntervalInMinutes())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
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

    PcfDeployCommandResponse pcfDeployCommandResponse =
        (PcfDeployCommandResponse) executionResponse.getPcfCommandResponse();

    if (pcfDeployCommandResponse.getInstanceDataUpdated() == null) {
      pcfDeployCommandResponse.setInstanceDataUpdated(new ArrayList<>());
    }

    // update PcfDeployStateExecutionData,
    PcfDeployStateExecutionData stateExecutionData = (PcfDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setInstanceData(pcfDeployCommandResponse.getInstanceDataUpdated());

    InstanceElementListParam instanceElementListParam =
        InstanceElementListParamBuilder.anInstanceElementListParam()
            .withInstanceElements(Collections.emptyList())
            .withPcfInstanceElements(getPcfInstanceElements(pcfDeployCommandResponse.getInstanceTokens()))
            .build();

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  private List<PcfInstanceElement> getPcfInstanceElements(List<String> instanceTokens) {
    if (CollectionUtils.isEmpty(instanceTokens)) {
      return Collections.EMPTY_LIST;
    }

    List<PcfInstanceElement> pcfInstanceElements = new ArrayList<>();
    for (String instanceToken : instanceTokens) {
      String[] arr = instanceToken.split(":");
      pcfInstanceElements.add(
          PcfInstanceElement.builder().applicationId(arr[0]).instanceIndex(arr[1]).displayName(instanceToken).build());
    }

    return pcfInstanceElements;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  // is right ?
  private Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(PCF_RESIZE_COMMAND)
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
                                          .commandUnitType(CommandUnitType.PCF_RESIZE);

    activityBuilder.environmentId(env.getUuid())
        .environmentName(env.getName())
        .environmentType(env.getEnvironmentType());
    return activityService.save(activityBuilder.build());
  }
}
