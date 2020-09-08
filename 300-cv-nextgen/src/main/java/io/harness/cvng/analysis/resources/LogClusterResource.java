package io.harness.cvng.analysis.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.LOG_CLUSTER_RESOURCE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.cvng.analysis.beans.LogClusterDTO;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;

import java.time.Instant;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(LOG_CLUSTER_RESOURCE)
@Path(LOG_CLUSTER_RESOURCE)
@Produces("application/json")
public class LogClusterResource {
  @Inject private LogClusterService logClusterService;

  @GET
  @Path("/test-data")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<List<LogClusterDTO>> getTestData(@QueryParam("verificationTaskId") String verificationTaskId,
      @QueryParam("clusterLevel") LogClusterLevel logClusterLevel, @QueryParam("startTime") Long startTime,
      @QueryParam("endTime") Long endTime, @QueryParam("host") String host) {
    return new RestResponse<>(logClusterService.getDataForLogCluster(
        verificationTaskId, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), host, logClusterLevel));
  }

  @Produces({"application/json", "application/v1+json"})
  @POST
  @Path("/save-clustered-logs")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<Boolean> saveClusteredData(@QueryParam("taskId") String taskId,
      @QueryParam("verificationTaskId") String verificationTaskId, @QueryParam("timestamp") String timestamp,
      @QueryParam("clusterLevel") LogClusterLevel clusterLevel, List<LogClusterDTO> clusterDTO) {
    logClusterService.saveClusteredData(clusterDTO, verificationTaskId, Instant.parse(timestamp), taskId, clusterLevel);
    return new RestResponse<>(true);
  }
}
