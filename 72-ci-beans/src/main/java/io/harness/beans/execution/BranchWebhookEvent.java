package io.harness.beans.execution;

import static io.harness.beans.execution.WebhookEvent.Type.BRANCH;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("Branch")
public class BranchWebhookEvent implements WebhookEvent {
  @Override
  public Type getType() {
    return BRANCH;
  }
}
