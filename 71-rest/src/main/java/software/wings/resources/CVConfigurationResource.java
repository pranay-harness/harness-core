package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author Vaibhav Tulsyan
 * 08/Oct/2018
 */

@Api("cv-configuration")
@Path("/cv-configuration")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class CVConfigurationResource {
  @Inject CVConfigurationService cvConfigurationService;

  @GET
  @Path("{serviceConfigurationId}")
  @Timed
  @ExceptionMetered
  public <T extends CVConfiguration> RestResponse<T> getConfiguration(
      @QueryParam("accountId") @Valid final String accountId,
      @PathParam("serviceConfigurationId") String serviceConfigurationId) {
    return new RestResponse<>(cvConfigurationService.getConfiguration(serviceConfigurationId));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveCVConfiguration(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("appId") @Valid final String appId, @QueryParam("stateType") StateType stateType,
      @Body Object params) {
    return new RestResponse<>(cvConfigurationService.saveConfiguration(accountId, appId, stateType, params));
  }

  @GET
  @Timed
  @ExceptionMetered
  public <T extends CVConfiguration> RestResponse<List<T>> listConfigurations(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("appId") @Valid final String appId,
      @QueryParam("envId") @Valid final String envId) {
    return new RestResponse<>(cvConfigurationService.listConfigurations(accountId, appId, envId));
  }

  @PUT
  @Path("{serviceConfigurationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> updateCVConfiguration(@PathParam("serviceConfigurationId") String serviceConfigurationId,
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("appId") @Valid final String appId,
      @QueryParam("stateType") StateType stateType, @Body Object params) {
    return new RestResponse<>(
        cvConfigurationService.updateConfiguration(accountId, appId, stateType, params, serviceConfigurationId));
  }

  @DELETE
  @Path("{serviceConfigurationId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteCVConfiguration(@PathParam("serviceConfigurationId") String serviceConfigurationId,
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("appId") @Valid final String appId) {
    return new RestResponse<>(cvConfigurationService.deleteConfiguration(accountId, appId, serviceConfigurationId));
  }
}
