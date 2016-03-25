package software.wings.resources;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.HostInstanceMapping;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.service.intfc.InfraService;

@Path("/infra")
public class InfraResources {
  private InfraService infraService;

  @Inject
  public InfraResources(InfraService infraService) {
    this.infraService = infraService;
  }

  @GET
  @Path("environments/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<Environment>> listEnvironments(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<Environment> pageRequest) {
    pageRequest.addFilter("applicationId", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Environment>>(infraService.listEnvironments(pageRequest));
  }

  @POST
  @Path("environments/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Environment> createEnvironment(
      @PathParam("applicationId") String applicationId, Environment environment) {
    return new RestResponse<Environment>(infraService.createEnvironment(applicationId, environment));
  }

  @GET
  @Path("hosts/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<Host>> listHosts(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter("applicationId", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Host>>(infraService.listHosts(pageRequest));
  }

  @POST
  @Path("hosts/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Host> createHost(@PathParam("applicationId") String applicationId, Host host) {
    return new RestResponse<Host>(infraService.createHost(applicationId, host));
  }

  @GET
  @Path("host-mappings/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<HostInstanceMapping>> listHostInstanceMapping(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<HostInstanceMapping> pageRequest) {
    pageRequest.addFilter("applicationId", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<HostInstanceMapping>>(infraService.listHostInstanceMapping(pageRequest));
  }

  @POST
  @Path("host-mappings/{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<HostInstanceMapping> createHostMapping(
      @PathParam("applicationId") String applicationId, HostInstanceMapping hostInstanceMapping) {
    return new RestResponse<HostInstanceMapping>(
        infraService.createHostInstanceMapping(applicationId, hostInstanceMapping));
  }
}
