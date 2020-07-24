package io.harness.cvng.core.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.beans.SplunkSampleResponse;
import io.harness.cvng.beans.SplunkSavedSearch;
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
@ExposeInternalException(withStackTrace = true)
public class SplunkResource {
  @Inject private SplunkService splunkService;
  @GET
  @Path("saved-searches")
  @Timed
  @ExceptionMetered
  public RestResponse<List<SplunkSavedSearch>> getSavedSearches(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("requestGuid") @NotNull String requestGuid) {
    return new RestResponse<>(splunkService.getSavedSearches(accountId, connectorId, requestGuid));
  }

  @GET
  @Path("histogram")
  @Timed
  @ExceptionMetered
  public RestResponse<CVHistogram> getHistogram(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("query") String query,
      @QueryParam("requestGuid") @NotNull String requestGuid) {
    return new RestResponse<>(splunkService.getHistogram(accountId, connectorId, query, requestGuid));
  }

  @GET
  @Path("samples")
  @Timed
  @ExceptionMetered
  public RestResponse<SplunkSampleResponse> getSamples(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorId") String connectorId, @QueryParam("query") String query,
      @QueryParam("requestGuid") @NotNull String requestGuid) {
    return new RestResponse<>(splunkService.getSamples(accountId, connectorId, query, requestGuid));
  }
}
