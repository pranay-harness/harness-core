package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.SKIPPED;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ecs.EcsBGSetupData;
import software.wings.api.ecs.EcsListenerUpdateStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;

public class EcsBGUpdateListnerState extends State {
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private ActivityService activityService;
  @Inject private EcsStateHelper ecsStateHelper;
  @Inject protected LogService logService;

  public static final String ECS_UPDATE_LISTENER_COMMAND = "ECS Update Listener Command";

  @Attributes(title = "Downsize Older Service") private boolean downsizeOldService;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public EcsBGUpdateListnerState(String name) {
    super(name, StateType.ECS_LISTENER_UPDATE.name());
  }

  public EcsBGUpdateListnerState(String name, String stateType) {
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
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application application = appService.get(context.getAppId());
    Environment environment = workflowStandardParams.getEnv();

    EcsInfrastructureMapping infrastructureMapping = (EcsInfrastructureMapping) infrastructureMappingService.get(
        application.getUuid(), context.fetchInfraMappingId());

    ContainerServiceElement containerElement = ecsStateHelper.getSetupElementFromSweepingOutput(
        context, StateType.ECS_LISTENER_UPDATE_ROLLBACK.name().equals(this.getStateType()));
    if (containerElement == null || containerElement.getEcsBGSetupData() == null) {
      return ExecutionResponse.builder()
          .executionStatus(SKIPPED)
          .stateExecutionData(aStateExecutionData().withErrorMsg("No container setup element found. Skipping.").build())
          .build();
    }

    Activity activity = createActivity(context);
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) awsConfig, context.getAppId(), context.getWorkflowExecutionId());

    EcsListenerUpdateRequestConfigData requestConfigData = getEcsListenerUpdateRequestConfigData(containerElement);

    return ecsStateHelper.queueDelegateTaskForEcsListenerUpdate(application, awsConfig, delegateService,
        infrastructureMapping, activity.getUuid(), environment.getUuid(), ECS_UPDATE_LISTENER_COMMAND,
        requestConfigData, encryptedDetails);
  }

  protected EcsListenerUpdateRequestConfigData getEcsListenerUpdateRequestConfigData(
      ContainerServiceElement containerServiceElement) {
    EcsBGSetupData ecsBGSetupData = containerServiceElement.getEcsBGSetupData();
    return EcsListenerUpdateRequestConfigData.builder()
        .clusterName(containerServiceElement.getClusterName())
        .prodListenerArn(ecsBGSetupData.getProdEcsListener())
        .stageListenerArn(ecsBGSetupData.getStageEcsListener())
        .serviceName(containerServiceElement.getNewEcsServiceName())
        .region(containerServiceElement.getEcsRegion())
        .targetGroupForNewService(containerServiceElement.getTargetGroupForNewService())
        .targetGroupForExistingService(containerServiceElement.getTargetGroupForExistingService())
        .serviceNameDownsized(containerServiceElement.getEcsBGSetupData().getDownsizedServiceName())
        .downsizeOldService(downsizeOldService)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      String activityId = response.keySet().iterator().next();

      EcsCommandExecutionResponse executionResponse = (EcsCommandExecutionResponse) response.values().iterator().next();
      ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
          ? ExecutionStatus.SUCCESS
          : ExecutionStatus.FAILED;

      activityService.updateStatus(activityId, context.getAppId(), executionStatus);

      EcsListenerUpdateStateExecutionData stateExecutionData =
          (EcsListenerUpdateStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);
      stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
      stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
      return ExecutionResponse.builder()
          .stateExecutionData(stateExecutionData)
          .errorMessage(executionResponse.getErrorMessage())
          .executionStatus(executionStatus)
          .build();

    } catch (WingsException ex) {
      throw ex;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext) {
    return ecsStateHelper.createActivity(executionContext, ECS_UPDATE_LISTENER_COMMAND, getStateType(),
        CommandUnitType.AWS_ECS_UPDATE_LISTENER_BG, activityService);
  }

  public boolean isDownsizeOldService() {
    return downsizeOldService;
  }

  public void setDownsizeOldService(boolean downsizeOldService) {
    this.downsizeOldService = downsizeOldService;
  }
}
