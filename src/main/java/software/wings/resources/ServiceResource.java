package software.wings.resources;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by anubhaw on 3/25/16.
 */
@Path("services")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceResource {
  @Inject private ServiceResourceService srs;

  @GET
  @Path("{appId}")
  public RestResponse<PageResponse<Service>> list(
      @PathParam("appId") String appId, @BeanParam PageRequest<Service> pageRequest) {
    pageRequest.addFilter("application", appId, EQ);
    return new RestResponse<>(srs.list(pageRequest));
  }

  @GET
  @Path("{appId}/{serviceId}")
  public RestResponse<Service> get(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(srs.get(appId, serviceId));
  }

  @POST
  @Path("{appId}")
  public RestResponse<Service> save(@PathParam("appId") String appId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(srs.save(appId, service));
  }

  @PUT
  @Path("{appId}/{serviceId}")
  public RestResponse<Service> update(
      @PathParam("appId") String appId, @PathParam("serviceId") String serviceId, Service service) {
    service.setAppId(appId);
    service.setUuid(serviceId);
    return new RestResponse<>(srs.update(service));
  }

  @DELETE
  @Path("{appId}/{serviceId}")
  public RestResponse<Service> delete(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    srs.delete(serviceId);
    return new RestResponse(of("status", "success"));
  }
}
