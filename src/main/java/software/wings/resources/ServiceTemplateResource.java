package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.ConfigFile;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceTemplate;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.List;
import java.util.Map;
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
 * Created by anubhaw on 4/4/16.
 */
@Path("/service-templates")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class ServiceTemplateResource {
  @Inject ServiceTemplateService serviceTemplateService;

  @GET
  public RestResponse<PageResponse<ServiceTemplate>> list(@QueryParam("envId") String envId,
      @QueryParam("appId") String appId, @BeanParam PageRequest<ServiceTemplate> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("envId", envId, EQ);
    return new RestResponse<>(serviceTemplateService.list(pageRequest));
  }

  @POST
  public RestResponse<ServiceTemplate> create(
      @QueryParam("envId") String envId, @QueryParam("appId") String appId, ServiceTemplate serviceTemplate) {
    serviceTemplate.setAppId(appId);
    serviceTemplate.setEnvId(envId);
    return new RestResponse<>(serviceTemplateService.save(serviceTemplate));
  }

  @GET
  @Path("{templateId}")
  public RestResponse<ServiceTemplate> get(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId) {
    return new RestResponse<>(serviceTemplateService.get(appId, envId, serviceTemplateId));
  }

  @PUT
  @Path("{templateId}")
  public RestResponse<ServiceTemplate> update(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId, ServiceTemplate serviceTemplate) {
    serviceTemplate.setAppId(appId);
    serviceTemplate.setEnvId(envId);
    serviceTemplate.setUuid(serviceTemplateId);
    return new RestResponse<>(serviceTemplateService.update(serviceTemplate));
  }

  @DELETE
  @Path("{templateId}")
  public RestResponse delete(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId) {
    serviceTemplateService.delete(appId, envId, serviceTemplateId);
    return new RestResponse();
  }

  @PUT
  @Path("{templateId}/map-hosts")
  public RestResponse<ServiceTemplate> mapHosts(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId, List<String> hostIds) {
    return new RestResponse<>(serviceTemplateService.updateHosts(appId, serviceTemplateId, hostIds));
  }

  @GET
  @Path("{templateId}/host-configs")
  public RestResponse<Map<String, List<ConfigFile>>> hostConfigs(@QueryParam("envId") String envId,
      @QueryParam("appId") String appId, @PathParam("templateId") String templateId) {
    return new RestResponse<>(serviceTemplateService.computedConfigFiles(appId, envId, templateId));
  }
}
