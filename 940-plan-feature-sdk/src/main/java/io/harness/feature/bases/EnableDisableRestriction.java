/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;

import lombok.Getter;

@Getter
public class EnableDisableRestriction extends Restriction {
  private boolean enabled;

  public EnableDisableRestriction(RestrictionType restrictionType, boolean enabled) {
    super(restrictionType);
    this.enabled = enabled;
  }
}
