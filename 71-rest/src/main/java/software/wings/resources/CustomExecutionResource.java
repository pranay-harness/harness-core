package software.wings.resources;

import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.Redesign;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.rest.RestResponse;
import io.harness.state.execution.ExecutionInstance;
import software.wings.security.annotations.AuthRule;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Redesign
@Path("/execute2")
@Produces("application/json")
public class CustomExecutionResource {
  @Inject CustomExecutionService customExecutionService;

  @GET
  @Timed
  @ExceptionMetered
  @Path("/http-switch")
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<ExecutionInstance> executeHttpSwitch() {
    return new RestResponse<>(customExecutionService.executeHttpSwitch());
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/http-fork")
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<ExecutionInstance> executeHttpFork() {
    return new RestResponse<>(customExecutionService.executeHttpFork());
  }
}
