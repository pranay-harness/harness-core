package io.harness.yaml.core.intfc;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface OverridesApplier<T> {
  @JsonIgnore T applyOverrides(T overrideConfig);
}
