package software.wings.resources;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
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
import software.wings.beans.PlatformSoftware;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PlatformService;

@Path("/platforms")
public class PlatformResource {
  private PlatformService platformService;

  public PlatformResource() {
    platformService = WingsBootstrap.lookup(PlatformService.class);
  }

  @GET
  @Path("{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<PlatformSoftware>> list(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<PlatformSoftware> pageRequest) {
    pageRequest.addFilter("application", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<PlatformSoftware>>(platformService.list(pageRequest));
  }

  @POST
  @Path("{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PlatformSoftware> save(
      @PathParam("applicationId") String applicationId, PlatformSoftware platform) {
    platform.setApplication(WingsBootstrap.lookup(AppService.class).findByUUID(applicationId));
    return new RestResponse<PlatformSoftware>(platformService.create(platform));
  }
}
