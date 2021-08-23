package io.harness.delegate.resources;

import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;

import software.wings.annotations.DelegateServiceResource;

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
@DelegateServiceResource
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
