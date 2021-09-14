/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.exception;

import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationExceptionMapper implements ExceptionMapper<NotificationException> {
  @Override
  public Response toResponse(NotificationException exception) {
    log.error("Exception occurred: {}", exception.getMessage(), exception);
    ErrorDTO errorDTO = ErrorDTO.newError(Status.ERROR, ErrorCode.UNKNOWN_ERROR, exception.getMessage());
    return Response.status(Response.Status.BAD_REQUEST).entity(errorDTO).type(MediaType.APPLICATION_JSON).build();
  }
}
