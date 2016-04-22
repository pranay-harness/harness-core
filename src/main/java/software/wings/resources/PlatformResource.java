package software.wings.resources;

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

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/platforms")
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class PlatformResource {
  private PlatformService platformService;

  @Inject
  public PlatformResource(PlatformService platformService) {
    this.platformService = platformService;
  }

  @GET
  @Path("{applicationId}")
  public RestResponse<PageResponse<PlatformSoftware>> list(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<PlatformSoftware> pageRequest) {
    pageRequest.addFilter("application", applicationId, SearchFilter.Operator.EQ);
    return new RestResponse<>(platformService.list(pageRequest));
  }

  @POST
  @Path("{applicationId}")
  public RestResponse<PlatformSoftware> save(
      @PathParam("applicationId") String applicationId, PlatformSoftware platform) {
    platform.setApplication(WingsBootstrap.lookup(AppService.class).findByUuid(applicationId));
    return new RestResponse<>(platformService.create(platform));
  }
}
