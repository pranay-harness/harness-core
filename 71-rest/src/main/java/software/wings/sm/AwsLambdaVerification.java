package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.api.AwsLambdaExecutionData;
import software.wings.api.AwsLambdaFunctionElement;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.LambdaTestEvent;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.LambdaConvention;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AwsLambdaVerification extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsLambdaVerification.class);

  @Attributes(title = "Function Test Events") private List<LambdaTestEvent> lambdaTestEvents = new ArrayList<>();

  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private SecretManager secretManager;
  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient private transient SettingsService settingsService;
  private static final long TIME_OUT_IN_MINUTES = 2;
  @Inject private DelegateService delegateService;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public AwsLambdaVerification(String name) {
    super(name, StateType.AWS_LAMBDA_VERIFICATION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    AwsLambdaExecutionData awsLambdaExecutionData = new AwsLambdaExecutionData();
    try {
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      Application app = workflowStandardParams.getApp();

      AwsLambdaInfraStructureMapping infrastructureMapping =
          (AwsLambdaInfraStructureMapping) infrastructureMappingService.get(
              app.getUuid(), phaseElement.getInfraMappingId());

      SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

      AwsLambdaFunctionElement awsLambdaFunctionElement =
          context.getContextElement(ContextElementType.AWS_LAMBDA_FUNCTION);
      FunctionMeta functionMeta = awsLambdaFunctionElement.getFunctionArn();

      awsLambdaExecutionData.setFunctionArn(functionMeta.getFunctionArn());
      awsLambdaExecutionData.setFunctionName(functionMeta.getFunctionName());
      awsLambdaExecutionData.setFunctionVersion(functionMeta.getVersion());

      ImmutableMap<String, LambdaTestEvent> functionNameMap = lambdaTestEvents == null
          ? ImmutableMap.of()
          : Maps.uniqueIndex(lambdaTestEvents,
                lambdaTestEvent
                -> LambdaConvention.normalizeFunctionName(context.renderExpression(lambdaTestEvent.getFunctionName())));

      LambdaTestEvent lambdaTestEvent =
          functionNameMap.getOrDefault(functionMeta.getFunctionName(), LambdaTestEvent.builder().build());

      return executeTask(awsConfig.getAccountId(),
          AwsLambdaExecuteFunctionRequest.builder()
              .awsConfig(awsConfig)
              .encryptionDetails(
                  secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()))
              .region(awsLambdaFunctionElement.getRegion())
              .functionName(functionMeta.getFunctionArn())
              .qualifier(functionMeta.getVersion())
              .payload(isNotBlank(lambdaTestEvent.getPayload()) ? lambdaTestEvent.getPayload() : null)
              .awsLambdaExecutionData(awsLambdaExecutionData)
              .lambdaTestEvent(lambdaTestEvent)
              .build(),
          context.getAppId(), activityId);

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(ExceptionUtils.getMessage(e), e);
    }
  }

  private ExecutionResponse executeTask(String accountId, AwsLambdaRequest request, String appId, String activityId) {
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(accountId)
            .appId(isNotEmpty(appId) ? appId : GLOBAL_APP_ID)
            .async(true)
            .tags(isNotEmpty(request.getAwsConfig().getTag()) ? singletonList(request.getAwsConfig().getTag()) : null)
            .data(TaskData.builder()
                      .taskType(TaskType.AWS_LAMBDA_TASK.name())
                      .parameters(new Object[] {request})
                      .timeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                      .build())
            .waitId(activityId)
            .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(
            CommandStateExecutionData.Builder.aCommandStateExecutionData().withActivityId(activityId).build())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      String activityId = response.keySet().iterator().next();
      AwsLambdaExecuteFunctionResponse functionResponse =
          (AwsLambdaExecuteFunctionResponse) response.values().iterator().next();

      AwsLambdaExecutionData awsLambdaExecutionData = functionResponse.getAwsLambdaExecutionData();
      awsLambdaExecutionData.setStatusCode(functionResponse.getStatusCode());
      awsLambdaExecutionData.setFunctionError(functionResponse.getFunctionError());

      if (functionResponse.getExecutionStatus() == ExecutionStatus.FAILED) {
        return ExecutionResponse.Builder.anExecutionResponse()
            .withExecutionStatus(functionResponse.getExecutionStatus())
            .withStateExecutionData(awsLambdaExecutionData)
            .withErrorMessage(functionResponse.getErrorMessage())
            .build();
      }

      boolean assertionStatus = true;
      ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
      String logResult = functionResponse.getLogResult();
      if (logResult != null) {
        awsLambdaExecutionData.setLogResult(logResult);
      }
      awsLambdaExecutionData.setPayload(functionResponse.getPayload());
      awsLambdaExecutionData.setAssertionStatement(functionResponse.getLambdaTestEvent().getAssertion());

      if (isNotBlank(functionResponse.getLambdaTestEvent().getAssertion())) {
        assertionStatus = (boolean) context.evaluateExpression(
            functionResponse.getLambdaTestEvent().getAssertion(), awsLambdaExecutionData);
      }

      if (!assertionStatus || awsLambdaExecutionData.getStatusCode() < 200
          || awsLambdaExecutionData.getStatusCode() > 299) { // Lambda return non 200 range for failure
        executionStatus = ExecutionStatus.FAILED;
      }
      awsLambdaExecutionData.setAssertionStatus(executionStatus.name());

      updateActivityStatus(activityId, ((ExecutionContextImpl) context).getApp().getUuid(), executionStatus);
      return ExecutionResponse.Builder.anExecutionResponse()
          .withExecutionStatus(executionStatus)
          .withStateExecutionData(awsLambdaExecutionData)
          .build();

    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    Activity activity = Activity.builder()
                            .applicationName(app.getName())
                            .environmentId(env.getUuid())
                            .environmentName(env.getName())
                            .environmentType(env.getEnvironmentType())
                            .commandName(getName())
                            .type(Type.Verification)
                            .workflowType(executionContext.getWorkflowType())
                            .workflowExecutionName(executionContext.getWorkflowExecutionName())
                            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                            .commandType(getStateType())
                            .workflowExecutionId(executionContext.getWorkflowExecutionId())
                            .workflowId(executionContext.getWorkflowId())
                            .commandUnits(Collections.emptyList())
                            .status(ExecutionStatus.RUNNING)
                            .build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }

  private void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  @Override
  @SchemaIgnore
  public ContextElementType getRequiredContextElementType() {
    return ContextElementType.AWS_LAMBDA_FUNCTION;
  }

  public List<LambdaTestEvent> getLambdaTestEvents() {
    return lambdaTestEvents;
  }

  public void setLambdaTestEvents(List<LambdaTestEvent> lambdaTestEvents) {
    this.lambdaTestEvents = lambdaTestEvents;
  }
}