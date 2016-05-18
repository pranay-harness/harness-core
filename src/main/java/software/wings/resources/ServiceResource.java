package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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
      @QueryParam("appId") String appId, @BeanParam PageRequest<Service> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(srs.list(pageRequest));
  }

  @GET
  @Path("{serviceId}")
  public RestResponse<Service> get(@PathParam("serviceId") String serviceId) {
    return new RestResponse<>(srs.get(serviceId));
  }

  @POST
  public RestResponse<Service> save(@QueryParam("appId") String appId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(srs.save(service));
  }

  @PUT
  @Path("{serviceId}")
  public RestResponse<Service> update(@PathParam("serviceId") String serviceId, Service service) {
    service.setUuid(serviceId);
    return new RestResponse<>(srs.update(service));
  }

  @DELETE
  @Path("{serviceId}")
  public RestResponse delete(@PathParam("serviceId") String serviceId) {
    srs.delete(serviceId);
    return new RestResponse();
  }
}
