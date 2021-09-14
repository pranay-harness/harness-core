/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.handlers;

import io.harness.feature.beans.RestrictionDTO;
import io.harness.feature.constants.RestrictionType;

public interface RestrictionHandler {
  RestrictionType getRestrictionType();
  void check(String accountIdentifier);
  RestrictionDTO toRestrictionDTO(String accountIdentifier);
}
