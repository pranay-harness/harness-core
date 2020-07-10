package io.harness.cvng.core.dashboard.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;

import java.time.Instant;
import java.util.Map;
import java.util.SortedSet;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("heatmap")
@Path("/heatmap")
@Produces("application/json")
public class HeatMapResource {
  @Inject private HeatMapService heatMapService;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<Map<CVMonitoringCategory, SortedSet<HeatMapDTO>>> getCVConfig(
      @QueryParam("accountId") @NotNull final String accountId,
      @QueryParam("serviceIdentifier") @NotNull final String serviceIdentifier,
      @QueryParam("envIdentifier") @NotNull final String envIdentifier,
      @QueryParam("startTimeMs") @NotNull final Long startTimeMs,
      @QueryParam("endTimeMs") @NotNull final Long endTimeMs) {
    return new RestResponse<>(heatMapService.getHeatMap(accountId, serviceIdentifier, envIdentifier,
        Instant.ofEpochMilli(startTimeMs), Instant.ofEpochMilli(endTimeMs)));
  }
}
