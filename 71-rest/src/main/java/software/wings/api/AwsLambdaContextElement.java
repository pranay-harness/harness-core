package software.wings.api;

import io.harness.context.ContextElementType;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.AwsConfig;
import software.wings.beans.Tag;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * The type Aws lambda context element.
 */
@Value
@Builder
public class AwsLambdaContextElement implements ContextElement {
  public static final String AWS_LAMBDA_REQUEST_PARAM = "AWS_LAMBDA_REQUEST_PARAM";

  @Value
  @Builder
  public static class FunctionMeta {
    private String functionName;
    private String functionArn;
    private String version;
  }

  private AwsConfig awsConfig;
  private String region;
  private List<FunctionMeta> functionArns;
  private List<String> aliases;
  private List<Tag> tags;

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
}
