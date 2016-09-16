package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.ApplicationHost;
import software.wings.beans.ConfigFile;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.InfrastructureService;
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
@Api("service-templates")
@Path("/service-templates")
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class ServiceTemplateResource {
  /**
   * The Service template service.
   */
  @Inject ServiceTemplateService serviceTemplateService;
  @Inject private InfrastructureService infrastructureService;

  /**
   * List.
   *
   * @param envId       the env id
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<ServiceTemplate>> list(@QueryParam("envId") String envId,
      @QueryParam("appId") String appId, @BeanParam PageRequest<ServiceTemplate> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("envId", envId, EQ);
    return new RestResponse<>(serviceTemplateService.list(pageRequest, true));
  }

  /**
   * Creates the.
   *
   * @param envId           the env id
   * @param appId           the app id
   * @param serviceTemplate the service template
   * @return the rest response
   */
  @POST
  public RestResponse<ServiceTemplate> create(
      @QueryParam("envId") String envId, @QueryParam("appId") String appId, ServiceTemplate serviceTemplate) {
    serviceTemplate.setAppId(appId);
    serviceTemplate.setEnvId(envId);
    return new RestResponse<>(serviceTemplateService.save(serviceTemplate));
  }

  /**
   * Gets the.
   *
   * @param envId             the env id
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @return the rest response
   */
  @GET
  @Path("{templateId}")
  public RestResponse<ServiceTemplate> get(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId) {
    return new RestResponse<>(serviceTemplateService.get(appId, envId, serviceTemplateId, true));
  }

  /**
   * Update.
   *
   * @param envId             the env id
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @param serviceTemplate   the service template
   * @return the rest response
   */
  @PUT
  @Path("{templateId}")
  public RestResponse<ServiceTemplate> update(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId, ServiceTemplate serviceTemplate) {
    serviceTemplate.setAppId(appId);
    serviceTemplate.setEnvId(envId);
    serviceTemplate.setUuid(serviceTemplateId);
    return new RestResponse<>(serviceTemplateService.update(serviceTemplate));
  }

  /**
   * Delete.
   *
   * @param envId             the env id
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @return the rest response
   */
  @DELETE
  @Path("{templateId}")
  public RestResponse delete(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId) {
    serviceTemplateService.delete(appId, envId, serviceTemplateId);
    return new RestResponse();
  }

  /**
   * Map hosts.
   *
   * @param envId             the env id
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @param hostIds           the host ids
   * @return the rest response
   */
  @PUT
  @Path("{templateId}/map-hosts")
  public RestResponse<ServiceTemplate> mapHosts(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId, List<String> hostIds) {
    return new RestResponse<>(serviceTemplateService.updateHosts(appId, envId, serviceTemplateId, hostIds));
  }

  /**
   * Map tags.
   *
   * @param envId             the env id
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   * @param tagIds            the tag ids
   * @return the rest response
   */
  @PUT
  @Path("{templateId}/map-tags")
  public RestResponse<ServiceTemplate> mapTags(@QueryParam("envId") String envId, @QueryParam("appId") String appId,
      @PathParam("templateId") String serviceTemplateId, List<String> tagIds) {
    return new RestResponse<>(serviceTemplateService.updateTags(appId, envId, serviceTemplateId, tagIds));
  }

  /**
   * Host configs.
   *
   * @param envId      the env id
   * @param appId      the app id
   * @param templateId the template id
   * @return the rest response
   */
  @GET
  @Path("{templateId}/host-configs")
  public RestResponse<Map<String, List<ConfigFile>>> hostConfigs(@QueryParam("envId") String envId,
      @QueryParam("appId") String appId, @PathParam("templateId") String templateId) {
    return new RestResponse<>(serviceTemplateService.computedConfigFiles(appId, envId, templateId));
  }

  /**
   * Host configs.
   *
   * @param envId       the env id
   * @param appId       the app id
   * @param templateId  the template id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("{templateId}/tagged-hosts")
  public RestResponse<PageResponse<ApplicationHost>> hostConfigs(@QueryParam("envId") String envId,
      @QueryParam("appId") String appId, @PathParam("templateId") String templateId,
      @BeanParam PageRequest<ApplicationHost> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("infraId", infrastructureService.getInfraByEnvId(appId, envId), EQ);
    return new RestResponse<>(serviceTemplateService.getTaggedHosts(appId, envId, templateId, pageRequest));
  }

  /**
   * Override files rest response.
   *
   * @param envId      the env id
   * @param appId      the app id
   * @param templateId the template id
   * @return the rest response
   */
  @GET
  @Path("{templateId}/override-files")
  public RestResponse<List<ConfigFile>> overrideFiles(@QueryParam("envId") String envId,
      @QueryParam("appId") String appId, @PathParam("templateId") String templateId) {
    return new RestResponse<>(serviceTemplateService.getOverrideFiles(appId, envId, templateId));
  }
}
