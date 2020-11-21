package io.harness.cvng.dashboard.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.AnomalyDTO;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("anomaly")
@Path("/anomaly")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class AnomalyResource {
  @Inject private AnomalyService anomalyService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "get list of anomalies in a given time range", nickname = "getAnomalies")
  public RestResponse<List<AnomalyDTO>> getAnomalies(@QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("serviceIdentifier") @NotNull final String serviceIdentifier,
      @QueryParam("envIdentifier") @NotNull final String envIdentifier,
      @QueryParam("category") @NotNull final CVMonitoringCategory category,
      @QueryParam("startTimeMs") @NotNull final Long startTimeMs,
      @QueryParam("endTimeMs") @NotNull final Long endTimeMs) {
    return new RestResponse<>(anomalyService.getAnomalies(accountId, serviceIdentifier, envIdentifier, category,
        Instant.ofEpochMilli(startTimeMs), Instant.ofEpochMilli(endTimeMs)));
  }
}
