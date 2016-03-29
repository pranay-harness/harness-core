package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.intfc.ServiceResourceService;

import javax.ws.rs.*;
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
  RestResponse<Service> save(Service service) {
    return new RestResponse<>(srs.save(service));
  }

  @PUT
  RestResponse<Service> update(Service service) {
    return new RestResponse<>(srs.update(service));
  }
}
