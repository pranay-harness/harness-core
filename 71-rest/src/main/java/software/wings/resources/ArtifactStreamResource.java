package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.Map;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * ArtifactStreamResource class.
 *
 * @author Rishi
 */
@Api("artifactstreams")
@Path("/artifactstreams")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
@AuthRule(permissionType = SERVICE, skipAuth = true)
public class ArtifactStreamResource {
  private ArtifactStreamService artifactStreamService;
  private AppService appService;

  /**
   * Instantiates a new Artifact stream resource.
   *
   * @param artifactStreamService the artifact stream service
   * @param appService            the app service
   */
  @Inject
  public ArtifactStreamResource(ArtifactStreamService artifactStreamService, AppService appService) {
    this.appService = appService;
    this.artifactStreamService = artifactStreamService;
  }

  /**
   * Sets artifact stream service.
   *
   * @param artifactStreamService the artifact stream service
   */
  public void setArtifactStreamService(ArtifactStreamService artifactStreamService) {
    this.artifactStreamService = artifactStreamService;
  }

  /**
   * Sets app service.
   *
   * @param appService the app service
   */
  public void setAppService(AppService appService) {
    this.appService = appService;
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
  public RestResponse<PageResponse<ArtifactStream>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<ArtifactStream> pageRequest) {
    pageRequest.addFilter("appId", EQ, appId);
    return new RestResponse<>(artifactStreamService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId    the app id
   * @param streamId the stream id
   * @return the rest response
   */
  @GET
  @Path("{streamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> get(@QueryParam("appId") String appId, @PathParam("streamId") String streamId) {
    return new RestResponse<>(artifactStreamService.get(appId, streamId));
  }

  /**
   * Save rest response.
   *
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> save(@QueryParam("appId") String appId, ArtifactStream artifactStream) {
    if (!appService.exist(appId)) {
      throw new NotFoundException("application with id " + appId + " not found.");
    }
    artifactStream.setAppId(appId);
    return new RestResponse<>(artifactStreamService.create(artifactStream));
  }

  /**
   * Update rest response.
   *
   * @param appId          the app id
   * @param streamId       the stream id
   * @param artifactStream the artifact stream
   * @return the rest response
   */
  @PUT
  @Path("{streamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> update(
      @QueryParam("appId") String appId, @PathParam("streamId") String streamId, ArtifactStream artifactStream) {
    artifactStream.setUuid(streamId);
    artifactStream.setAppId(appId);
    return new RestResponse<>(artifactStreamService.update(artifactStream));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @param id    the id
   * @return the rest response
   */
  @DELETE
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("id") String id) {
    artifactStreamService.delete(appId, id);
    return new RestResponse<>();
  }

  /**
   * Gets build source types.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the build source types
   */
  @GET
  @Path("buildsource-types")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getBuildSourceTypes(
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(artifactStreamService.getSupportedBuildSourceTypes(appId, serviceId));
  }
}
