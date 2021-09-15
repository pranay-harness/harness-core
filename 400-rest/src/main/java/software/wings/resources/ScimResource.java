/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.resources;

import com.google.common.collect.Sets;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ScimResource {
  Response getExceptionResponse(Exception ex, int statusCode, Status status) {
    ScimError scimError = ScimError.builder()
                              .status(statusCode)
                              .detail(ex.getMessage())
                              .schemas(Sets.newHashSet("urn:ietf:params:scim:api:messages:2.0:Error"))
                              .build();
    return Response.status(status).entity(scimError).build();
  }
}
