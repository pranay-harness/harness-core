package io.harness.cvng.verificationjob.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.analysis.beans.TransactionSummaryPageDTO;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("deployment-time-series-analysis")
@Path("deployment-time-series-analysis")
@Produces("application/json")
@ExposeInternalException
public class DeploymentTimeSeriesAnalysisResource {
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;

  @Produces({"application/json", "application/v1+json"})
  @GET
  @Path("/{verificationJobInstanceId}")
  @Timed
  @LearningEngineAuth
  @ExceptionMetered
  public RestResponse<TransactionSummaryPageDTO> getMetrics(
      @PathParam("verificationJobInstanceId") String verificationJobInstanceId,
      @QueryParam("accountId") String accountId, @QueryParam("anomalousMetricsOnly") boolean anomalousMetricsOnly,
      @QueryParam("hostName") String hostName, @QueryParam("pageNumber") int pageNumber) {
    return new RestResponse(deploymentTimeSeriesAnalysisService.getMetrics(
        accountId, verificationJobInstanceId, anomalousMetricsOnly, hostName, pageNumber));
  }
}
