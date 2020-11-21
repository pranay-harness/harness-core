package software.wings.sm.states.azure.appservices;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SETUP;
import static io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters.AzureAppServiceType.WEB_APP;
import static io.harness.exception.ExceptionUtils.getMessage;

import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.AZURE_APP_SERVICE_SLOT_SETUP;
import static software.wings.sm.StateType.AZURE_WEBAPP_SLOT_SETUP;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;

import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureConstants;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.Misc;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;

import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnit;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.sm.states.azure.AzureVMSSStateHelper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AzureWebAppSlotSetup extends State {
  @Getter @Setter private String slotSteadyStateTimeout;
  @Getter @Setter private List<AzureAppServiceApplicationSetting> applicationSettings;
  @Getter @Setter private List<AzureAppServiceConnectionString> appServiceConnectionStrings;
  @Getter @Setter private String appServiceSlotSetupTimeOut;
  @Inject protected transient DelegateService delegateService;
  @Inject protected transient AzureVMSSStateHelper azureVMSSStateHelper;
  @Inject protected ActivityService activityService;
  public static final String APP_SERVICE_SLOT_SETUP = "App Service Slot Setup";

  public AzureWebAppSlotSetup(String name) {
    super(name, AZURE_WEBAPP_SLOT_SETUP.name());
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis(ExecutionContext context) {
    return getTimeOut(context);
  }

  @NotNull
  private Integer getTimeOut(ExecutionContext context) {
    return azureVMSSStateHelper.renderExpressionOrGetDefault(
        slotSteadyStateTimeout, context, AzureConstants.DEFAULT_AZURE_VMSS_TIMEOUT_MIN);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    Activity activity = azureVMSSStateHelper.createAndSaveActivity(
        context, null, getStateType(), APP_SERVICE_SLOT_SETUP, AZURE_APP_SERVICE_SLOT_SETUP, getCommandUnits());
    ManagerExecutionLogCallback executionLogCallback = azureVMSSStateHelper.getExecutionLogCallback(activity);
    try {
      AzureAppServiceStateData azureAppServiceStateData = azureVMSSStateHelper.populateAzureAppServiceData(context);
      AzureTaskExecutionRequest executionRequest =
          buildTaskExecutionRequest(context, azureAppServiceStateData, activity);
      AzureAppServiceSlotSetupExecutionData stateExecutionData =
          createAndEnqueueDelegateTask(activity, context, azureAppServiceStateData, executionRequest);
      return successResponse(activity, stateExecutionData);
    } catch (Exception exception) {
      return taskCreationFailureResponse(activity, executionLogCallback, exception);
    }
  }

  private AzureTaskExecutionRequest buildTaskExecutionRequest(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    AzureWebAppSlotSetupParameters slotSetupParameters =
        buildSlotSetupParams(context, azureAppServiceStateData, activity);

    return AzureTaskExecutionRequest.builder()
        .azureConfigDTO(azureVMSSStateHelper.createAzureConfigDTO(azureAppServiceStateData.getAzureConfig()))
        .azureConfigEncryptionDetails(azureAppServiceStateData.getAzureEncryptedDataDetails())
        .azureTaskParameters(slotSetupParameters)
        .build();
  }

  private AzureWebAppSlotSetupParameters buildSlotSetupParams(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity) {
    Map<String, AzureAppServiceApplicationSetting> applicationSettingMap = new HashMap<>();
    Map<String, AzureAppServiceConnectionString> connectionStringMap = new HashMap<>();

    if (isNotEmpty(applicationSettings)) {
      applicationSettingMap = applicationSettings.stream().collect(
          Collectors.toMap(AzureAppServiceApplicationSetting::getName, setting -> setting));
    }
    if (isNotEmpty(appServiceConnectionStrings)) {
      connectionStringMap = appServiceConnectionStrings.stream().collect(
          Collectors.toMap(AzureAppServiceConnectionString::getName, setting -> setting));
    }

    return AzureWebAppSlotSetupParameters.builder()
        .accountId(azureAppServiceStateData.getApplication().getAccountId())
        .appId(azureAppServiceStateData.getApplication().getAppId())
        .commandName(APP_SERVICE_SLOT_SETUP)
        .activityId(activity.getUuid())
        .subscriptionId(azureAppServiceStateData.getSubscriptionId())
        .resourceGroupName(azureAppServiceStateData.getResourceGroup())
        .appServiceType(WEB_APP)
        .appSettings(applicationSettingMap)
        .connSettings(connectionStringMap)
        .slotName(azureAppServiceStateData.getDeploymentSlot())
        .webAppName(azureAppServiceStateData.getAppService())
        .timeoutIntervalInMin(getTimeOut(context))
        .commandType(SLOT_SETUP)
        .build();
  }

  private AzureAppServiceSlotSetupExecutionData createAndEnqueueDelegateTask(Activity activity,
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData,
      AzureTaskExecutionRequest executionRequest) {
    AzureAppServiceSlotSetupExecutionData stateExecutionData =
        AzureAppServiceSlotSetupExecutionData.builder()
            .activityId(activity.getUuid())
            .appServiceName(azureAppServiceStateData.getAppService())
            .deploySlotName(azureAppServiceStateData.getDeploymentSlot())
            .infrastructureMappingId(azureAppServiceStateData.getInfrastructureMapping().getUuid())
            .build();

    Application application = azureAppServiceStateData.getApplication();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(application.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, application.getUuid())
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.AZURE_APP_SERVICE_TASK.name())
                      .parameters(new Object[] {executionRequest})
                      .timeout(MINUTES.toMillis(getTimeoutMillis(context)))
                      .build())
            .setupAbstraction(Cd1SetupFields.ENV_ID_FIELD, azureAppServiceStateData.getEnvironment().getUuid())
            .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD,
                azureAppServiceStateData.getInfrastructureMapping().getUuid())
            .build();
    delegateService.queueTask(delegateTask);
    return stateExecutionData;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    AzureTaskExecutionResponse executionResponse = (AzureTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = azureVMSSStateHelper.getAppServieExecutionStatus(executionResponse);
    if (executionStatus == ExecutionStatus.FAILED) {
      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .errorMessage(executionResponse.getErrorMessage())
          .build();
    }

    updateActivityStatus(response, context.getAppId(), executionStatus);

    AzureAppServiceSlotSetupExecutionData stateExecutionData =
        populateSlotSetupStateExecutionData(context, executionResponse, executionStatus);

    AzureAppServiceSlotSetupContextElement slotSetupContextElement =
        buildSlotSetupContextElement(context, executionResponse);

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .contextElement(slotSetupContextElement)
        .notifyElement(slotSetupContextElement)
        .build();
  }

  private AzureAppServiceSlotSetupContextElement buildSlotSetupContextElement(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse) {
    AzureWebAppSlotSetupResponse slotSetupTaskResponse =
        (AzureWebAppSlotSetupResponse) executionResponse.getAzureTaskResponse();
    AzureAppServiceSlotSetupExecutionData stateExecutionData = context.getStateExecutionData();
    AzureAppServicePreDeploymentData preDeploymentData = slotSetupTaskResponse.getPreDeploymentData();

    return AzureAppServiceSlotSetupContextElement.builder()
        .infraMappingId(stateExecutionData.getInfrastructureMappingId())
        .appServiceSlotSetupTimeOut(getTimeOut(context))
        .commandName(APP_SERVICE_SLOT_SETUP)
        .webApp(preDeploymentData.getAppName())
        .deploymentSlot(preDeploymentData.getSlotName())
        .preDeploymentData(preDeploymentData)
        .build();
  }

  private AzureAppServiceSlotSetupExecutionData populateSlotSetupStateExecutionData(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse, ExecutionStatus executionStatus) {
    AzureWebAppSlotSetupResponse slotSetupTaskResponse =
        (AzureWebAppSlotSetupResponse) executionResponse.getAzureTaskResponse();

    AzureAppServiceSlotSetupExecutionData stateExecutionData = context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());
    stateExecutionData.setDelegateMetaInfo(executionResponse.getDelegateMetaInfo());
    stateExecutionData.setAppServiceName(slotSetupTaskResponse.getPreDeploymentData().getAppName());
    stateExecutionData.setDeploySlotName(slotSetupTaskResponse.getPreDeploymentData().getSlotName());
    return stateExecutionData;
  }

  private void updateActivityStatus(Map<String, ResponseData> response, String appId, ExecutionStatus executionStatus) {
    if (response.keySet().iterator().hasNext()) {
      String activityId = response.keySet().iterator().next();
      azureVMSSStateHelper.updateActivityStatus(appId, activityId, executionStatus);
    }
  }

  protected List<CommandUnit> getCommandUnits() {
    return ImmutableList.of();
  }

  private ExecutionResponse successResponse(Activity activity, AzureAppServiceSlotSetupExecutionData executionData) {
    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(executionData)
        .executionStatus(ExecutionStatus.SUCCESS)
        .correlationId(activity.getUuid())
        .build();
  }

  private ExecutionResponse taskCreationFailureResponse(
      Activity activity, ManagerExecutionLogCallback executionLogCallback, Exception exception) {
    log.error("Azure Web app slot setup failed - ", exception);
    Misc.logAllMessages(exception, executionLogCallback, CommandExecutionStatus.FAILURE);
    String errorMessage = getMessage(exception);
    ExecutionResponseBuilder responseBuilder = ExecutionResponse.builder();
    return responseBuilder.correlationIds(singletonList(activity.getUuid()))
        .executionStatus(FAILED)
        .errorMessage(errorMessage)
        .stateExecutionData(AzureAppServiceSlotSetupExecutionData.builder().build())
        .async(true)
        .build();
  }
}
