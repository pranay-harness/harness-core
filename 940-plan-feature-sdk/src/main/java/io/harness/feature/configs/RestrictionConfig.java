package io.harness.feature.configs;

import io.harness.feature.TimeUnit;
import io.harness.feature.constants.RestrictionType;

import lombok.Value;

@Value
public class RestrictionConfig {
  private RestrictionType restrictionType;
  private Boolean enabled;
  private Long limit;
  private TimeUnit timeUnit;
  private String implClass;
}
