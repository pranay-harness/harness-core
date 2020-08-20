package io.harness.cvng.dashboard.services.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.AnomalyDTO;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

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
public class AnomalyResource {
  @Inject private AnomalyService anomalyService;

  @GET
  @Timed
  @ExceptionMetered
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
