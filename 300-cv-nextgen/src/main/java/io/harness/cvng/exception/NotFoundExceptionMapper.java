/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExceptionUtils;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * ExceptionMapper for handling 404 NotFoundException
 */
@Slf4j
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
  @Context private ResourceInfo resourceInfo;

  @Override
  public Response toResponse(NotFoundException exception) {
    log.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);
    return Response.status(Response.Status.NOT_FOUND)
        .entity(ResponseMessage.builder().message(exception.toString()).code(ErrorCode.RESOURCE_NOT_FOUND).build())
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
