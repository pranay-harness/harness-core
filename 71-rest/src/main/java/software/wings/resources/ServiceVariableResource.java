package software.wings.resources;

import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.MASKED;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.ServiceVariable;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.auth.AuthHandler;
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
  @Inject private AuthHandler authHandler;

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
    return new RestResponse<>(serviceVariablesService.list(pageRequest, MASKED));
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
    return new RestResponse<>(serviceVariablesService.saveWithChecks(appId, serviceVariable));
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
    ServiceVariable serviceVariable = serviceVariablesService.get(appId, serviceVariableId, MASKED);
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
    return new RestResponse<>(serviceVariablesService.updateWithChecks(appId, serviceVariableId, serviceVariable));
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
    serviceVariablesService.deleteWithChecks(appId, serviceVariableId);
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
