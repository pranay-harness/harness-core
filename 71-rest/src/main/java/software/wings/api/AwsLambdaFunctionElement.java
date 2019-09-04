package software.wings.api;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Value;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.beans.AwsConfig;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * The type Aws lambda context element.
 */
@Value
@Builder
public class AwsLambdaFunctionElement implements ContextElement {
  private String uuid;
  private String name;

  private AwsConfig awsConfig;
  private String region;
  private FunctionMeta functionArn;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AWS_LAMBDA_FUNCTION;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
