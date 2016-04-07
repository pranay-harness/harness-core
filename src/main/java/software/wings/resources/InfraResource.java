package software.wings.resources;

import javax.inject.Inject;
import javax.ws.rs.*;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import software.wings.beans.*;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.InfraService;

import java.util.List;

@Path("/infra")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfraResource {
  @Inject private InfraService infraService;

  @GET
  @Path("envID")
  public RestResponse<PageResponse<Infra>> listInfra(
      @PathParam("envID") String envID, @BeanParam PageRequest<Infra> pageRequest) {
    pageRequest.addFilter("envID", envID, SearchFilter.OP.EQ);
    return new RestResponse<>(infraService.listInfra(envID, pageRequest));
  }

  @POST
  @Path("envID")
  public RestResponse<Infra> createInfra(@PathParam("envID") String envID, Infra infra) {
    return new RestResponse<>(infraService.createInfra(infra, envID));
  }

  @GET
  @Path("/{infraID}/hosts")
  public RestResponse<PageResponse<Host>> listHosts(
      @PathParam("infraID") String infraID, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter("infraID", infraID, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Host>>(infraService.listHosts(pageRequest));
  }

  @GET
  @Path("{infraID}/hosts/{hostID}")
  public RestResponse<Host> listHosts(@PathParam("infraID") String infraID, @PathParam("hostID") String hostID) {
    return new RestResponse<>(infraService.getHost(infraID, hostID));
  }

  @POST
  @Path("{infraID}/hosts")
  public RestResponse<Host> createHost(@PathParam("infraID") String infraID, Host host) {
    return new RestResponse<Host>(infraService.createHost(infraID, host));
  }

  @POST
  @Path("{infraID}/hosts")
  public RestResponse<Host> updateHost(@PathParam("infraID") String infraID, Host host) {
    return new RestResponse<Host>(infraService.updateHost(infraID, host));
  }

  @POST
  @Path("tags/{envID}")
  public RestResponse<Tag> saveTag(@PathParam("envID") String envID, Tag tag) {
    return new RestResponse<>(infraService.createTag(envID, tag));
  }

  @PUT
  @Path("hosts/{hostID}/tag/{tagID}")
  public RestResponse<Host> applyTag(@PathParam("hostID") String hostID, @PathParam("tagID") String tagID) {
    return new RestResponse<>(infraService.applyTag(hostID, tagID));
  }
}
