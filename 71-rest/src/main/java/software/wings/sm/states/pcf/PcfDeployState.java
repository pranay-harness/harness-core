package software.wings.sm.states.pcf;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyList;
import static software.wings.beans.InstanceUnitType.PERCENTAGE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.PhaseElement;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.common.Constants;
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
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PcfDeployState extends State {
  @Inject private transient AppService appService;
  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient PcfStateHelper pcfStateHelper;

  @Attributes(title = "Desired Instances(cumulative)", required = true) private Integer instanceCount;
  @Attributes(title = "Instance Unit Type", required = true)
  private InstanceUnitType instanceUnitType = InstanceUnitType.PERCENTAGE;
  @Attributes(title = "Desired Instances- Old version") private Integer downsizeInstanceCount;
  @Attributes(title = "Instance Unit Type")
  private InstanceUnitType downsizeInstanceUnitType = InstanceUnitType.PERCENTAGE;
  public static final String PCF_RESIZE_COMMAND = "PCF Resize";

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
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();

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
        (EncryptableSetting) pcfConfig, context.getAppId(), context.getWorkflowExecutionId());

    Integer upsizeUpdateCount = getUpsizeUpdateCount(pcfSetupContextElement);
    Integer downsizeUpdateCount = getDownsizeUpdateCount(upsizeUpdateCount, pcfSetupContextElement);

    PcfDeployStateExecutionData stateExecutionData =
        getPcfDeployStateExecutionData(pcfSetupContextElement, activity, upsizeUpdateCount, downsizeUpdateCount);

    PcfCommandRequest commandRequest = getPcfCommandRequest(context, app, activity.getUuid(), pcfSetupContextElement,
        pcfConfig, upsizeUpdateCount, downsizeUpdateCount, stateExecutionData, pcfInfrastructureMapping);

    DelegateTask task =
        pcfStateHelper.getDelegateTask(app.getAccountId(), app.getUuid(), TaskType.PCF_COMMAND_TASK, activity.getUuid(),
            env.getUuid(), pcfInfrastructureMapping.getUuid(), new Object[] {commandRequest, encryptedDataDetails}, 5);

    delegateService.queueTask(task);

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
        .releaseName(pcfSetupContextElement.getNewPcfApplicationDetails().getApplicationName())
        .updateCount(upsizeUpdateCount)
        .updateDetails(new StringBuilder()
                           .append("{Name: ")
                           .append(pcfSetupContextElement.getNewPcfApplicationDetails().getApplicationName())
                           .append(", DesiredCount: ")
                           .append(upsizeUpdateCount)
                           .append("}")
                           .toString())
        .setupContextElement(pcfSetupContextElement)
        .build();
  }

  protected Integer getUpsizeUpdateCount(PcfSetupContextElement pcfSetupContextElement) {
    Integer count = pcfSetupContextElement.isUseCurrentRunningInstanceCount()
        ? pcfSetupContextElement.getCurrentRunningInstanceCount()
        : pcfSetupContextElement.getMaxInstanceCount();
    return getInstanceCountToBeUpdated(count, instanceCount, instanceUnitType, true);
  }

  @VisibleForTesting
  protected Integer getDownsizeUpdateCount(Integer updateCount, PcfSetupContextElement pcfSetupContextElement) {
    // if downsizeInstanceCount is not set, use same updateCount as upsize
    Integer downsizeUpdateCount = updateCount;

    Integer instanceCount = pcfSetupContextElement.isUseCurrentRunningInstanceCount()
        ? pcfSetupContextElement.getCurrentRunningInstanceCount()
        : pcfSetupContextElement.getMaxInstanceCount();
    if (downsizeInstanceCount != null) {
      downsizeUpdateCount =
          getInstanceCountToBeUpdated(instanceCount, downsizeInstanceCount, downsizeInstanceUnitType, false);
    }

    return downsizeUpdateCount;
  }

  private Integer getInstanceCountToBeUpdated(
      Integer maxInstanceCount, Integer instanceCountValue, InstanceUnitType unitType, boolean upsize) {
    // final count after upsize or downsize in this deploy phase
    Integer updateCount;
    if (unitType == PERCENTAGE) {
      int percent = Math.min(instanceCountValue, 100);
      int count = (int) Math.round((percent * maxInstanceCount) / 100.0);
      if (upsize) {
        // if use inputs 40%, means count after this phase deployment should be 40% of maxInstances
        updateCount = Math.max(count, 1);
      } else {
        // if use inputs 40%, means 60% (100 - 40) of maxInstances should be downsized for old apps
        // so only 40% of the max instances would remain
        updateCount = Math.max(count, 0);
        updateCount = maxInstanceCount - updateCount;
      }
    } else {
      if (upsize) {
        // if use inputs 5, means count after this phase deployment should be 5
        updateCount = instanceCountValue;
      } else {
        // if use inputs 5, means count after this phase deployment for old apps should be,
        // so manxInstances - 5 should be downsized
        updateCount = maxInstanceCount - instanceCountValue;
      }
    }
    return updateCount;
  }

  protected PcfCommandRequest getPcfCommandRequest(ExecutionContext context, Application application, String activityId,
      PcfSetupContextElement pcfSetupContextElement, PcfConfig pcfConfig, Integer updateCount,
      Integer downsizeUpdateCount, PcfDeployStateExecutionData stateExecutionData,
      PcfInfrastructureMapping infrastructureMapping) {
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
                ? Integer.valueOf(0)
                : pcfSetupContextElement.getTotalPreviousInstanceCount())
        .resizeStrategy(pcfSetupContextElement.getResizeStrategy())
        .instanceData(emptyList())
        .routeMaps(pcfSetupContextElement.getRouteMaps())
        .appId(application.getUuid())
        .accountId(application.getAccountId())
        .newReleaseName(pcfSetupContextElement.getNewPcfApplicationDetails().getApplicationName())
        .timeoutIntervalInMin(pcfSetupContextElement.getTimeoutIntervalInMinutes())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
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
            .withInstanceElements(emptyList())
            .withPcfInstanceElements(pcfDeployCommandResponse.getPcfInstanceElements())
            .build();

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    ActivityBuilder activityBuilder = pcfStateHelper.getActivityBuilder(app.getName(), app.getUuid(),
        PCF_RESIZE_COMMAND, Type.Command, executionContext, getStateType(), CommandUnitType.PCF_RESIZE, env);

    return activityService.save(activityBuilder.build());
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = newHashMap();
    if (instanceCount == null || instanceCount < 0) {
      invalidFields.put("instanceCount", "Instance count needs to be populated");
    }
    return invalidFields;
  }
}
