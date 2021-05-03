package io.harness.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ScmException;
import io.harness.exception.UnexpectedException;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class ScmResponseStatusUtils {
  // If different provider have different status code take provider type as input too.
  public void checkScmResponseStatusAndThrowException(int statusCode) {
    switch (statusCode) {
      case 404:
        throw new ScmException(ErrorCode.SCM_NOT_FOUND_ERROR);
      case 409:
        throw new ScmException(ErrorCode.SCM_CONFLICT_ERROR);
      case 422:
        throw new ScmException(ErrorCode.SCM_UNPROCESSABLE_ENTITY);
      default:
        if (!(statusCode == 200 || statusCode == 201)) {
          log.error("Encountered new status code: [{}] from scm", statusCode);
          throw new UnexpectedException("Unexpected error occurred while doing scm operation");
        }
    }
  }
}
