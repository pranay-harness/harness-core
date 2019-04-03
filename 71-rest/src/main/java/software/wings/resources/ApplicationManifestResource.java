package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.yaml.directory.DirectoryNode;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.CREATE)
  public RestResponse<ApplicationManifest> createApplicationManifest(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, ApplicationManifest applicationManifest) {
    applicationManifest.setAppId(appId);
    applicationManifest.setServiceId(serviceId);
    applicationManifest.setKind(AppManifestKind.K8S_MANIFEST);
    return new RestResponse<>(applicationManifestService.create(applicationManifest));
  }

  @GET
  @Path("{appManifestId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ApplicationManifest> getApplicationManifest(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @PathParam("appManifestId") String appManifestId) {
    return new RestResponse<>(applicationManifestService.getById(appId, appManifestId));
  }

  @PUT
  @Path("{appManifestId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<ApplicationManifest> updateApplicationManifest(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @PathParam("appManifestId") String appManifestId,
      ApplicationManifest applicationManifest) {
    applicationManifest.setUuid(appManifestId);
    applicationManifest.setAppId(appId);
    applicationManifest.setServiceId(serviceId);
    return new RestResponse<>(applicationManifestService.update(applicationManifest));
  }

  @DELETE
  @Path("{appManifestId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.DELETE)
  public RestResponse deleteApplicationManifest(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @PathParam("appManifestId") String appManifestId) {
    applicationManifestService.deleteAppManifest(appId, appManifestId);
    return new RestResponse();
  }

  @POST
  @Path("{appManifestId}/manifest-file")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.CREATE)
  public RestResponse<ManifestFile> createManifestFile(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @PathParam("appManifestId") String appManifestId,
      ManifestFile manifestFile) {
    manifestFile.setAppId(appId);
    manifestFile.setApplicationManifestId(appManifestId);
    return new RestResponse<>(applicationManifestService.createManifestFileByServiceId(manifestFile, serviceId));
  }

  @GET
  @Path("{appManifestId}/manifest-file/{manifestFileId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ManifestFile> getManifestFile(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @PathParam("appManifestId") String appManifestId,
      @PathParam("manifestFileId") String manifestFileId) {
    return new RestResponse<>(applicationManifestService.getManifestFileById(appId, manifestFileId));
  }

  @PUT
  @Path("{appManifestId}/manifest-file/{manifestFileId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.UPDATE)
  public RestResponse<ManifestFile> updateManifestFile(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @PathParam("appManifestId") String appManifestId,
      @PathParam("manifestFileId") String manifestFileId, ManifestFile manifestFile) {
    manifestFile.setUuid(manifestFileId);
    manifestFile.setAppId(appId);
    manifestFile.setApplicationManifestId(appManifestId);
    return new RestResponse<>(applicationManifestService.updateManifestFileByServiceId(manifestFile, serviceId));
  }

  @DELETE
  @Path("{appManifestId}/manifest-file/{manifestFileId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.DELETE)
  public RestResponse deleteManifestFile(@QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId,
      @PathParam("appManifestId") String appManifestId, @PathParam("manifestFileId") String manifestFileId) {
    applicationManifestService.deleteManifestFileById(appId, manifestFileId);
    return new RestResponse();
  }

  @GET
  @Path("{appManifestId}/manifest-files-from-git/")
  @Timed
  @ExceptionMetered
  public RestResponse<DirectoryNode> getManifestFilesFromGit(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @PathParam("appManifestId") String appManifestId) {
    return new RestResponse<>(applicationManifestService.getManifestFilesFromGit(appId, appManifestId));
  }

  @GET
  @Path("{appManifestId}/manifest-files")
  @Timed
  @ExceptionMetered
  public RestResponse<List<ManifestFile>> listManifestFiles(
      @PathParam("appManifestId") String appManifestId, @QueryParam("appId") String appId) {
    return new RestResponse<>(applicationManifestService.listManifestFiles(appManifestId, appId));
  }
}