package software.wings.exception;

import static software.wings.beans.RestResponse.Builder.aRestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
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
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(aRestResponse().withResponseMessages(ex.getResponseMessageList()).build())
        .build();
  }
}
