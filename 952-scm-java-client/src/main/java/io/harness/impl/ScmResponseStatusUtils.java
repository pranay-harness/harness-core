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
    try {
      switch (statusCode) {
        case 404:
          throw new ScmException(ErrorCode.SCM_NOT_FOUND_ERROR);
        case 409:
          throw new ScmException(ErrorCode.SCM_CONFLICT_ERROR);
        case 422:
          throw new ScmException(ErrorCode.SCM_UNPROCESSABLE_ENTITY);
        case 401:
          throw new ScmException(ErrorCode.SCM_UNAUTHORIZED);
        default:
          if (statusCode >= 300) {
            log.error("Encountered new status code: [{}] from scm", statusCode);
            throw new UnexpectedException("Unexpected error occurred while doing scm operation");
          }
      }
    } catch (ScmException e) {
      if (isNotEmpty(errorMsg)) {
        throw new ExplanationException(errorMsg, e);
      } else {
        throw e;
      }
    }
  }
}
