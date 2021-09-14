/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
