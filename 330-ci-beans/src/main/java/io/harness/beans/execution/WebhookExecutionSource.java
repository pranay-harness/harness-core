package io.harness.beans.execution;

import static io.harness.beans.execution.ExecutionSource.Type.Webhook;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("Webhook")
public class WebhookExecutionSource implements ExecutionSource {
  private WebhookGitUser user;
  private WebhookEvent webhookEvent;
  private String triggerName;

  @Override
  public Type getType() {
    return Webhook;
  }
}
