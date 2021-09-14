/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.exceptionmappers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class GenericExceptionMapperV2 implements ExceptionMapper<Throwable> {
  @Context private ResourceInfo resourceInfo;
  private static final String message = "Oops, something went wrong on our end. Please contact Harness Support.";

  private boolean hasExposeExceptionAnnotation() {
    return resourceInfo != null
        && ((resourceInfo.getResourceClass() != null
                && resourceInfo.getResourceClass().isAnnotationPresent(ExposeInternalException.class))
            || (resourceInfo.getResourceMethod() != null
                && resourceInfo.getResourceMethod().isAnnotationPresent(ExposeInternalException.class)));
  }

  @Override
  public Response toResponse(Throwable exception) {
    log.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);
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
