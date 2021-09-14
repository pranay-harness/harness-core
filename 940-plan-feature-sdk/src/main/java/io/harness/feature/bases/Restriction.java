/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.bases;

import io.harness.feature.constants.RestrictionType;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class Restriction {
  protected RestrictionType restrictionType;

  public RestrictionType getRestrictionType() {
    return restrictionType;
  }
}
