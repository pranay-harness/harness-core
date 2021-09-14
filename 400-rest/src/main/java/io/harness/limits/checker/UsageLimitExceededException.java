/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits.checker;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.limits.lib.Limit;

import java.util.EnumSet;
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(PL)
@Value
@EqualsAndHashCode(callSuper = false)
public class UsageLimitExceededException extends WingsException {
  private Limit limit;
  private String accountId;

  private static final String MESSAGE_KEY = "message";
  public UsageLimitExceededException(
      ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets, Limit limit, String accountId) {
    super("Usage limit reached. Limit: " + limit + " , accountId=" + accountId, null, code, level, reportTargets, null);
    param(MESSAGE_KEY, "Usage limit reached. Limit: " + limit + " , accountId=" + accountId);
    this.limit = limit;
    this.accountId = accountId;
  }

  @Override
  public String getMessage() {
    return "Usage limit reached. Limit: " + limit + " , accountId=" + accountId;
  }
}
