/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.user.exception.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.logging.ExceptionLogger;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.user.exception.InvalidUserRemoveRequestException;

import java.util.stream.Collectors;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class InvalidUserRemoveRequestExceptionMapper implements ExceptionMapper<InvalidUserRemoveRequestException> {
  @Context private ResourceInfo resourceInfo;

  @Override
  public Response toResponse(InvalidUserRemoveRequestException exception) {
    ExceptionLogger.logProcessedMessages(exception, MANAGER, log);

    ErrorCode errorCode = exception.getCode() != null ? exception.getCode() : ErrorCode.UNKNOWN_ERROR;
    ErrorDTO errorBody = ErrorDTO.newError(Status.ERROR, errorCode, null);
    errorBody.setResponseMessages(exception.getLastAdminScopes()
                                      .stream()
                                      .map(scopeString -> ResponseMessage.builder().message(scopeString).build())
                                      .collect(Collectors.toList()));

    errorBody.setMessage(exception.getMessage());

    return Response.status(BAD_REQUEST).entity(errorBody).build();
  }
}
