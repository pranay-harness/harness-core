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
  @Path("environments/{applicationId}")
  public RestResponse<PageResponse<Environment>> listEnvironments(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<Environment> pageRequest) {
    pageRequest.addFilter("applicationId", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Environment>>(infraService.listEnvironments(pageRequest));
  }

  @POST
  @Path("environments/{applicationId}")
  public RestResponse<Environment> createEnvironment(
      @PathParam("applicationId") String applicationId, Environment environment) {
    return new RestResponse<Environment>(infraService.createEnvironment(applicationId, environment));
  }

  @GET
  @Path("hosts/{applicationId}")
  public RestResponse<PageResponse<Host>> listHosts(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter("applicationId", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Host>>(infraService.listHosts(pageRequest));
  }

  @POST
  @Path("hosts/{applicationId}")
  public RestResponse<Host> createHost(@PathParam("applicationId") String applicationId, Host host) {
    return new RestResponse<Host>(infraService.createHost(applicationId, host));
  }

  @GET
  @Path("host-mappings/{applicationId}")
  public RestResponse<PageResponse<HostInstanceMapping>> listHostInstanceMapping(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<HostInstanceMapping> pageRequest) {
    pageRequest.addFilter("applicationId", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<HostInstanceMapping>>(infraService.listHostInstanceMapping(pageRequest));
  }

  @POST
  @Path("host-mappings/{applicationId}")
  public RestResponse<HostInstanceMapping> createHostMapping(
      @PathParam("applicationId") String applicationId, HostInstanceMapping hostInstanceMapping) {
    return new RestResponse<HostInstanceMapping>(
        infraService.createHostInstanceMapping(applicationId, hostInstanceMapping));
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
