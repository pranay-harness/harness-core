package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Command;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceResourceService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 3/25/16.
 */
@Api("services")
@Path("services")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceResource {
  private ServiceResourceService serviceResourceService;

  @Inject
  public ServiceResource(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }

  @GET
  public RestResponse<PageResponse<Service>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Service> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(serviceResourceService.list(pageRequest));
  }

  @GET
  @Path("{serviceId}")
  public RestResponse<Service> get(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(serviceResourceService.get(appId, serviceId));
  }

  @POST
  public RestResponse<Service> save(@QueryParam("appId") String appId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.save(service));
  }

  @PUT
  @Path("{serviceId}")
  public RestResponse<Service> update(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, Service service) {
    service.setUuid(serviceId);
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.update(service));
  }

  @DELETE
  @Path("{serviceId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    serviceResourceService.delete(appId, serviceId);
    return new RestResponse();
  }

  @POST
  @Path("{serviceId}/commands")
  public RestResponse<Service> saveCommand(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, Command command) {
    command.setServiceId(serviceId);
    return new RestResponse<>(serviceResourceService.addCommand(appId, serviceId, command));
  }

  @DELETE
  @Path("{serviceId}/command/{commandName}")
  public RestResponse<Service> deleteCommand(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("commandName") String commandName) {
    return new RestResponse<>(serviceResourceService.deleteCommand(appId, serviceId, commandName));
  }
}
