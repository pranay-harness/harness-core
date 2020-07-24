package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

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

@Api("cv-config")
@Path("cv-config")
@Produces("application/json")
@ExposeInternalException
public class CVConfigResource {
  @Inject private CVConfigService cvConfigService;
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
    return new RestResponse<>(cvConfigService.save(cvConfig));
  }

  @POST
  @Path("batch")
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVConfig>> saveCVConfig(
      @QueryParam("accountId") @Valid final String accountId, @Body List<CVConfig> cvConfigs) {
    return new RestResponse<>(cvConfigService.save(cvConfigs));
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

  @GET
  @Path("/list")
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVConfig>> listCVConfigs(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("connectorId") String connectorId) {
    // keeping it simple for now. We will improve and evolve it based on more requirement on list api.
    return new RestResponse<>(cvConfigService.list(accountId, connectorId));
  }

  @GET
  @Path("/product-names")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> getProductNames(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("connectorId") String connectorId) {
    return new RestResponse<>(cvConfigService.getProductNames(accountId, connectorId));
  }
}
