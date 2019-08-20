package software.wings.resources;

import com.unboundid.scim2.common.messages.ErrorResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ScimResource {
  protected Response getExceptionResponse(Exception ex, int statusCode, Status conflict) {
    ErrorResponse errorResponse = new ErrorResponse(statusCode);
    errorResponse.setDetail(ex.getMessage());
    return Response.status(conflict).entity(errorResponse).build();
  }
}
