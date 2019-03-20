package io.harness.delegate.task.http;

import static java.util.Arrays.asList;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class HttpTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private String method;
  @Expression private String url;
  @Expression private String header;
  @Expression private String body;
  private int socketTimeoutMillis;

  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(url));
  }
}
