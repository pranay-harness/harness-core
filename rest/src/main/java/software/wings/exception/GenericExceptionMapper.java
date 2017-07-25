package software.wings.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.common.cache.ResponseCodeCache;
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
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
   */
  @Override
  public Response toResponse(Throwable exception) {
    Misc.error(logger, "Exception occurred: " + exception.getMessage(), exception);
    RestResponse<T> restResponse = new RestResponse<>();

    // No known exception or error code
    if (restResponse.getResponseMessages().size() == 0) {
      restResponse.getResponseMessages().add(
          ResponseCodeCache.getInstance().getResponseMessage(ErrorCode.DEFAULT_ERROR_CODE));
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(restResponse)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }
}
