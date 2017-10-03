package software.wings.api;

import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.beans.AwsConfig;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * The type Aws lambda context element.
 */
public class AwsLambdaFunctionElement implements ContextElement {
  private String uuid;
  private String name;

  private AwsConfig awsConfig;
  private String region;
  private FunctionMeta functionArn;

  public AwsConfig getAwsConfig() {
    return awsConfig;
  }

  public void setAwsConfig(AwsConfig awsConfig) {
    this.awsConfig = awsConfig;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public FunctionMeta getFunctionArn() {
    return functionArn;
  }

  public void setFunctionArn(FunctionMeta functionArn) {
    this.functionArn = functionArn;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AWS_LAMBDA_FUNCTION;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  public static final class Builder {
    private AwsLambdaFunctionElement awsLambdaFunctionElement;

    private Builder() {
      awsLambdaFunctionElement = new AwsLambdaFunctionElement();
    }

    public static Builder anAwsLambdaContextElement() {
      return new Builder();
    }

    public Builder withAwsConfig(AwsConfig awsConfig) {
      awsLambdaFunctionElement.setAwsConfig(awsConfig);
      return this;
    }

    public Builder withRegion(String region) {
      awsLambdaFunctionElement.setRegion(region);
      return this;
    }

    public Builder withFunctionArn(FunctionMeta functionArn) {
      awsLambdaFunctionElement.setFunctionArn(functionArn);
      return this;
    }

    public Builder but() {
      return anAwsLambdaContextElement()
          .withAwsConfig(awsLambdaFunctionElement.getAwsConfig())
          .withRegion(awsLambdaFunctionElement.getRegion())
          .withFunctionArn(awsLambdaFunctionElement.getFunctionArn());
    }

    public AwsLambdaFunctionElement build() {
      return awsLambdaFunctionElement;
    }
  }
}
