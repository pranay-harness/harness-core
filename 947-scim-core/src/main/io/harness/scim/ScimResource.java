package io.harness.scim;

import com.google.common.collect.Sets;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ScimResource {
  protected Response getExceptionResponse(Exception ex, int statusCode, Status status) {
    ScimError scimError = ScimError.builder()
                              .status(statusCode)
                              .detail(ex.getMessage())
                              .schemas(Sets.newHashSet("urn:ietf:params:scim:api:messages:2.0:Error"))
                              .build();
    return Response.status(status).entity(scimError).build();
  }
}
