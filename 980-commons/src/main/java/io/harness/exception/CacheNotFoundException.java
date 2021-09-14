/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(PL)
public class CacheNotFoundException extends WingsException {
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public CacheNotFoundException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, ErrorCode.CACHE_NOT_FOUND_EXCEPTION, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
