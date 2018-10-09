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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author Vaibhav Tulsyan
 * 08/Oct/2018
 */

@Api("cv")
@Path("/cv")
@Produces("application/json")
@Scope(ResourceType.SETTING) // <- is this correct? do i need to add anything to PermissionAttribute?
public class CVConfigurationResource {
  @Inject CVConfigurationService cvConfigurationService;

  @GET
  @Path("/get-configuration")
  @Timed
  @ExceptionMetered
  public <T extends CVConfiguration> RestResponse<T> getConfiguration(@QueryParam("accountId") final String accountId,
      @QueryParam("serviceConfigurationId") String serviceConfigurationId) {
    return new RestResponse<>(cvConfigurationService.getConfiguration(serviceConfigurationId));
  }

  @POST
  @Path("/configure")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveCVConfiguration(@QueryParam("accountId") final String accountId,
      @QueryParam("stateType") StateType stateType, @Body Object params) {
    return new RestResponse<>(cvConfigurationService.saveConfiguration(stateType, params));
  }
}
