package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/platforms")
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class PlatformResource {
  //
  //	private PlatformService platformService;
  //
  //	@Inject
  //	public PlatformResource(PlatformService platformService) {
  //		this.platformService = platformService;
  //	}
  //
  //	@GET
  //	@Path("{applicationId}")
  //	public RestResponse<PageResponse<PlatformSoftware>> list(@PathParam("applicationId") String applicationId,
  //@BeanParam PageRequest<PlatformSoftware> pageRequest) {
  //		pageRequest.addFilter("application", applicationId, SearchFilter.OP.EQ);
  //		return new RestResponse<>(platformService.list(pageRequest));
  //	}
  //
  //	@POST
  //	@Path("{applicationId}")
  //	public RestResponse<PlatformSoftware> save(@PathParam("applicationId") String applicationId, PlatformSoftware
  //platform) {
  //		platform.setApplication(WingsBootstrap.lookup(AppService.class).findByUUID(applicationId));
  //		return new RestResponse<>(platformService.create(platform));
  //	}
}
