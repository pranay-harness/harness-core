package io.harness.cvng.analysis.resource;

import static io.harness.cvng.CVConstants.LOG_CLUSTER_RESOURCE;

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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(LOG_CLUSTER_RESOURCE)
@Path(LOG_CLUSTER_RESOURCE)
@Produces("application/json")
public class LogClusterResource {
  @Inject private LogClusterService logClusterService;

  @GET
  @Path("/serviceguard-test-data")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<LogClusterDTO> getTestData(@QueryParam("cvConfigId") String cvConfigId,
      @QueryParam("logClusterLevel") LogClusterLevel logClusterLevel,
      @QueryParam("logRecordInstant") String logRecordInstant, @QueryParam("host") String host) {
    return new RestResponse<>(
        logClusterService.getDataForLogCluster(cvConfigId, Instant.parse(logRecordInstant), host, logClusterLevel));
  }
}
