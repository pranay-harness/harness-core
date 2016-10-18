package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;

import javax.inject.Inject;
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
@Api("artifactstream")
@Path("/artifactstream")
@Produces("application/json")
@Consumes("application/json")
public class ArtifactStreamResource {
  private ArtifactStreamService artifactStreamService;

  private AppService appService;

  /**
   * Instantiates a new release resource.
   *
   * @param artifactStreamService the release service
   * @param appService            the app service
   */
  @Inject
  public ArtifactStreamResource(ArtifactStreamService artifactStreamService, AppService appService) {
    this.appService = appService;
    this.artifactStreamService = artifactStreamService;
  }

  /**
   * Sets release service.
   *
   * @param artifactStreamService the release service
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
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(artifactStreamService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId the app id
   * @param id    the id
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  @Path("{id}")
  public RestResponse<ArtifactStream> get(@QueryParam("appId") String appId, @PathParam("id") String id) {
    return new RestResponse<>(artifactStreamService.get(id, appId));
  }

  /**
   * Endpoint to create a new release.
   *
   * @param appId          QueryParam app_id.
   * @param artifactStream the artifact source
   * @return newly created release.
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> save(@QueryParam("appId") String appId, ArtifactStream artifactStream) {
    try {
      if (!appService.exist(appId)) {
        throw new NotFoundException("application with id " + appId + " not found.");
      }
      artifactStream.setAppId(appId);
      return new RestResponse<>(artifactStreamService.create(artifactStream));
    } catch (Exception exception) {
      exception.printStackTrace();
      throw exception;
    }
  }

  /**
   * Endpoint to update a release.
   *
   * @param appId          QueryParam app_id.
   * @param id             the id
   * @param artifactStream the artifact source
   * @return release to be updated.
   */
  @PUT
  @Timed
  @ExceptionMetered
  @Path("{id}")
  public RestResponse<ArtifactStream> update(
      @QueryParam("appId") String appId, @PathParam("id") String id, ArtifactStream artifactStream) {
    artifactStream.setUuid(id);
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
  @Timed
  @ExceptionMetered
  @Path("{id}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("id") String id) {
    artifactStreamService.delete(id, appId);
    return new RestResponse<>();
  }
}
