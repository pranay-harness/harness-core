/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline.service.yamlschema.exception;

import static io.harness.eraro.ErrorCode.CACHE_NOT_FOUND_EXCEPTION;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(HarnessTeam.PIPELINE)
public class YamlSchemaCacheException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public YamlSchemaCacheException(String message) {
    super(message, null, CACHE_NOT_FOUND_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public YamlSchemaCacheException(String message, Throwable cause) {
    super(message, cause, CACHE_NOT_FOUND_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
