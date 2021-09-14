/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.bases;

import io.harness.feature.TimeUnit;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.LimitRestriction;
import io.harness.feature.interfaces.RateLimitInterface;

import lombok.Getter;

@Getter
public class RateLimitRestriction extends Restriction implements LimitRestriction {
  long limit;
  TimeUnit timeUnit;
  RateLimitInterface rateLimitInterface;

  public RateLimitRestriction(
      RestrictionType restrictionType, long limit, TimeUnit timeUnit, RateLimitInterface rateLimitInterface) {
    super(restrictionType);
    this.limit = limit;
    this.timeUnit = timeUnit;
    this.rateLimitInterface = rateLimitInterface;
  }
}
