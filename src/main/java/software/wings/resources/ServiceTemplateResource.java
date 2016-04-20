package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceTemplate;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ServiceTemplateService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
