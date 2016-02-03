package software.wings.resources;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import software.wings.app.WingsBootstrap;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ReleaseService;

/**
 *  ReleaseResource class.
 *
 *
 * @author Rishi
 *
 */
@Path("/releases")
public class ReleaseResource {
  private ReleaseService releaseService;

  public ReleaseResource() {
    releaseService = WingsBootstrap.lookup(ReleaseService.class);
  }

  @GET
  @Path("{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<Release>> list(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<Release> pageRequest) {
    pageRequest.addFilter("application", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Release>>(releaseService.list(pageRequest));
  }

  @POST
  @Path("{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Release> save(@PathParam("applicationId") String applicationId, Release release) {
    release.setApplication(WingsBootstrap.lookup(AppService.class).findByUUID(applicationId));
    return new RestResponse<Release>(releaseService.create(release));
  }
}
