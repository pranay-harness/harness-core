package software.wings.sm;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.LogType;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.codec.binary.Base64;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.api.AwsLambdaExecutionData;
import software.wings.api.AwsLambdaFunctionElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.LambdaTestEvent;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.utils.LambdaConvention;
import software.wings.utils.Misc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AwsLambdaVerification extends State {
  @Attributes(title = "Function Test Events") private List<LambdaTestEvent> lambdaTestEvents = new ArrayList<>();

  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private AwsHelperService awsHelperService;
  @Transient private static final Logger logger = LoggerFactory.getLogger(AwsLambdaVerification.class);

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
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage = null;

    AwsLambdaExecutionData awsLambdaExecutionData = new AwsLambdaExecutionData();
    boolean assertionStatus = true;
    try {
      AwsLambdaFunctionElement awsLambdaFunctionElement =
          context.getContextElement(ContextElementType.AWS_LAMBDA_FUNCTION);
      AwsConfig awsConfig = awsLambdaFunctionElement.getAwsConfig();
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

      InvokeRequest invokeRequest = new InvokeRequest()
                                        .withFunctionName(functionMeta.getFunctionArn())
                                        .withQualifier(functionMeta.getVersion())
                                        .withLogType(LogType.Tail);

      if (isNotBlank(lambdaTestEvent.getPayload())) {
        invokeRequest.setPayload(lambdaTestEvent.getPayload());
      }

      InvokeResult invokeResult = awsHelperService.invokeFunction(
          awsLambdaFunctionElement.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey(), invokeRequest);
      logger.info("Lambda invocation result: " + invokeResult.toString());

      awsLambdaExecutionData.setStatusCode(invokeResult.getStatusCode());
      awsLambdaExecutionData.setFunctionError(invokeResult.getFunctionError());
      String logResult = invokeResult.getLogResult();
      if (logResult != null) {
        logResult = new String(Base64.decodeBase64(logResult), "UTF-8");
      }
      awsLambdaExecutionData.setLogResult(logResult);
      awsLambdaExecutionData.setPayload(StandardCharsets.UTF_8.decode(invokeResult.getPayload()).toString());
      awsLambdaExecutionData.setAssertionStatement(lambdaTestEvent.getAssertion());

      if (isNotBlank(lambdaTestEvent.getAssertion())) {
        assertionStatus = (boolean) context.evaluateExpression(lambdaTestEvent.getAssertion(), awsLambdaExecutionData);
      }
    } catch (Exception ex) {
      logger.error("Exception in verifying lambda", ex);
      errorMessage = Misc.getMessage(ex);
      awsLambdaExecutionData.setErrorMsg(errorMessage);
      executionStatus = ExecutionStatus.FAILED;
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
        .withErrorMessage(errorMessage)
        .build();
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
                            .serviceVariables(Maps.newHashMap())
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
