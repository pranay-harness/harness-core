package io.harness.cvng.core.resources;

import static io.harness.cvng.core.CVNextGenConstants.CV_NEXTGEN_RESOURCE_PREFIX;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.core.services.api.DataSourceService;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.models.DataSourceType;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import retrofit2.http.Body;

import java.util.Collection;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(CV_NEXTGEN_RESOURCE_PREFIX + "/data-source")
@Path(CV_NEXTGEN_RESOURCE_PREFIX + "/data-source")
@Produces("application/json")
public class DataSourceResource {
  @Inject private DataSourceService dataSourceService;

  @GET
  @Path("/metric-packs")
  @Timed
  @ExceptionMetered
  public RestResponse<Collection<MetricPack>> getMetricPacks(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectId") @NotNull String projectId,
      @QueryParam("datasourceType") @NotNull DataSourceType dataSourceType) {
    return new RestResponse<>(dataSourceService.getMetricPacks(accountId, projectId, dataSourceType));
  }

  @POST
  @Path("/metric-packs")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveMetricPacks(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("projectId") @NotNull String projectId,
      @QueryParam("datasourceType") @NotNull DataSourceType dataSourceType,
      @NotNull @Valid @Body List<MetricPack> metricPacks) {
    return new RestResponse<>(dataSourceService.saveMetricPacks(accountId, projectId, dataSourceType, metricPacks));
  }
}
