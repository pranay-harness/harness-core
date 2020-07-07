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
import java.util.Set;
import javax.validation.Valid;
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
  public RestResponse<Set<HeatMapDTO>> getCVConfig(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("serviceIdentifier") @Valid final String serviceIdentifier,
      @QueryParam("envIdentifier") @Valid final String envIdentifier,
      @QueryParam("category") @Valid final CVMonitoringCategory category,
      @QueryParam("startTimeMs") @Valid final Long startTimeMs, @QueryParam("endTimeMs") @Valid final Long endTimeMs) {
    return new RestResponse<>(heatMapService.getHeatMap(accountId, serviceIdentifier, envIdentifier, category,
        Instant.ofEpochMilli(startTimeMs), Instant.ofEpochMilli(endTimeMs)));
  }
}
