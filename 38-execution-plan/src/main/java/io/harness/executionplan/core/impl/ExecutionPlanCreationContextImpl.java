package io.harness.executionplan.core.impl;

import io.harness.executionplan.core.ExecutionPlanCreationContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
public class ExecutionPlanCreationContextImpl implements ExecutionPlanCreationContext {
  String accountId;
  @Default @Getter(AccessLevel.NONE) Map<String, Object> attributes = new ConcurrentHashMap<>();

  public <T> Optional<T> getAttribute(String key) {
    return Optional.ofNullable((T) attributes.get(key));
  }

  public <T> void addAttribute(String key, T value) {
    attributes.put(key, value);
  }

  @Override
  public void removeAttribute(String key) {
    attributes.remove(key);
  }
}
