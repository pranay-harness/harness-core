package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.cvng.core.services.api.SplunkService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("splunk/")
@Path("splunk")
@Produces("application/json")
public class SplunkResource {
  @Inject private SplunkService splunkService;
  @GET
  @Path("saved-searches")
  @Timed
  @ExceptionMetered
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
  public RestResponse<SplunkValidationResponse> getValidationResponse(
      @NotNull @QueryParam("accountId") final String accountId,
      @NotNull @QueryParam("connectorIdentifier") String connectorIdentifier,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier, @NotNull @QueryParam("query") String query,
      @QueryParam("requestGuid") @NotNull String requestGuid) {
    return new RestResponse<>(splunkService.getValidationResponse(
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, query, requestGuid));
  }
}
