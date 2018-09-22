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

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class Builder {
    private String uuid;
    private String name;
    private AwsConfig awsConfig;
    private String region;
    private FunctionMeta functionArn;

    private Builder() {}

    public static Builder anAwsLambdaFunctionElement() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withAwsConfig(AwsConfig awsConfig) {
      this.awsConfig = awsConfig;
      return this;
    }

    public Builder withRegion(String region) {
      this.region = region;
      return this;
    }

    public Builder withFunctionArn(FunctionMeta functionArn) {
      this.functionArn = functionArn;
      return this;
    }

    public AwsLambdaFunctionElement build() {
      AwsLambdaFunctionElement awsLambdaFunctionElement = new AwsLambdaFunctionElement();
      awsLambdaFunctionElement.setUuid(uuid);
      awsLambdaFunctionElement.setName(name);
      awsLambdaFunctionElement.setAwsConfig(awsConfig);
      awsLambdaFunctionElement.setRegion(region);
      awsLambdaFunctionElement.setFunctionArn(functionArn);
      return awsLambdaFunctionElement;
    }
  }
}
