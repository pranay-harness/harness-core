package software.wings.sm;

import com.google.inject.Inject;

import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.github.reinert.jjschema.Attributes;
import software.wings.api.AwsLambdaContextElement;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.beans.AwsConfig;
import software.wings.common.Constants;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsSettingProvider;
import software.wings.stencils.EnumData;

import java.util.List;

public class AwsLambdaVerification extends State {
  @EnumData(enumDataProvider = AwsSettingProvider.class)
  @Attributes(required = true, title = "AWS account")
  private String awsCredentialsConfigId;

  @Inject private AwsHelperService awsHelperService;

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
    ContextElement contextElement =
        context.getContextElement(ContextElementType.PARAM, Constants.AWS_LAMBDA_REQUEST_PARAM);
    AwsLambdaContextElement awsLambdaContextElement = (AwsLambdaContextElement) contextElement;
    AwsConfig awsConfig = awsLambdaContextElement.getAwsConfig();
    List<FunctionMeta> functionArns = awsLambdaContextElement.getFunctionArns();
    String region = awsLambdaContextElement.getRegion();
    FunctionMeta functionMeta = functionArns.get(0);
    InvokeResult invokeResult = awsHelperService.invokeFunction(region, awsConfig.getAccessKey(),
        awsConfig.getSecretKey(),
        new InvokeRequest().withFunctionName(functionMeta.getFunctionArn()).withQualifier(functionMeta.getVersion()));
    System.out.println(invokeResult.toString());
    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withStateExecutionData(null)
        .withErrorMessage(null)
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
}
