package io.harness.beans.execution;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ManualExecutionSource.class, name = "MANUAL")
  , @JsonSubTypes.Type(value = WebhookExecutionSource.class, name = "WEBHOOK")
  , @JsonSubTypes.Type(value = CustomExecutionSource.class, name = "CUSTOM")
})

public interface ExecutionSource {
  enum Type { WEBHOOK, MANUAL, CUSTOM }
  ExecutionSource.Type getType();
}
