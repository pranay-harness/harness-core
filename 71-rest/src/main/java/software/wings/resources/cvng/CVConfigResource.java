package software.wings.resources.cvng;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.CVConfigService;
import io.harness.cvng.models.CVConfig;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cv-nextgen/cv-config/")
@Path("/cv-nextgen/cv-config")
@Produces("application/json")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class CVConfigResource {
  @Inject CVConfigService cvConfigService;
  @GET
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  public RestResponse<CVConfig> getCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @PathParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(cvConfigService.get(cvConfigId));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<CVConfig> saveCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @Body CVConfig cvConfig) {
    return new RestResponse<>(cvConfigService.save(accountId, cvConfig));
  }

  @PUT
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> updateCVConfig(@PathParam("cvConfigId") String cvConfigId,
      @QueryParam("accountId") @Valid final String accountId, @Body CVConfig cvConfig) {
    return null;
  }

  @DELETE
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteCVConfig(
      @PathParam("cvConfigId") String cvConfigId, @QueryParam("accountId") @Valid final String accountId) {
    return null;
  }
}