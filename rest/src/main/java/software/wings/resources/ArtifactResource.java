package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;

import java.io.File;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * ArtifactResource.
 *
 * @author Rishi
 */
@Api("artifacts")
@Path("/artifacts")
@Produces("application/json")
@Scope(APPLICATION)
@AuthRule(permissionType = SERVICE)
public class ArtifactResource {
  private ArtifactService artifactService;
  private ArtifactStreamService artifactStreamService;

  /**
   * Instantiates a new artifact resource.
   *
   * @param artifactService the artifact service
   * @param artifactStreamService
   */
  @Inject
  public ArtifactResource(ArtifactService artifactService, ArtifactStreamService artifactStreamService) {
    this.artifactService = artifactService;
    this.artifactStreamService = artifactStreamService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ, dbFieldName = "serviceIds")
  public RestResponse<PageResponse<Artifact>> list(@QueryParam("appId") String appId,
      @QueryParam("serviceId") String serviceId, @BeanParam PageRequest<Artifact> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    return new RestResponse<>(artifactService.listSortByBuildNo(appId, serviceId, pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the rest response
   */
  @GET
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ, skipAuth = true)
  public RestResponse<Artifact> get(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId) {
    return new RestResponse<>(artifactService.get(appId, artifactId, true));
  }

  /**
   * Save.
   *
   * @param appId    the app id
   * @param artifact the artifact
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Artifact> save(@QueryParam("appId") String appId, Artifact artifact) {
    artifact.setAppId(appId);
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifact.getArtifactStreamId());
    artifact.setDisplayName(artifactStream.getArtifactDisplayName(artifact.getBuildNo()));
    return new RestResponse<>(artifactService.create(artifact));
  }

  /**
   * Update.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @param artifact   the artifact
   * @return the rest response
   */
  @PUT
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ, skipAuth = true)
  public RestResponse<Artifact> update(
      @QueryParam("appId") String appId, @PathParam("artifactId") String artifactId, Artifact artifact) {
    artifact.setUuid(artifactId);
    artifact.setAppId(appId);
    return new RestResponse<>(artifactService.update(artifact));
  }

  /**
   * Update.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the rest response
   */
  @DELETE
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId) {
    artifactService.delete(appId, artifactId);
    return new RestResponse();
  }

  /**
   * Download.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the response
   */
  @GET
  @Path("{artifactId}/artifactFile")
  @Encoded
  @Timed
  @ExceptionMetered
  @AuthRule(skipAuth = true)
  public Response download(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId) {
    File artifactFile = artifactService.download(appId, artifactId);
    ResponseBuilder response = Response.ok(artifactFile, MediaType.APPLICATION_OCTET_STREAM);
    response.header("Content-Disposition", "attachment; filename=" + artifactFile.getName());
    return response.build();
  }

  /**
   * Save.
   * @param appId    the app id
   * @param artifact the artifact
   * @return the rest response
   */
  @PUT
  @Timed
  @ExceptionMetered
  public RestResponse<Artifact> collectArtifactContent(@QueryParam("appId") String appId, Artifact artifact) {
    artifact.setAppId(appId);
    return new RestResponse<>(artifactService.startArtifactCollection(appId, artifact.getUuid()));
  }
}
