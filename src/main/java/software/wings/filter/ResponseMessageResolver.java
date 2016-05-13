package software.wings.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorConstants;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;

import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ResponseMessageResolver<T> implements ExceptionMapper<Throwable> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public Response toResponse(Throwable exception) {
    logger.error("Exception occured", exception);
    RestResponse<T> restResponse = new RestResponse<>();

    WingsException we = getWingsExceptionFromCause(exception);
    if (we != null) {
      List<ResponseMessage> responseMessageList = we.getResponseMessageList();
      if (responseMessageList != null && responseMessageList.size() > 0) {
        for (ResponseMessage responseMessage : responseMessageList) {
          ResponseMessage rm =
              ResponseCodeCache.getInstance().getResponseMessage(responseMessage.getCode(), we.getParams());
          if (rm != null) {
            restResponse.getResponseMessages().add(rm);
          }
        }
      }
    }

    // No known exception or error code
    if (restResponse.getResponseMessages().size() == 0) {
      restResponse.getResponseMessages().add(
          ResponseCodeCache.getInstance().getResponseMessage(ErrorConstants.DEFAULT_ERROR_CODE));
    }

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(restResponse)
        .type(MediaType.APPLICATION_JSON)
        .build();
  }

  private WingsException getWingsExceptionFromCause(Throwable throwable) {
    while (throwable != null) {
      if (throwable instanceof WingsException) {
        WingsException e = (WingsException) throwable;
        return e;
      }
      throwable = throwable.getCause();
    }
    return null;
  }
}
