package software.wings.exception;

import software.wings.beans.RestResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static software.wings.beans.RestResponse.Builder.aRestResponse;

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
public class WingsExceptionMapper implements ExceptionMapper<WingsException> {
  @Override
  public Response toResponse(WingsException e) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(aRestResponse().withResponseMessages(e.getResponseMessageList()).build())
        .build();
  }
}
