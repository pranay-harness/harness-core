/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.exceptionmappers;

import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.FailureDTO;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
  @Override
  public Response toResponse(NotFoundException exception) {
    log.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);
    FailureDTO failureDTO =
        FailureDTO.toBody(Status.FAILURE, ErrorCode.RESOURCE_NOT_FOUND_EXCEPTION, exception.getMessage(), null);
    return Response.status(Response.Status.BAD_REQUEST).entity(failureDTO).type(MediaType.APPLICATION_JSON).build();
  }
}
