/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.exceptionmappers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.FailureDTO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;

@OwnedBy(PL)
@Slf4j
public class OptimisticLockingFailureExceptionMapper implements ExceptionMapper<OptimisticLockingFailureException> {
  @Override
  public Response toResponse(OptimisticLockingFailureException exception) {
    log.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);
    FailureDTO failureDTO = FailureDTO.toBody(Status.FAILURE, ErrorCode.OPTIMISTIC_LOCKING_EXCEPTION,
        "Request failed as you have an older version of entity, "
            + "please reload the page and try again.",
        null);
    return Response.status(Response.Status.BAD_REQUEST).entity(failureDTO).type(MediaType.APPLICATION_JSON).build();
  }
}
