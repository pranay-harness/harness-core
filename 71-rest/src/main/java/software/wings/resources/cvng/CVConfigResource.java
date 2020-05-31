package software.wings.resources.cvng;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.CVConfigService;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

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

  @POST
  @Path("batch")
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVConfig>> saveCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @Body List<CVConfig> cvConfigs) {
    return new RestResponse<>(cvConfigService.save(accountId, cvConfigs));
  }

  @PUT
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  public RestResponse<CVConfig> updateCVConfig(@PathParam("cvConfigId") String cvConfigId,
      @QueryParam("accountId") @Valid final String accountId, @Body CVConfig cvConfig) {
    cvConfig.setUuid(cvConfigId);
    cvConfigService.update(cvConfig);
    return new RestResponse<>(cvConfig);
  }

  @PUT
  @Path("batch")
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVConfig>> updateCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @Body List<CVConfig> cvConfigs) {
    cvConfigService.update(cvConfigs);
    return new RestResponse<>(cvConfigs);
  }

  @DELETE
  @Path("{cvConfigId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> deleteCVConfig(
      @PathParam("cvConfigId") String cvConfigId, @QueryParam("accountId") @Valid final String accountId) {
    cvConfigService.delete(cvConfigId);
    return null;
  }

  @DELETE
  @Path("batch")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> deleteCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @Body List<String> cvConfigIds) {
    cvConfigService.delete(cvConfigIds);
    return null;
  }

  @GET
  @Path("/list")
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVConfig>> listCVConfigs(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("connectorId") String connectorId) {
    // keeping it simple for now. We will improve and evolve it based on more requirement on list api.
    return new RestResponse<>(cvConfigService.list(accountId, connectorId));
  }
}