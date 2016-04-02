package software.wings.resources;

import javax.inject.Inject;
import javax.ws.rs.*;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import software.wings.beans.*;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.InfraService;

@Path("/infra")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfraResource {
  @Inject private InfraService infraService;

  @GET
  @Path("hosts/{applicationId}")
  public RestResponse<PageResponse<Host>> listHosts(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter("applicationId", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Host>>(infraService.listHosts(pageRequest));
  }

  @GET
  @Path("hosts/{applicationID}/{hostID}")
  public RestResponse<Host> listHosts(@PathParam("applicationID") String appID, @PathParam("hostID") String hostID) {
    return new RestResponse<>(infraService.getHost(appID, hostID));
  }

  @POST
  @Path("hosts/{applicationId}")
  public RestResponse<Host> createHost(@PathParam("applicationId") String applicationId, Host host) {
    return new RestResponse<Host>(infraService.createHost(applicationId, host));
  }

  @POST
  @Path("tags")
  public RestResponse<Tag> saveTag(Tag tag) {
    return new RestResponse<>(infraService.createTag(tag));
  }

  @PUT
  @Path("hosts/{hostID}/tag/{tagID}")
  public RestResponse<Host> applyTag(@PathParam("hostID") String hostID, @PathParam("tagID") String tagID) {
    return new RestResponse<>(infraService.applyTag(hostID, tagID));
  }
}
