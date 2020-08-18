package software.wings.resources.customdeployment;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.customdeployment.CustomDeploymentTypeDTO;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeService;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("customDeploymentType")
@Path("/customDeploymentType")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AuthRule(permissionType = ACCOUNT)
public class CustomDeploymentTypeResource {
  @Inject private CustomDeploymentTypeService customDeploymentTypeService;

  @GET
  @Path("{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<List<CustomDeploymentTypeDTO>> list(@PathParam("accountId") String accountId,
      @QueryParam("appId") String appId, @QueryParam("withDetails") boolean withDetails) {
    return new RestResponse<>(customDeploymentTypeService.list(accountId, appId, withDetails));
  }

  @GET
  @Path("{accountId}/{templateId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<CustomDeploymentTypeDTO> get(@PathParam("accountId") String accountId,
      @PathParam("templateId") String templateId, @QueryParam("version") String version) {
    return new RestResponse<>(customDeploymentTypeService.get(accountId, templateId, version));
  }
}
