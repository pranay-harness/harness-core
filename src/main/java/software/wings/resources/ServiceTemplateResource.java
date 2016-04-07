package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import software.wings.beans.*;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ServiceTemplateService;

import javax.ws.rs.*;

/**
 * Created by anubhaw on 4/4/16.
 */

@Path("/service_templates")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class ServiceTemplateResource {
  @Inject ServiceTemplateService serviceTemplateService;

  @GET
  @Path("{envID}")
  public RestResponse<PageResponse<ServiceTemplate>> list(
      @PathParam("envID") String envID, @BeanParam PageRequest<ServiceTemplate> pageRequest) {
    pageRequest.addFilter("envID", envID, SearchFilter.OP.EQ);
    return new RestResponse<>(serviceTemplateService.list(envID, pageRequest));
  }

  @POST
  @Path("{envID}")
  public RestResponse<ServiceTemplate> create(@PathParam("envID") String envID, ServiceTemplate serviceTemplate) {
    return new RestResponse<>(serviceTemplateService.createServiceTemplate(envID, serviceTemplate));
  }

  @PUT
  @Path("{envID}")
  public RestResponse<ServiceTemplate> update(@PathParam("envID") String envID, ServiceTemplate serviceTemplate) {
    return new RestResponse<>(serviceTemplateService.updateServiceTemplate(envID, serviceTemplate));
  }
}
