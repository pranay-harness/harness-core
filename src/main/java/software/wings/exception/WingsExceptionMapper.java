package software.wings.exception;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static software.wings.beans.RestResponse.Builder.aRestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorConstants;
import software.wings.beans.ResponseMessage;

import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
public class WingsExceptionMapper implements ExceptionMapper<WingsException> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /* (non-Javadoc)
   * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
   */
  @Override
  public Response toResponse(WingsException ex) {
    logger.error("Exception occured", ex);
    return Response.status(resolveHttpStatus(ex.getResponseMessageList()))
        .entity(aRestResponse().withResponseMessages(ex.getResponseMessageList()).build())
        .build();
  }

  private Status resolveHttpStatus(List<ResponseMessage> responseMessageList) {
    ErrorConstants errorCode = null;
    if (responseMessageList != null && responseMessageList.size() > 0) {
      errorCode = responseMessageList.get(responseMessageList.size() - 1).getCode();
    }
    if (errorCode != null) {
      return errorCode.getStatus();
    } else {
      return INTERNAL_SERVER_ERROR;
    }
  }
}
