package software.wings.api;

import static software.wings.common.Constants.AWS_LAMBDA_REQUEST_PARAM;

import software.wings.beans.AwsConfig;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Aws lambda context element.
 */
public class AwsLambdaContextElement implements ContextElement {
  private AwsConfig awsConfig;
  private String region;
  private List<FunctionMeta> functionArns = new ArrayList<>();

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

  public List<FunctionMeta> getFunctionArns() {
    return functionArns;
  }

  public void setFunctionArns(List<FunctionMeta> functionArns) {
    this.functionArns = functionArns;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return AWS_LAMBDA_REQUEST_PARAM;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  public static class FunctionMeta {
    private String functionName;
    private String functionArn;
    private String version;

    private FunctionMeta() {}

    private FunctionMeta(Builder builder) {
      setFunctionName(builder.functionName);
      setFunctionArn(builder.functionArn);
      setVersion(builder.version);
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static Builder newBuilder(FunctionMeta copy) {
      Builder builder = new Builder();
      builder.functionName = copy.functionName;
      builder.functionArn = copy.functionArn;
      builder.version = copy.version;
      return builder;
    }

    public String getFunctionName() {
      return functionName;
    }

    public void setFunctionName(String functionName) {
      this.functionName = functionName;
    }

    public String getFunctionArn() {
      return functionArn;
    }

    public void setFunctionArn(String functionArn) {
      this.functionArn = functionArn;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public static final class Builder {
      private String functionName;
      private String functionArn;
      private String version;

      private Builder() {}

      public Builder withFunctionName(String functionName) {
        this.functionName = functionName;
        return this;
      }

      public Builder withFunctionArn(String functionArn) {
        this.functionArn = functionArn;
        return this;
      }

      public Builder withVersion(String version) {
        this.version = version;
        return this;
      }

      public FunctionMeta build() {
        return new FunctionMeta(this);
      }
    }
  }

  public static final class Builder {
    private AwsLambdaContextElement awsLambdaContextElement;

    private Builder() {
      awsLambdaContextElement = new AwsLambdaContextElement();
    }

    public static Builder anAwsLambdaContextElement() {
      return new Builder();
    }

    public Builder withAwsConfig(AwsConfig awsConfig) {
      awsLambdaContextElement.setAwsConfig(awsConfig);
      return this;
    }

    public Builder withRegion(String region) {
      awsLambdaContextElement.setRegion(region);
      return this;
    }

    public Builder withFunctionArns(List<FunctionMeta> functionArns) {
      awsLambdaContextElement.setFunctionArns(functionArns);
      return this;
    }

    public Builder but() {
      return anAwsLambdaContextElement()
          .withAwsConfig(awsLambdaContextElement.getAwsConfig())
          .withRegion(awsLambdaContextElement.getRegion())
          .withFunctionArns(awsLambdaContextElement.getFunctionArns());
    }

    public AwsLambdaContextElement build() {
      return awsLambdaContextElement;
    }
  }
}
