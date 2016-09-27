package software.wings.resources;

import static software.wings.beans.ServiceVariable.DEFAULT_TEMPLATE_ID;

import com.google.inject.Inject;

import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceVariableService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by peeyushaggarwal on 9/26/16.
 */
@Api("service-variables")
@Path("/service-variables")
@Produces("application/json")
public class ServiceVariableResource {
  private ServiceVariableService serviceVariablesService;

  @Inject
  public ServiceVariableResource(ServiceVariableService serviceVariablesService) {
    this.serviceVariablesService = serviceVariablesService;
  }

  @GET
  public RestResponse<PageResponse<ServiceVariable>> list(@BeanParam PageRequest<ServiceVariable> pageRequest) {
    return new RestResponse<>(serviceVariablesService.list(pageRequest));
  }

  @POST
  public RestResponse<ServiceVariable> save(@QueryParam("appId") String appId, ServiceVariable serviceVariable) {
    serviceVariable.setAppId(appId);
    return new RestResponse<>(serviceVariablesService.save(serviceVariable));
  }

  @GET
  @Path("{serviceVariableId}")
  public RestResponse<ServiceVariable> get(
      @QueryParam("appId") String appId, @PathParam("serviceVariableId") String serviceVariableId) {
    return new RestResponse<>(serviceVariablesService.get(appId, serviceVariableId));
  }

  @PUT
  @Path("{serviceVariableId}")
  public RestResponse update(@QueryParam("appId") String appId,
      @PathParam("serviceVariableId") String serviceVariableId, ServiceVariable serviceVariable) {
    serviceVariable.setUuid(serviceVariableId);
    return new RestResponse<>(serviceVariablesService.update(serviceVariable));
  }

  @DELETE
  @Path("{serviceVariableId}")
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("serviceVariableId") String serviceVariableId) {
    serviceVariablesService.delete(appId, serviceVariableId);
    return new RestResponse();
  }

  @DELETE
  @Path("/entity/{entityId}")
  public RestResponse deleteByEntity(@QueryParam("appId") String appId,
      @DefaultValue(DEFAULT_TEMPLATE_ID) @QueryParam("templateId") String templateId,
      @PathParam("entityId") String entityId) {
    serviceVariablesService.deleteByEntityId(appId, templateId, entityId);
    return new RestResponse();
  }
}
