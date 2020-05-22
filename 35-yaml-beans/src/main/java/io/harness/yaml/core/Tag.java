package io.harness.yaml.core;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

/**
 * Base class for tag structure
 */
@Value
@Builder
public class Tag {
  @NotNull String key;
  @NotNull String value;
}
