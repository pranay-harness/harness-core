/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.exception;

import static io.harness.rest.RestResponse.Builder.aRestResponse;

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by peeyushaggarwal on 6/8/16.
 */
@Provider
@Slf4j
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
  @Override
  public Response toResponse(JsonProcessingException exception) {
    /*
     * If the error is in the JSON generation, it's a server error.
     */
    if (exception instanceof JsonGenerationException) {
      log.warn("Error generating JSON", exception);
      return Response.serverError()
          .entity(aRestResponse()
                      .withResponseMessages(singletonList(ResponseMessage.builder()
                                                              .code(ErrorCode.DEFAULT_ERROR_CODE)
                                                              .message("Error generating response")
                                                              .level(Level.ERROR)
                                                              .build()))
                      .build())
          .build();
    }

    final String message = exception.getOriginalMessage();

    /*
     * If we can't deserialize the JSON because someone forgot a no-arg constructor, it's a
     * server error and we should inform the developer.
     */
    if (message.startsWith("No suitable constructor found")) {
      log.error("Unable to deserialize the specific type", exception);
      return Response.serverError()
          .entity(aRestResponse()
                      .withResponseMessages(singletonList(ResponseMessage.builder()
                                                              .code(ErrorCode.DEFAULT_ERROR_CODE)
                                                              .message("Error reading request")
                                                              .level(Level.ERROR)
                                                              .build()))
                      .build())
          .build();
    }

    /*
     * Otherwise, it's those pesky users.
     */
    log.info("Unable to process JSON", exception);
    return Response.status(BAD_REQUEST)
        .entity(aRestResponse()
                    .withResponseMessages(singletonList(ResponseMessage.builder()
                                                            .code(ErrorCode.DEFAULT_ERROR_CODE)
                                                            .message("Unable to process JSON " + message)
                                                            .level(Level.ERROR)
                                                            .build()))
                    .build())
        .build();
  }
}
