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

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
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

  @GET
  @Path(VerificationConstants.HEATMAP)
  @Timed
  @ExceptionMetered
  public RestResponse<List<HeatMap>> getHeatMap(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("serviceId") @Valid final String serviceId, @QueryParam("resolution") int resolution,
      @QueryParam("startTime") long startTime, @QueryParam("endTime") long endTime) {
    return new RestResponse<>(
        continuousVerificationService.getHeatMap(accountId, serviceId, resolution, startTime, endTime));
  }

  @GET
  @Path(VerificationConstants.GET_DEPLOYMENTS_24_7)
  @Timed
  @ExceptionMetered
  public RestResponse<List<CVDeploymentData>> getCVExecutionDeploymentRecords(@QueryParam("accountId") String accountId,
      @QueryParam("beginEpochTs") long beginEpochTs, @QueryParam("endEpochTs") long endEpochTs,
      @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(continuousVerificationService.getCVDeploymentData(
        accountId, beginEpochTs, endEpochTs, UserThreadLocal.get().getPublicUser(), serviceId));
  }
}
