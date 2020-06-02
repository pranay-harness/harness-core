package io.harness.executionplan.core.impl;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Value;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Value
@Builder
public class CreateExecutionPlanContextImpl implements CreateExecutionPlanContext {
  String accountId;
  @Default @Getter(AccessLevel.NONE) Map<String, Object> attributes = new ConcurrentHashMap<>();

  public <T> Optional<T> getAttribute(String key) {
    return Optional.ofNullable((T) attributes.get(key));
  }

  public <T> void addAttribute(String key, T value) {
    attributes.put(key, value);
  }
}
