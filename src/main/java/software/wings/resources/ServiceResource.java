package software.wings.resources;

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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by anubhaw on 3/25/16.
 */
@Path("/services")
@Timed
@ExceptionMetered
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServiceResource {
  @Inject private ServiceResourceService srs;

  @GET
  public RestResponse<PageResponse<Service>> list(@BeanParam PageRequest<Service> pageRequest) {
    return new RestResponse<>(srs.list(pageRequest));
  }

  @POST
  public RestResponse<Service> save(Service service) {
    return new RestResponse<>(srs.save(service));
  }

  @PUT
  public RestResponse<Service> update(Service service) {
    return new RestResponse<>(srs.update(service));
  }
}
