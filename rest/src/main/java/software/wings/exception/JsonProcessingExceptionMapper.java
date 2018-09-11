package software.wings.exception;

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.beans.RestResponse.Builder.aRestResponse;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by peeyushaggarwal on 6/8/16.
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonProcessingExceptionMapper.class);

  @Override
  public Response toResponse(JsonProcessingException exception) {
    /*
     * If the error is in the JSON generation, it's a server error.
     */
    if (exception instanceof JsonGenerationException) {
      LOGGER.warn("Error generating JSON", exception);
      return Response.serverError()
          .entity(aRestResponse()
                      .withResponseMessages(singletonList(aResponseMessage()
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
      LOGGER.error("Unable to deserialize the specific type", exception);
      return Response.serverError()
          .entity(aRestResponse()
                      .withResponseMessages(singletonList(aResponseMessage()
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
    LOGGER.info("Unable to process JSON", exception);
    return Response.status(BAD_REQUEST)
        .entity(aRestResponse()
                    .withResponseMessages(singletonList(aResponseMessage()
                                                            .code(ErrorCode.DEFAULT_ERROR_CODE)
                                                            .message("Unable to process JSON " + message)
                                                            .level(Level.ERROR)
                                                            .build()))
                    .build())
        .build();
  }
}
