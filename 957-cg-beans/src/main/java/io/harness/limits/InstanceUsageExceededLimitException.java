/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@OwnedBy(PL)
@Value
public class InstanceUsageExceededLimitException extends RuntimeException {
  private String accountId;
  private double usage;
  private String message;
}
