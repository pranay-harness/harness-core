package software.wings.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.ErrorCodeName;
import io.harness.eraro.Level;
import io.harness.eraro.MessageManager;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExceptionUtils;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * The Class GenericExceptionMapper.
 *
 * @param <T> the generic type
 */
@Slf4j
public class GenericExceptionMapper<T> implements ExceptionMapper<Throwable> {
  @Override
  public Response toResponse(Throwable exception) {
    logger.error("Exception occurred: " + ExceptionUtils.getMessage(exception), exception);

    if (exception instanceof ClientErrorException) {
      return getHttpErrorResponse((ClientErrorException) exception);
    } else {
      return getDefaultResponse();
    }
  }

  private Response getHttpErrorResponse(ClientErrorException exception) {
    return Response.status(exception.getResponse().getStatus())
        .entity(new RestResponse<>())
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private Response getDefaultResponse() {
    RestResponse<T> restResponse = new RestResponse<>();

    restResponse.getResponseMessages().add(
        ResponseMessage.builder()
            .code(ErrorCode.DEFAULT_ERROR_CODE)
            .level(Level.ERROR)
            .message(MessageManager.getInstance().prepareMessage(
                ErrorCodeName.builder().value(DEFAULT_ERROR_CODE.name()).build(), null, null))
            .build());

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
        .entity(restResponse)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
