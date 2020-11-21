package io.harness.cvng.core.resources;

import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("splunk/")
@Path("splunk")
@Produces("application/json")
@NextGenManagerAuth
public class SplunkResource {
  @Inject private SplunkService splunkService;
  @GET
  @Path("saved-searches")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets saved searches in splunk", nickname = "getSavedSearches")
  public RestResponse<List<SplunkSavedSearch>> getSavedSearches(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("requestGuid") @NotNull String requestGuid) {
    return new RestResponse<>(
        splunkService.getSavedSearches(accountId, connectorIdentifier, orgIdentifier, projectIdentifier, requestGuid));
  }

  @GET
  @Path("validation")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "validates given setting for splunk data source", nickname = "getValidationResponseForSplunk")
  public RestResponse<SplunkValidationResponse> getValidationResponse(
      @NotNull @QueryParam("accountId") final String accountId,
      @NotNull @QueryParam("connectorIdentifier") String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @NotNull @NotEmpty @QueryParam("query") String query, @QueryParam("requestGuid") @NotNull String requestGuid) {
    return new RestResponse<>(splunkService.getValidationResponse(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, query, requestGuid));
  }
}
