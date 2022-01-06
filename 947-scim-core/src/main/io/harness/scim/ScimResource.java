/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
