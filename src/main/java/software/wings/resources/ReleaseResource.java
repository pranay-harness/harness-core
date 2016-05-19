package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Application;
import software.wings.beans.ArtifactSource;
import software.wings.beans.Release;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ReleaseService;

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
 * ReleaseResource class.
 *
 * @author Rishi
 */
@Path("/releases")
@Produces("application/json")
@Consumes("application/json")
public class ReleaseResource {
  private ReleaseService releaseService;

  private AppService appService;

  @Inject
  public ReleaseResource(ReleaseService releaseService, AppService appService) {
    this.appService = appService;
    this.releaseService = releaseService;
  }

  public void setReleaseService(ReleaseService releaseService) {
    this.releaseService = releaseService;
  }

  public void setAppService(AppService appService) {
    this.appService = appService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Release>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Release> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(releaseService.list(pageRequest));
  }

  /**
   * Endpoint to create a new release.
   *
   * @param appId   QueryParam app_id.
   * @param release release to be created.
   * @return newly created release.
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Release> save(@QueryParam("appId") String appId, Release release) {
    try {
      Application application = appService.findByUuid(appId);
      if (application == null) {
        throw new NotFoundException("application with id " + appId + " not found.");
      }
      release.setAppId(appId);
      return new RestResponse<>(releaseService.create(release));
    } catch (Exception exception) {
      exception.printStackTrace();
      throw exception;
    }
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("{id}")
  public RestResponse<Release> update(
      @QueryParam("appId") String appId, @PathParam("id") String releaseId, Release release) {
    release.setUuid(releaseId);
    return new RestResponse<>(releaseService.update(release));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("{id}/artifactsources")
  public RestResponse<Release> addArtifactSource(
      @QueryParam("appId") String appId, @PathParam("id") String releaseId, ArtifactSource artifactSource) {
    return new RestResponse<>(releaseService.addArtifactSource(releaseId, artifactSource));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("{id}/artifactsources")
  public RestResponse<Release> deleteArtifactSource(
      @QueryParam("appId") String appId, @PathParam("id") String releaseId, @BeanParam ArtifactSource artifactSource) {
    return new RestResponse<>(releaseService.deleteArtifactSource(releaseId, artifactSource));
  }
}
