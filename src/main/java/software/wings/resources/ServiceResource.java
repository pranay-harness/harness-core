package software.wings.resources;

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
import javax.ws.rs.QueryParam;

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
  public RestResponse<PageResponse<Service>> list(
      @QueryParam("app_id") String appId, @BeanParam PageRequest<Service> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(srs.list(pageRequest));
  }

  @GET
  @Path("{service_id}")
  public RestResponse<Service> get(@PathParam("service_id") String serviceId) {
    return new RestResponse<>(srs.get(serviceId));
  }

  @POST
  public RestResponse<Service> save(@QueryParam("app_id") String appId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(srs.save(service));
  }

  @PUT
  @Path("{service_id}")
  public RestResponse<Service> update(@PathParam("service_id") String serviceId, Service service) {
    service.setUuid(serviceId);
    return new RestResponse<>(srs.update(service));
  }

  @DELETE
  @Path("{service_id}")
  public RestResponse delete(@PathParam("service_id") String serviceId) {
    srs.delete(serviceId);
    return new RestResponse();
  }
}
