package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceTemplate;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
  public RestResponse<PageResponse<ServiceTemplate>> list(
      @QueryParam("envId") String envId, @BeanParam PageRequest<ServiceTemplate> pageRequest) {
    pageRequest.addFilter("envId", envId, EQ);
    return new RestResponse<>(serviceTemplateService.list(envId, pageRequest));
  }

  @POST
  public RestResponse<ServiceTemplate> create(ServiceTemplate serviceTemplate) {
    return new RestResponse<>(serviceTemplateService.createServiceTemplate(serviceTemplate));
  }

  @PUT
  @Path("{templateId}")
  public RestResponse<ServiceTemplate> update(
      @PathParam("templateId") String serviceTemplateId, ServiceTemplate serviceTemplate) {
    serviceTemplate.setUuid(serviceTemplateId);
    return new RestResponse<>(serviceTemplateService.updateServiceTemplate(serviceTemplate));
  }

  @PUT
  @Path("{templateId}/map_hosts")
  public RestResponse<ServiceTemplate> mapHosts(@PathParam("templateId") String serviceTemplateId,
      @FormDataParam("tags") List<String> tagIds, @FormDataParam("hosts") List<String> hostIds) {
    return new RestResponse<>(serviceTemplateService.updateHostAndTags(serviceTemplateId, tagIds, hostIds));
  }
}
