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
  @Path("{envId}")
  public RestResponse<PageResponse<ServiceTemplate>> list(
      @PathParam("envId") String envId, @BeanParam PageRequest<ServiceTemplate> pageRequest) {
    pageRequest.addFilter("envId", envId, SearchFilter.OP.EQ);
    return new RestResponse<>(serviceTemplateService.list(envId, pageRequest));
  }

  @POST
  @Path("{envId}")
  public RestResponse<ServiceTemplate> create(@PathParam("envId") String envId, ServiceTemplate serviceTemplate) {
    return new RestResponse<>(serviceTemplateService.createServiceTemplate(envId, serviceTemplate));
  }

  @PUT
  @Path("{envId}")
  public RestResponse<ServiceTemplate> update(@PathParam("envId") String envId, ServiceTemplate serviceTemplate) {
    return new RestResponse<>(serviceTemplateService.updateServiceTemplate(envId, serviceTemplate));
  }
}
