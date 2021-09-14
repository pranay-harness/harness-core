/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.account;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(PL)
public class ServiceAccountConfig {
  public static final long DEFAULT_API_KEY_LIMIT = 5;
  public static final long DEFAULT_TOKEN_LIMIT = 5;

  @Getter @Setter private long apiKeyLimit = DEFAULT_API_KEY_LIMIT;
  @Getter @Setter private long tokenLimit = DEFAULT_TOKEN_LIMIT;

  @Builder
  public ServiceAccountConfig(long apiKeyLimit, long tokenLimit) {
    this.apiKeyLimit = apiKeyLimit;
    this.tokenLimit = tokenLimit;
  }
}
