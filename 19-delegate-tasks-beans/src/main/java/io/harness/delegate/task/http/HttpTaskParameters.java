package io.harness.delegate.task.http;

import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpTaskParameters implements TaskParameters {
  private String method;
  @Expression private String url;
  @Expression private String header;
  @Expression private String body;
  private int socketTimeoutMillis;
}
