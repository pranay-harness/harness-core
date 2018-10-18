package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.SERVICE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.RestResponse;
import software.wings.common.VerificationConstants;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.CVDeploymentData;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesDataPoint;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cvdash")
@Path("/cvdash")
@Produces("application/json")
@Scope(SERVICE)
public class ContinuousVerificationDashboardResource {
  @Transient @Inject @SchemaIgnore protected ContinuousVerificationService continuousVerificationService;

  @GET
  @Path(VerificationConstants.CV_DASH_GET_RECORDS)
  @Timed
  @ExceptionMetered
  public RestResponse<LinkedHashMap<Long,
      LinkedHashMap<String,
          LinkedHashMap<String,
              LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>>
  getCVExecutionRecords(@QueryParam("accountId") String accountId, @QueryParam("beginEpochTs") long beginEpochTs,
      @QueryParam("endEpochTs") long endEpochTs) throws ParseException {
    return new RestResponse<>(continuousVerificationService.getCVExecutionMetaData(
        accountId, beginEpochTs, endEpochTs, UserThreadLocal.get().getPublicUser()));
  }

  @GET
  @Path(VerificationConstants.GET_ALL_CV_EXECUTIONS)
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ContinuousVerificationExecutionMetaData>> getAllCVExecutions(
      @QueryParam("accountId") String accountId, @QueryParam("beginEpochTs") long beginEpochTs,
      @QueryParam("endEpochTs") long endEpochTs, @QueryParam("isTimeSeries") boolean isTimeSeries,
      @BeanParam PageRequest<ContinuousVerificationExecutionMetaData> request) {
    return new RestResponse<>(continuousVerificationService.getAllCVExecutionsForTime(
        accountId, beginEpochTs, endEpochTs, isTimeSeries, request));
  }

  /**
   * Response format:
   * {"envName":
   *    [
   *      "cvConfiguration": {cvConfigurationObject},
   *      "riskLevelSummary": {
   *        "startTime": epoch in ms,
   *        "endTime": epoch in ms,
   *        "passed": int,
   *        "failed": int,
   *        "error": int,
   *        "timeSeries": null
   *      },
   *      "observedTimeSeries": {
   *        "transactionName": {
   *          "metricName": [
   *            {
   *              "timestamp": epoch in ms
   *              "value": floating point
   *            }
   *          ]
   *        }
   *      },
   *      "predictedTimeSeries": (same format as observedTimeSeries)
   *    ]
   * }
   *
   * The time series is an ordered list, ch
   *
   * @param accountId
   * @param serviceId
   * @param startTime
   * @param endTime
   * @return
   */
  @GET
  @Path(VerificationConstants.HEATMAP)
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, List<HeatMap>>> getDetailedHeatMap(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("serviceId") @Valid final String serviceId,
      @QueryParam("startTime") long startTime, @QueryParam("endTime") long endTime) {
    return new RestResponse<>(continuousVerificationService.getHeatMap(accountId, serviceId, startTime, endTime, true));
  }

  @GET
  @Path(VerificationConstants.GET_DEPLOYMENTS_24_7)
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVDeploymentData>> getCVExecutionDeploymentRecords(@QueryParam("accountId") String accountId,
      @QueryParam("startTime") long startTime, @QueryParam("endTime") long endTime,
      @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(continuousVerificationService.getCVDeploymentData(
        accountId, startTime, endTime, UserThreadLocal.get().getPublicUser(), serviceId));
  }

  @GET
  @Path("/heatmap-summary")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, List<HeatMap>>> getHeatMapSummary(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("serviceId") @Valid final String serviceId,
      @QueryParam("startTime") long startTime, @QueryParam("endTime") long endTime) {
    return new RestResponse<>(
        continuousVerificationService.getHeatMap(accountId, serviceId, startTime, endTime, false));
  }

  @GET
  @Path("/timeseries")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, Map<String, List<TimeSeriesDataPoint>>>> getTimeSeriesOfHeatMapUnit(
      @QueryParam("accountId") @Valid final String accountId, @QueryParam("startTime") long startTime,
      @QueryParam("endTime") long endTime, @QueryParam("cvConfigId") String cvConfigId) {
    return new RestResponse<>(
        continuousVerificationService.getTimeSeriesOfHeatMapUnit(accountId, cvConfigId, startTime, endTime));
  }
}
