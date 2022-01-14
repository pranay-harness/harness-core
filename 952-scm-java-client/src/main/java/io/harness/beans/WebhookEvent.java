package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@OwnedBy(DX)
public interface WebhookEvent {
  enum Type { PR, PUSH, ISSUE_COMMENT }
  WebhookEvent.Type getType();
  WebhookBaseAttributes getBaseAttributes();
}
