package software.wings.resources;

import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.execution.PlanExecution;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.rest.RestResponse;
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
  @Path("/http-switch")
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<PlanExecution> executeHttpSwitch() {
    return new RestResponse<>(customExecutionService.executeHttpSwitch());
  }

  @GET
  @Path("/http-fork")
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<PlanExecution> executeHttpFork() {
    return new RestResponse<>(customExecutionService.executeHttpFork());
  }

  @GET
  @Path("/http-section")
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<PlanExecution> executeSectionPlan() {
    return new RestResponse<>(customExecutionService.executeSectionPlan());
  }

  @GET
  @Path("/http-retry")
  @AuthRule(permissionType = DEPLOYMENT, action = EXECUTE, skipAuth = true)
  public RestResponse<PlanExecution> executeRetryPlan() {
    return new RestResponse<>(customExecutionService.executeRetryPlan());
  }
}
