/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;
import io.harness.feature.interfaces.LimitRestriction;
import io.harness.feature.interfaces.StaticLimitInterface;

import lombok.Getter;

@Getter
public class StaticLimitRestriction extends Restriction implements LimitRestriction {
  long limit;
  StaticLimitInterface staticLimitInterface;

  public StaticLimitRestriction(
      RestrictionType restrictionType, long limit, StaticLimitInterface staticLimitInterface) {
    super(restrictionType);
    this.limit = limit;
    this.staticLimitInterface = staticLimitInterface;
  }
}
