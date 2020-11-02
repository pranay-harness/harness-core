package io.harness.cvng.exception;

import io.harness.annotations.ExposeInternalException;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExceptionUtils;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * The Class GenericExceptionMapper.
 *
 * The current implementation is taken from 71-rest, it hasn't been moved to 12-commons as this one will evolve over
 * time
 * @param <T> the generic type
 */
@Slf4j
@Provider
public class GenericExceptionMapper<T> implements ExceptionMapper<Throwable> {
  @Context private ResourceInfo resourceInfo;
  @Override
  public Response toResponse(Throwable exception) {
    log.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);
    if (hasExposeExceptionAnnotation()) {
      RestResponse<T> restResponse = new RestResponse<>();
      restResponse.getResponseMessages().add(ResponseMessage.builder()
                                                 .code(ErrorCode.DEFAULT_ERROR_CODE)
                                                 .level(Level.ERROR)
                                                 .exception(exposeStackTrace() ? exception : null)
                                                 .message(exception.toString())
                                                 .build());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(restResponse)
          .type(MediaType.APPLICATION_JSON)
          .build();
    }
    return getDefaultResponse();
  }

  private boolean hasExposeExceptionAnnotation() {
    if (resourceInfo == null || resourceInfo.getResourceClass() == null) { // can be null in case of 404
      return false;
    }
    return resourceInfo.getResourceClass().isAnnotationPresent(ExposeInternalException.class)
        || resourceInfo.getResourceMethod().isAnnotationPresent(ExposeInternalException.class);
  }

  private boolean exposeStackTrace() {
    if (resourceInfo.getResourceClass().getAnnotation(ExposeInternalException.class) != null) {
      return resourceInfo.getResourceClass().getAnnotation(ExposeInternalException.class).withStackTrace();
    } else if (resourceInfo.getResourceMethod().getAnnotation(ExposeInternalException.class) != null) {
      return resourceInfo.getResourceMethod().getAnnotation(ExposeInternalException.class).withStackTrace();
    }
    throw new IllegalStateException("Check if it has ExposeInternalException annotation.");
  }

  private Response getDefaultResponse() {
    RestResponse<T> restResponse = new RestResponse<>();

    restResponse.getResponseMessages().add(
        ResponseMessage.builder()
            .code(ErrorCode.DEFAULT_ERROR_CODE)
            .level(Level.ERROR)
            .message("An error has occurred. Please contact the Harness support team.")
            .build());

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .entity(restResponse)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
