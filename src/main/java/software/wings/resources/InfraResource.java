package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.Tag;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.InfraService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/infra")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfraResource {
  @Inject private InfraService infraService;

  @GET
  @Path("envId")
  public RestResponse<PageResponse<Infra>> listInfra(
      @PathParam("envId") String envId, @BeanParam PageRequest<Infra> pageRequest) {
    pageRequest.addFilter("envId", envId, SearchFilter.Operator.EQ);
    return new RestResponse<>(infraService.listInfra(envId, pageRequest));
  }

  @POST
  @Path("envId")
  public RestResponse<Infra> createInfra(@PathParam("envId") String envId, Infra infra) {
    return new RestResponse<>(infraService.createInfra(infra, envId));
  }

  @GET
  @Path("/{infraId}/hosts")
  public RestResponse<PageResponse<Host>> listHosts(
      @PathParam("infraId") String infraId, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter("infraId", infraId, SearchFilter.Operator.EQ);
    return new RestResponse<PageResponse<Host>>(infraService.listHosts(pageRequest));
  }

  @GET
  @Path("{infraId}/hosts/{hostId}")
  public RestResponse<Host> listHosts(@PathParam("infraId") String infraId, @PathParam("hostId") String hostId) {
    return new RestResponse<>(infraService.getHost(infraId, hostId));
  }

  @POST
  @Path("{infraId}/hosts")
  public RestResponse<Host> createHost(@PathParam("infraId") String infraId, Host host) {
    return new RestResponse<Host>(infraService.createHost(infraId, host));
  }

  @PUT
  @Path("{infraId}/hosts")
  public RestResponse<Host> updateHost(@PathParam("infraId") String infraId, Host host) {
    return new RestResponse<Host>(infraService.updateHost(infraId, host));
  }

  @POST
  @Path("tags/{envId}")
  public RestResponse<Tag> saveTag(@PathParam("envId") String envId, Tag tag) {
    return new RestResponse<>(infraService.createTag(envId, tag));
  }

  @PUT
  @Path("hosts/{hostId}/tag/{tagId}")
  public RestResponse<Host> applyTag(@PathParam("hostId") String hostId, @PathParam("tagId") String tagId) {
    return new RestResponse<>(infraService.applyTag(hostId, tagId));
  }
}
