package software.wings.resources;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ScimResource {
  protected Response getExceptionResponse(Exception ex, int statusCode, Status conflict) {
    ScimError scimError = ScimError.builder().status(statusCode).detail(ex.getMessage()).build();
    return Response.status(conflict).entity(scimError).build();
  }
}
