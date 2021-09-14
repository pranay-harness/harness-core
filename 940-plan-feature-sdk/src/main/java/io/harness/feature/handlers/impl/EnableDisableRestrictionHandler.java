/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.handlers.impl;

import io.harness.feature.bases.EnableDisableRestriction;
import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.constants.RestrictionType;
import io.harness.feature.exceptions.FeatureNotSupportedException;
import io.harness.feature.handlers.RestrictionHandler;

public class EnableDisableRestrictionHandler implements RestrictionHandler {
  private final EnableDisableRestriction enableDisableRestriction;

  public EnableDisableRestrictionHandler(EnableDisableRestriction enableDisableRestriction) {
    this.enableDisableRestriction = enableDisableRestriction;
  }

  @Override
  public RestrictionType getRestrictionType() {
    return enableDisableRestriction.getRestrictionType();
  }

  @Override
  public void check(String accountIdentifier) {
    if (!enableDisableRestriction.isEnabled()) {
      throw new FeatureNotSupportedException("Feature is not enabled");
    }
  }

  @Override
  public RestrictionDTO toRestrictionDTO(String accountIdentifier) {
    return RestrictionDTO.builder()
        .restrictionType(enableDisableRestriction.getRestrictionType())
        .enabled(enableDisableRestriction.isEnabled())
        .build();
  }
}
