/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.exception;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.ReportTarget.REST_API;

import io.harness.eraro.ResponseMessage;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;

import com.google.inject.Singleton;
import io.grpc.Status;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class WingsExceptionGrpcMapper implements GrpcExceptionMapper<WingsException> {
  @Override
  public Status toStatus(WingsException throwable) {
    Status status = null;
    if (throwable != null) {
      ExceptionLogger.logProcessedMessages(throwable, MANAGER, log);
      List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(throwable, REST_API);
      if (isNotEmpty(responseMessages)) {
        status = Status.INTERNAL.withDescription(responseMessages.get(0).getMessage()).withCause(throwable);
      } else {
        status = Status.INTERNAL.withDescription(throwable.toString()).withCause(throwable);
      }
    }
    return status;
  }

  @Override
  public Class getClazz() {
    return WingsException.class;
  }
}
