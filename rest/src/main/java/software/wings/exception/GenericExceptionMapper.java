package software.wings.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import io.harness.eraro.ErrorCodeName;
import io.harness.eraro.Level;
import io.harness.eraro.MessageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.utils.Misc;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * The Class GenericExceptionMapper.
 *
 * @param <T> the generic type
 */
public class GenericExceptionMapper<T> implements ExceptionMapper<Throwable> {
  private static final Logger logger = LoggerFactory.getLogger(GenericExceptionMapper.class);

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
   */
  @Override
  public Response toResponse(Throwable exception) {
    logger.error("Exception occurred: " + Misc.getMessage(exception), exception);
    RestResponse<T> restResponse = new RestResponse<>();

    // No known exception or error code
    if (restResponse.getResponseMessages().isEmpty()) {
      restResponse.getResponseMessages().add(
          aResponseMessage()
              .code(DEFAULT_ERROR_CODE)
              .level(Level.ERROR)
              .message(MessageManager.getInstance().prepareMessage(
                  ErrorCodeName.builder().value(DEFAULT_ERROR_CODE.name()).build(), null))
              .build());
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(restResponse)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
