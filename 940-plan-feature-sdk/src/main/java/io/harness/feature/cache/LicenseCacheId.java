/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.cache;

import io.harness.ModuleType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LicenseCacheId {
  private String accountIdentifier;
  private ModuleType moduleType;
}
