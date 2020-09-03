package io.harness.beans.execution;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public interface ExecutionSource {
  enum Type { Webhook }
  ExecutionSource.Type getType();
}
