package software.wings.api;

import io.harness.context.ContextElementType;

import software.wings.beans.command.CodeDeployParams;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsCodeDeployRequestElement implements ContextElement {
  public static final String AWS_CODE_DEPLOY_REQUEST_PARAM = "AWS_CODE_DEPLOY_REQUEST_PARAM";

  private CodeDeployParams codeDeployParams;
  private CodeDeployParams oldCodeDeployParams;

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
    return AWS_CODE_DEPLOY_REQUEST_PARAM;
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
