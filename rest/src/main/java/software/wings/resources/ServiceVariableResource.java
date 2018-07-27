package software.wings.resources;

import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.common.Constants.SECRET_MASK;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
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
// ToBeRevisited
// Both service and env overrides variables use the same rest end points. So, no annotation can be determined
@Scope(ResourceType.APPLICATION)
public class ServiceVariableResource {
  @Inject private ServiceVariableService serviceVariablesService;
  @Inject private AppService appService;
  @Inject private ServiceTemplateService serviceTemplateService;

  /**
   * List rest response.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ServiceVariable>> list(@BeanParam PageRequest<ServiceVariable> pageRequest) {
    return new RestResponse<>(serviceVariablesService.list(pageRequest, true));
  }

  /**
   * Save rest response.
   *
   * @param appId           the app id
   * @param serviceVariable the service variable
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceVariable> save(@QueryParam("appId") String appId, ServiceVariable serviceVariable) {
    serviceVariable.setAppId(appId);
    serviceVariable.setAccountId(appService.get(appId).getAccountId());

    // TODO:: revisit. for environment envId can be specific
    String envId =
        serviceVariable.getEntityType().equals(SERVICE) || serviceVariable.getEntityType().equals(ENVIRONMENT)
        ? GLOBAL_ENV_ID
        : serviceTemplateService.get(serviceVariable.getAppId(), serviceVariable.getTemplateId()).getEnvId();
    serviceVariable.setEnvId(envId);
    ServiceVariable savedServiceVariable = serviceVariablesService.save(serviceVariable);
    if (savedServiceVariable.getType().equals(ENCRYPTED_TEXT)) {
      serviceVariable.setValue(SECRET_MASK.toCharArray());
    }
    if (savedServiceVariable.getOverriddenServiceVariable() != null
        && savedServiceVariable.getOverriddenServiceVariable().getType().equals(ENCRYPTED_TEXT)) {
      savedServiceVariable.getOverriddenServiceVariable().setValue(SECRET_MASK.toCharArray());
    }
    return new RestResponse<>(savedServiceVariable);
  }

  /**
   * Get rest response.
   *
   * @param appId             the app id
   * @param serviceVariableId the service variable id
   * @return the rest response
   */
  @GET
  @Path("{serviceVariableId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ServiceVariable> get(
      @QueryParam("appId") String appId, @PathParam("serviceVariableId") String serviceVariableId) {
    ServiceVariable serviceVariable = serviceVariablesService.get(appId, serviceVariableId, true);
    return new RestResponse<>(serviceVariable);
  }

  /**
   * Update rest response.
   *
   * @param appId             the app id
   * @param serviceVariableId the service variable id
   * @param serviceVariable   the service variable
   * @return the rest response
   */
  @PUT
  @Path("{serviceVariableId}")
  @Timed
  @ExceptionMetered
  public RestResponse update(@QueryParam("appId") String appId,
      @PathParam("serviceVariableId") String serviceVariableId, ServiceVariable serviceVariable) {
    serviceVariable.setUuid(serviceVariableId);
    serviceVariable.setAppId(appId);
    ServiceVariable savedServiceVariable = serviceVariablesService.update(serviceVariable);
    if (savedServiceVariable.getType().equals(ENCRYPTED_TEXT)) {
      serviceVariable.setValue(SECRET_MASK.toCharArray());
    }
    if (savedServiceVariable.getOverriddenServiceVariable() != null
        && savedServiceVariable.getOverriddenServiceVariable().getType().equals(ENCRYPTED_TEXT)) {
      savedServiceVariable.getOverriddenServiceVariable().setValue(SECRET_MASK.toCharArray());
    }
    return new RestResponse<>(savedServiceVariable);
  }

  /**
   * Delete rest response.
   *
   * @param appId             the app id
   * @param serviceVariableId the service variable id
   * @return the rest response
   */
  @DELETE
  @Path("{serviceVariableId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(
      @QueryParam("appId") String appId, @PathParam("serviceVariableId") String serviceVariableId) {
    serviceVariablesService.delete(appId, serviceVariableId);
    return new RestResponse();
  }

  /**
   * Delete by entity rest response.
   *
   * @param appId      the app id
   * @param entityId   the entity id
   * @return the rest response
   */
  @DELETE
  @Path("/entity/{entityId}")
  @Timed
  @ExceptionMetered
  public RestResponse deleteByEntity(@QueryParam("appId") String appId, @PathParam("entityId") String entityId) {
    serviceVariablesService.pruneByService(appId, entityId);
    return new RestResponse();
  }
}
