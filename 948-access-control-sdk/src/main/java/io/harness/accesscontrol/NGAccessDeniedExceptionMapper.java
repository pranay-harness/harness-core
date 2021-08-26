package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.logging.ExceptionLogger;
import io.harness.ng.core.Status;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NGAccessDeniedExceptionMapper implements ExceptionMapper<NGAccessDeniedException> {
  @Context private ResourceInfo resourceInfo;

  @Override
  public Response toResponse(NGAccessDeniedException exception) {
    ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
    ErrorCode errorCode = exception.getCode() != null ? exception.getCode() : ErrorCode.UNKNOWN_ERROR;
    AccessDeniedErrorDTO errorBody = new AccessDeniedErrorDTO(
        Status.ERROR, errorCode, exception.getMessage(), null, exception.getFailedPermissionChecks());
    return Response.status(Response.Status.fromStatusCode(errorCode.getStatus().getCode())).entity(errorBody).build();
  }
}
