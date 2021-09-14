/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExplanationException;
import io.harness.exception.ScmException;
import io.harness.exception.UnexpectedException;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class ScmResponseStatusUtils {
  // If different provider have different status code take provider type as input too.
  public void checkScmResponseStatusAndThrowException(int statusCode, String errorMsg) {
    if (statusCode >= 300) {
      ErrorCode errorCode = convertScmStatusCodeToErrorCode(statusCode);
      if (errorCode == ErrorCode.UNEXPECTED) {
        log.error("Encountered new status code: [{}] with message: [{}] from scm", statusCode, errorMsg);
        throw new UnexpectedException("Unexpected error occurred while doing scm operation");
      }
      if (isNotEmpty(errorMsg)) {
        throw new ExplanationException(errorMsg, new ScmException(errorCode));
      } else {
        throw new ScmException(errorCode);
      }
    }
  }

  public ErrorCode convertScmStatusCodeToErrorCode(int statusCode) {
    switch (statusCode) {
      case 304:
        return ErrorCode.SCM_NOT_MODIFIED;
      case 404:
        return ErrorCode.SCM_NOT_FOUND_ERROR;
      case 409:
        return ErrorCode.SCM_CONFLICT_ERROR;
      case 422:
        return ErrorCode.SCM_UNPROCESSABLE_ENTITY;
      case 401:
      case 403:
        return ErrorCode.SCM_UNAUTHORIZED;
      default:
        return ErrorCode.UNEXPECTED;
    }
  }
}
