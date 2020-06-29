package io.harness.ng.core.exceptionmappers;

import io.harness.annotations.ExposeInternalException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.ng.core.ErrorDTO;
import io.harness.ng.core.Status;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Slf4j
public class GenericExceptionMapperV2 implements ExceptionMapper<Throwable> {
  @Context private ResourceInfo resourceInfo;
  private static final String message = "Oops, something went wrong on our end. Please contact Harness Support.";

  private boolean hasExposeExceptionAnnotation() {
    return resourceInfo != null
        && (resourceInfo.getResourceClass().isAnnotationPresent(ExposeInternalException.class)
               || resourceInfo.getResourceMethod().isAnnotationPresent(ExposeInternalException.class));
  }

  @Override
  public Response toResponse(Throwable exception) {
    logger.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);
    ErrorDTO errorDto = ErrorDTO.newError(Status.ERROR, ErrorCode.UNKNOWN_ERROR, message);
    if (hasExposeExceptionAnnotation()) {
      errorDto.setDetailedMessage(exception.getMessage());
    }
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(errorDto)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
