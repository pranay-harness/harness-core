package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
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

// TODO: Auto-generated Javadoc

/**
 * ReleaseResource class.
 *
 * @author Rishi
 */
@Api("releases")
@Path("/releases")
@Produces("application/json")
@Consumes("application/json")
public class ReleaseResource {
  private ReleaseService releaseService;

  private AppService appService;

  /**
   * Instantiates a new release resource.
   *
   * @param releaseService the release service
   * @param appService     the app service
   */
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
  public RestResponse<PageResponse<Release>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Release> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(releaseService.list(pageRequest));
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
  public RestResponse<Release> get(@QueryParam("appId") String appId, @PathParam("id") String id) {
    return new RestResponse<>(releaseService.get(id, appId));
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

  /**
   * Endpoint to update a release.
   *
   * @param appId   QueryParam app_id.
   * @param id      the id
   * @param release release to be created.
   * @return release to be updated.
   */
  @PUT
  @Timed
  @ExceptionMetered
  @Path("{id}")
  public RestResponse<Release> update(@QueryParam("appId") String appId, @PathParam("id") String id, Release release) {
    release.setUuid(id);
    release.setAppId(appId);
    return new RestResponse<>(releaseService.update(release));
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
  public RestResponse<Release> delete(@QueryParam("appId") String appId, @PathParam("id") String id) {
    return new RestResponse<>(releaseService.softDelete(id, appId));
  }

  /**
   * Adds the artifact source.
   *
   * @param appId          the app id
   * @param id             the id
   * @param artifactSource the artifact source
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  @Path("{id}/artifactsources")
  public RestResponse<Release> addArtifactSource(
      @QueryParam("appId") String appId, @PathParam("id") String id, ArtifactSource artifactSource) {
    return new RestResponse<>(releaseService.addArtifactSource(id, appId, artifactSource));
  }

  /**
   * Delete artifact source.
   *
   * @param appId              the app id
   * @param id                 the id
   * @param artifactSourceName the artifact source name
   * @return the rest response
   */
  @DELETE
  @Timed
  @ExceptionMetered
  @Path("{id}/artifactsources/{artifactSourceName}")
  public RestResponse<Release> deleteArtifactSource(@QueryParam("appId") String appId, @PathParam("id") String id,
      @PathParam("artifactSourceName") String artifactSourceName) {
    return new RestResponse<>(releaseService.deleteArtifactSource(id, appId, artifactSourceName));
  }
}
