package software.wings.sm;

import static software.wings.beans.Activity.Builder.anActivity;

import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AwsLambdaContextElement;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.api.AwsLambdaExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsSettingProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.stencils.EnumData;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Inject;

public class AwsLambdaVerification extends State {
  @EnumData(enumDataProvider = AwsSettingProvider.class)
  @Attributes(required = true, title = "AWS account")
  private String awsCredentialsConfigId;

  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private AwsHelperService awsHelperService;
  @Transient private static final Logger logger = LoggerFactory.getLogger(AwsLambdaVerification.class);

  /**
   * Instantiates a new state.
   *
   * @param name      the name
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

    try {
      ContextElement contextElement = context.getContextElement(ContextElementType.AWS_LAMBDA_FUNCTION);
      AwsLambdaContextElement awsLambdaContextElement = (AwsLambdaContextElement) contextElement;
      AwsConfig awsConfig = awsLambdaContextElement.getAwsConfig();
      List<FunctionMeta> functionArns = awsLambdaContextElement.getFunctionArns();
      String region = awsLambdaContextElement.getRegion();
      FunctionMeta functionMeta = functionArns.get(0);

      awsLambdaExecutionData.setFunctionArn(functionMeta.getFunctionArn());
      awsLambdaExecutionData.setFunctionVersion(functionMeta.getVersion());

      InvokeResult invokeResult = awsHelperService.invokeFunction(region, awsConfig.getAccessKey(),
          awsConfig.getSecretKey(),
          new InvokeRequest().withFunctionName(functionMeta.getFunctionArn()).withQualifier(functionMeta.getVersion()));
      awsLambdaExecutionData.setStatusCode(invokeResult.getStatusCode());
      awsLambdaExecutionData.setFunctionError(invokeResult.getFunctionError());
      awsLambdaExecutionData.setLogResult(invokeResult.getLogResult());
      awsLambdaExecutionData.setPayload(StandardCharsets.UTF_8.decode(invokeResult.getPayload()).toString());
    } catch (Exception ex) {
      logger.error("Exception in verifying lambda", ex);
      errorMessage = ex.getMessage();
      awsLambdaExecutionData.setErrorMsg(errorMessage);
      executionStatus = ExecutionStatus.FAILED;
    }

    updateActivityStatus(activityId, ((ExecutionContextImpl) context).getApp().getUuid(), executionStatus);
    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(awsLambdaExecutionData)
        .withErrorMessage(errorMessage)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getAwsCredentialsConfigId() {
    return awsCredentialsConfigId;
  }

  public void setAwsCredentialsConfigId(String awsCredentialsConfigId) {
    this.awsCredentialsConfigId = awsCredentialsConfigId;
  }

  protected String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

    Activity.Builder activityBuilder =
        anActivity()
            .withAppId(app.getUuid())
            .withApplicationName(app.getName())
            .withEnvironmentId(env.getUuid())
            .withEnvironmentName(env.getName())
            .withEnvironmentType(env.getEnvironmentType())
            .withCommandName(getName())
            .withType(Type.Verification)
            .withWorkflowType(executionContext.getWorkflowType())
            .withWorkflowExecutionName(executionContext.getWorkflowExecutionName())
            .withStateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .withStateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .withCommandType(getStateType())
            .withWorkflowExecutionId(executionContext.getWorkflowExecutionId());
    return activityService.save(activityBuilder.build()).getUuid();
  }

  @Override
  public ContextElementType getRequiredContextElementType() {
    return ContextElementType.AWS_LAMBDA_FUNCTION;
  }

  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }
}
