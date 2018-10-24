package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.ApplicationManifestService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("app-manifests")
@Path("/app-manifests")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(SERVICE)
public class ApplicationManifestResource {
  @Inject private ApplicationManifestService applicationManifestService;

  @POST
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.CREATE)
  public RestResponse<ApplicationManifest> createApplicationManifest(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, ApplicationManifest applicationManifest) {
    applicationManifest.setAppId(appId);
    applicationManifest.setServiceId(serviceId);
    return new RestResponse<>(applicationManifestService.create(applicationManifest));
  }

  @GET
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ApplicationManifest> getApplicationManifest(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @BeanParam PageRequest<ApplicationManifest> pageRequest) {
    return new RestResponse<>(applicationManifestService.get(appId, serviceId));
  }

  @PUT
  @Path("{serviceId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<ApplicationManifest> updateApplicationManifest(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, ApplicationManifest applicationManifest) {
    applicationManifest.setAppId(appId);
    applicationManifest.setServiceId(serviceId);
    return new RestResponse<>(applicationManifestService.update(applicationManifest));
  }
}