package io.harness.delegate.resources;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.extern.slf4j.Slf4j;

@Api("/agent/delegates/tasks/v2")
@Path("/agent/delegates/tasks/v2")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
public class DelegateTasksResource {
  @DelegateAuth
  @GET
  @Path("dummyTask")
  @Timed
  @ExceptionMetered
  public RestResponse<String> getDummy() {
    log.info("Dummy endpoint!");
    return new RestResponse<>("Dummy endpoint!");
  }
}
