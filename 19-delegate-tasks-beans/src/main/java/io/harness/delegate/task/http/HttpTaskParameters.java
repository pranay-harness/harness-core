package io.harness.delegate.task.http;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class HttpTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String method;
  @Expression(ALLOW_SECRETS) private String url;
  @Expression(ALLOW_SECRETS) private String header;
  @Expression(ALLOW_SECRETS) private String body;
  private int socketTimeoutMillis;
  private boolean useProxy;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(url));
  }
}
