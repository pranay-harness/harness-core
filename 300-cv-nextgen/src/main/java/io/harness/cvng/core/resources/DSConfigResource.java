package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import retrofit2.http.Body;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ds-config")
@Path("ds-config")
@Produces("application/json")
@ExposeInternalException
public class DSConfigResource {
  @Inject private DSConfigService dsConfigService;
  @Inject private CVConfigService cvConfigService;
  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets list of data source configs", nickname = "getDataSourceConfigs")
  public RestResponse<List<DSConfig>> getDataSourceCVConfigs(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier, @QueryParam("productName") String productName) {
    return new RestResponse<>(dsConfigService.list(accountId, connectorIdentifier, productName));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "saves a data source config", nickname = "saveDataSourceCVConfig")
  public void saveDataSourceCVConfig(@QueryParam("accountId") @Valid final String accountId, @Body DSConfig dsConfig) {
    dsConfigService.upsert(dsConfig);
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "deletes all data source configs for a group", nickname = "deleteDataSourceCVConfigByGroup")
  public void deleteByGroup(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier, @QueryParam("productName") String productName,
      @QueryParam("identifier") String identifier) {
    dsConfigService.delete(accountId, connectorIdentifier, productName, identifier);
  }

  @GET
  @Path("/env-to-services")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets list of env to services mapping for which data sources are configured",
      nickname = "getEnvToServicesList")
  public RestResponse<List<EnvToServicesDTO>>
  getEnvToServicesList(@QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull final String projectIdentifier) {
    return new RestResponse<>(cvConfigService.getEnvToServicesList(accountId, orgIdentifier, projectIdentifier));
  }
}
