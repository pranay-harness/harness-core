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
import java.util.List;

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
  @Path("{appID}")
  public RestResponse<List<Service>> list(@PathParam("appID") String appID) {
    return new RestResponse<>(srs.list(appID));
  }

  @POST
  @Path("{appID}")
  public RestResponse<Service> save(@PathParam("appID") String appID, Service service) {
    return new RestResponse<>(srs.save(appID, service));
  }

  @PUT
  @Path("{appID}")
  public RestResponse<Service> update(Service service) {
    return new RestResponse<>(srs.update(service));
  }
}
