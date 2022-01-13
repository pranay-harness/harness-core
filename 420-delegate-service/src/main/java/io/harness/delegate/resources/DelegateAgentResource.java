/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.resources;

import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.extern.slf4j.Slf4j;

@Api("/agent/delegates/v2")
@Path("/agent/delegates/v2")
@Produces("application/json")
//@Scope(DELEGATE)
@Slf4j
public class DelegateAgentResource {
  @DelegateAuth
  @GET
  @Path("dummy")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getDummy() {
    log.info("Dummy endpoint!");
    return new RestResponse<>("Dummy endpoint!");
  }
}
