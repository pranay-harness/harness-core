package software.wings.sm.states.azure.appservices;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.exception.ExceptionUtils.getMessage;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.Misc;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;

import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.service.impl.azure.manager.AzureTaskExecutionRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.sm.states.azure.AzureSweepingOutputServiceHelper;
import software.wings.sm.states.azure.AzureVMSSStateHelper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public abstract class AbstractAzureAppServiceState extends State {
  @Inject protected transient DelegateService delegateService;
  @Inject protected transient AzureVMSSStateHelper azureVMSSStateHelper;
  @Inject protected transient AzureSweepingOutputServiceHelper azureSweepingOutputServiceHelper;
  @Inject protected ActivityService activityService;

  public AbstractAzureAppServiceState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis(ExecutionContext context) {
    return azureVMSSStateHelper.getStateTimeOutFromContext(context, ContextElementType.AZURE_WEBAPP_SETUP);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    Activity activity = azureVMSSStateHelper.createAndSaveActivity(
        context, null, getStateType(), commandType(), commandUnitType(), commandUnits());
    ManagerExecutionLogCallback executionLogCallback = azureVMSSStateHelper.getExecutionLogCallback(activity);
    try {
      if (!shouldExecute(context)) {
        return ExecutionResponse.builder().executionStatus(SKIPPED).errorMessage(skipMessage()).build();
      }
      AzureAppServiceStateData azureAppServiceStateData = azureVMSSStateHelper.populateAzureAppServiceData(context);

      AzureTaskExecutionRequest executionRequest =
          buildTaskExecutionRequest(context, azureAppServiceStateData, activity);

      StateExecutionData stateExecutionData =
          createAndEnqueueDelegateTask(activity, context, azureAppServiceStateData, executionRequest);

      return successResponse(activity, stateExecutionData);
    } catch (Exception exception) {
      return taskCreationFailureResponse(activity, executionLogCallback, exception);
    }
  }

  private StateExecutionData createAndEnqueueDelegateTask(Activity activity, ExecutionContext context,
      AzureAppServiceStateData azureAppServiceStateData, AzureTaskExecutionRequest executionRequest) {
    StateExecutionData stateExecutionData = buildPreStateExecutionData(activity, context, azureAppServiceStateData);
    Application application = azureAppServiceStateData.getApplication();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(application.getAccountId())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, application.getUuid())
            .waitId(activity.getUuid())
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
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, ResponseData> response) {
    AzureTaskExecutionResponse executionResponse = (AzureTaskExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus = azureVMSSStateHelper.getAppServiceExecutionStatus(executionResponse);
    ContextElement contextElement = buildContextElement(context, executionResponse);

    if (executionStatus == ExecutionStatus.FAILED) {
      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .contextElement(contextElement)
          .notifyElement(contextElement)
          .errorMessage(executionResponse.getErrorMessage())
          .build();
    }

    updateActivityStatus(response, context.getAppId(), executionStatus);
    StateExecutionData stateExecutionData = buildPostStateExecutionData(context, executionResponse, executionStatus);
    emitAnyDataForExternalConsumption(context, executionResponse);

    return ExecutionResponse.builder()
        .executionStatus(executionStatus)
        .errorMessage(executionResponse.getErrorMessage())
        .stateExecutionData(stateExecutionData)
        .contextElement(contextElement)
        .notifyElement(contextElement)
        .build();
  }

  protected void emitAnyDataForExternalConsumption(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse) {
    log.info(String.format("Nothing to save for external consumption - [%s]", getName()));
  }

  protected boolean shouldExecute(ExecutionContext context) {
    return verifyIfContextElementExist(context);
  }

  protected boolean verifyIfContextElementExist(ExecutionContext context) {
    ContextElement contextElement = context.getContextElement(ContextElementType.AZURE_WEBAPP_SETUP);
    if (!(contextElement instanceof AzureAppServiceSlotSetupContextElement)) {
      if (isRollback()) {
        return false;
      }
      throw new InvalidRequestException("Did not find Setup element of class AzureAppServiceSlotSetupContextElement");
    }
    return true;
  }

  protected AzureAppServiceSlotSetupContextElement getContextElement(ExecutionContext context) {
    ContextElement contextElement = context.getContextElement(ContextElementType.AZURE_WEBAPP_SETUP);
    return (AzureAppServiceSlotSetupContextElement) contextElement;
  }

  protected abstract AzureTaskExecutionRequest buildTaskExecutionRequest(
      ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData, Activity activity);

  protected abstract StateExecutionData buildPreStateExecutionData(
      Activity activity, ExecutionContext context, AzureAppServiceStateData azureAppServiceStateData);

  protected abstract StateExecutionData buildPostStateExecutionData(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse, ExecutionStatus executionStatus);

  protected abstract ContextElement buildContextElement(
      ExecutionContext context, AzureTaskExecutionResponse executionResponse);

  protected abstract List<CommandUnit> commandUnits();

  @NotNull protected abstract CommandUnitType commandUnitType();

  protected abstract String commandType();

  @NotNull protected abstract String errorMessageTag();

  public String skipMessage() {
    return "No Azure App service setup context element found. Skipping current step";
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // Do nothing on abort
  }

  private ExecutionResponse successResponse(Activity activity, StateExecutionData executionData) {
    return ExecutionResponse.builder()
        .async(true)
        .stateExecutionData(executionData)
        .executionStatus(ExecutionStatus.SUCCESS)
        .correlationId(activity.getUuid())
        .build();
  }

  private ExecutionResponse taskCreationFailureResponse(
      Activity activity, ManagerExecutionLogCallback executionLogCallback, Exception exception) {
    log.error(errorMessageTag() + " - ", exception);
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

  private void updateActivityStatus(Map<String, ResponseData> response, String appId, ExecutionStatus executionStatus) {
    if (response.keySet().iterator().hasNext()) {
      String activityId = response.keySet().iterator().next();
      azureVMSSStateHelper.updateActivityStatus(appId, activityId, executionStatus);
    }
  }
}
